package primula.api.core.assh.command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.AgentClassInfo;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import scheduler2022.DynamicPCInfo;
import scheduler2022.InformationCenter;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;
import scheduler2022.util.DHTutil;

/**
 * 修論用 demo:
 *  - TSPMasterAgent / SortMasterAgent / DL4JMSMaster を起動
 *  - 実行中に全ノードの DynamicPCInfo を収集して Max/Min を取る（updateMax/updateMin）
 *  - 各Masterの終了結果（任意）も集める
 *  - 終了後に 1つのテキストへ保存
 *  - ★追加：全Agent（Master/Slave含む）の migrate 履歴をログへ
 *
 * 重要:
 *  - AbstractAgent に追加した history(List) は「エージェント本体」に保持されます。
 *  - demo 側だけでは“全SlaveのAbstractAgentインスタンス”を掴めないため、
 *    各Agentが終了時に demo.reportAgentHistory(...) を呼んで経路を渡す方式にしています。
 */
public class demo extends AbstractCommand {

    /** Master側から「終了」を通知してもらうためのカウンタ（スレッド安全） */
    public static final AtomicInteger mastersFinished = new AtomicInteger(0);

    /** Master側から「結果文字列」を通知してもらう（任意） */
    private static final ConcurrentMap<String, String> masterResults = new ConcurrentHashMap<>();

    /** Master側から「イベントログ」を通知してもらう（任意） */
    private static final ConcurrentMap<String, List<String>> masterEvents = new ConcurrentHashMap<>();

    /** ★追加：全Agent（Master/Slave含む）の移動履歴（文字列） */
    private static final ConcurrentMap<String, String> agentHistories = new ConcurrentHashMap<>();

    /** デモ実行中のルート記録（既存互換） */
    public static List<String> routes = new ArrayList<>();

    /** PCInfo統計 */
    public static Map<String, DynamicPCInfo> Max = new HashMap<>();
    public static Map<String, DynamicPCInfo> Min = new HashMap<>();
    public static Map<String, StaticPCInfo> staticPCInfos = new HashMap<>();


    public static String strategy;
    
    private static class DemoConfig {
        int dl4jRounds = 10;
        int sortRounds = 20;
        String tspFile = "others/tsp17.txt"; // デフォルト

        @Override public String toString(){
            return "DemoConfig{dl4jRounds=" + dl4jRounds
                + ", sortRounds=" + sortRounds
                + ", tspFile=" + tspFile + "}";
        }
    }

    private static DemoConfig parseOpt(List<String> opt){
        DemoConfig c = new DemoConfig();
        if (opt == null || opt.isEmpty()) return c;

        // 1) まず key=value を処理
        for (String s : opt) {
            if (s == null) continue;
            String t = s.trim();
            while (t.startsWith("-")) t = t.substring(1);
            int eq = t.indexOf('=');
            if (eq < 0) continue;

            String key = t.substring(0, eq).trim();
            String val = t.substring(eq + 1).trim();

            try {
                switch (key) {
                    case "dl4jRounds": c.dl4jRounds = Integer.parseInt(val); break;
                    case "sortRounds": c.sortRounds = Integer.parseInt(val); break;
                    case "tspFile":    c.tspFile = val; break;
                }
            } catch (Exception ignore) {}
        }

        // 2) 次に位置引数（"5" "20" "file"）を拾う
        //    ※ key=value が無い純粋な数字/文字列だけを順に採用
        List<String> positional = new ArrayList<>();
        for (String s : opt) {
            if (s == null) continue;
            String t = s.trim();
            if (t.contains("=")) continue;
            if (t.startsWith("-")) continue; // -h みたいなのは除外
            positional.add(t);
        }

        try {
            if (positional.size() >= 1) c.dl4jRounds = Integer.parseInt(positional.get(0));
            if (positional.size() >= 2) c.sortRounds = Integer.parseInt(positional.get(1));
            if (positional.size() >= 3) c.tspFile = positional.get(2);
        } catch (Exception ignore) {}

        return c;
    }

    /** 起動する Master のクラス名（bin 直下にある前提） */
    private static final List<String> MASTER_CLASS_NAMES = Arrays.asList(
            "TSPMasterAgent",
            "SortMasterAgent",
            "DL4JMSMaster"
    );

    /** demoが待つ最大時間（安全弁） */
    private static final long DEMO_TIMEOUT_MS = 3 * 60 * 60 * 1000L; // 1hours

    /** Masterが終了時に呼ぶ：結果通知（おすすめ） */
    public static void reportMasterFinished(String masterAgentId, String resultText) {
        if (masterAgentId == null) masterAgentId = "(unknown)";
        if (resultText != null) masterResults.put(masterAgentId, resultText);
        mastersFinished.incrementAndGet();
    }

    /** Masterが任意で呼ぶ：途中経過ログ（任意） */
    public static void logMasterEvent(String masterAgentId, String event) {
        if (masterAgentId == null) masterAgentId = "(unknown)";
        masterEvents
            .computeIfAbsent(masterAgentId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(event);
    }

    /**
     * ★追加：任意のAgent(Master/Slave)が終了時に呼ぶ：移動履歴をdemoに渡す
     * 例）demo.reportAgentHistory(getAgentID(), getAgentName(), buildHistoryText());
     */
    public static void reportAgentHistory(String agentId, String agentName, String historyText) {
        if (agentId == null) agentId = "(unknown)";
        if (agentName == null) agentName = "(unknown)";
        if (historyText == null) historyText = "(null)";
        agentHistories.put(agentName + " id=" + agentId, historyText);
    }

    @Override
    public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {

    	
    	Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
    	    System.err.println("=== Uncaught Exception ===");
    	    System.err.println("Thread : " + t.getName());
    	    if (e != null) {
    	        e.printStackTrace();
    	    } else {
    	        System.err.println("Exception is null");
    	    }
    	});
        // opt + fileNames を合わせて parse する
        List<String> args = new ArrayList<>();
        if (opt != null) args.addAll(opt);
        if (fileNames != null) args.addAll(fileNames);

        DemoConfig cfg = parseOpt(args);

        System.out.println("[demo] raw opt=" + opt);
        System.out.println("[demo] fileNames=" + fileNames);
        System.out.println("[demo] merged args=" + args);
        System.out.println("[demo] " + cfg);

        // ---------- 初期化 ----------
        routes.clear();
        Max = new HashMap<>();
        Min = new HashMap<>();
        staticPCInfos = new HashMap<>();
        mastersFinished.set(0);
        masterResults.clear();
        masterEvents.clear();
        agentHistories.clear();

        List<Object> resultList = new ArrayList<>();
        List<AbstractAgent> masters = new ArrayList<>();
        
        strategy = Scheduler.getStrategy().getClass().getName();

        // ---------- 実験対象IPの取得（DHTベース） ----------
        Set<String> currentIPs = InformationCenter.getAllIPs();

        staticPCInfos = InformationCenter.getAllSPIs();

        // ---------- Master起動 ----------
        Map<String, String> masterIdToClass = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();

        for (String className : MASTER_CLASS_NAMES) {

            Object agentInstance = loadAgentInstance(className);
            if (!(agentInstance instanceof AbstractAgent)) {
                System.err.println("[demo] Failed to instantiate: " + className);
                continue;
            }
            AbstractAgent agent = (AbstractAgent) agentInstance;

            // クラス名で分岐（単純でOK）
            String simple = agent.getClass().getSimpleName();
            if (simple.equals("DL4JMSMaster")) {
                tryInvokeSetter(agent, "setRounds", int.class, cfg.dl4jRounds);
            }
            if (simple.equals("SortMasterAgent")) {
                tryInvokeSetter(agent, "setRounds", int.class, cfg.sortRounds);
            }
            if (simple.equals("TSPMasterAgent")) {
                tryInvokeSetter(agent, "setProblemFile", String.class, cfg.tspFile);
                // もしメソッド名が違う可能性があるなら保険で：
                tryInvokeSetter(agent, "setFileName", String.class, cfg.tspFile);
            }

            AgentAPI.runAgent(agent);

            masters.add(agent);
            resultList.add(agent);

            masterIdToClass.put(agent.getAgentID(), className);
            System.out.println("[demo] " + className + " started. id=" + agent.getAgentID());
        }

        int expectedMasters = masters.size();
        if (expectedMasters == 0) {
            System.err.println("[demo] No masters started. Abort.");
            return resultList;
        }

        // ---------- 収集ループ ----------
        long deadline = startTime + DEMO_TIMEOUT_MS;

        while (mastersFinished.get() < expectedMasters && System.currentTimeMillis() < deadline) {
        	
            for (String ip : currentIPs) {
                DynamicPCInfo dpi = InformationCenter.getOtherDPI(ip);

                if (dpi == null || dpi.LoadAverage < 0) continue;
                if (dpi.CPU == null) continue;

                if (dpi.GPUs == null) dpi.GPUs = new HashMap<>();
                if (dpi.NetworkCards == null) dpi.NetworkCards = new HashMap<>();

                // --- Max ---
                DynamicPCInfo maxDpi = Max.get(ip);
                if (maxDpi == null) {
                    Max.put(ip, dpi.deepCopy());
                } else {
                    updateMax(maxDpi, dpi);
                    Max.put(ip, maxDpi.deepCopy());
                }

                // --- Min ---
                DynamicPCInfo minDpi = Min.get(ip);
                if (minDpi == null) {
                    Min.put(ip, dpi.deepCopy());
                } else {
                    updateMin(minDpi, dpi);
                    Min.put(ip, minDpi.deepCopy());
                }
            }

            try {
                Thread.sleep(Scheduler.UPDATE_SPAN);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;

        String resultText = buildResultText(
                startTime, endTime, elapsed,
                masterIdToClass, expectedMasters,
                currentIPs
        );

        saveToFile(startTime, resultText);

        System.out.println("[demo] Finished. elapsed=" + elapsed + "ms finishedMasters="
                + mastersFinished.get() + "/" + expectedMasters);

        return resultList;
    }

    private static void tryInvokeSetter(Object target, String method, Class<?> argType, Object arg){
        try {
            target.getClass().getMethod(method, argType).invoke(target, arg);
            System.out.println("[demo] set " + method + "(" + arg + ") on " + target.getClass().getName());
        } catch (NoSuchMethodException e) {
            // setter無いなら無視
        } catch (Exception e) {
            System.err.println("[demo] failed " + method + " on " + target.getClass().getName() + ": " + e);
        }
    }

    // =========================================================
    // Max/Min 更新
    // =========================================================
    private static void updateMax(DynamicPCInfo maxDpi, DynamicPCInfo dpi) {
        maxDpi.LoadAverage = Math.max(maxDpi.LoadAverage, dpi.LoadAverage);

        if (maxDpi.CPU == null) maxDpi.CPU = new DynamicPCInfo.CPU();
        maxDpi.CPU.ClockSpeed = Math.max(maxDpi.CPU.ClockSpeed, dpi.CPU.ClockSpeed);
        maxDpi.CPU.LoadPercentByMXBean = Math.max(maxDpi.CPU.LoadPercentByMXBean, dpi.CPU.LoadPercentByMXBean);

        maxDpi.FreeMemory = Math.max(maxDpi.FreeMemory, dpi.FreeMemory);
        maxDpi.AgentsNum = Math.max(maxDpi.AgentsNum, dpi.AgentsNum);

        if (maxDpi.GPUs == null) maxDpi.GPUs = new HashMap<>();
        for (Map.Entry<String, DynamicPCInfo.GPU> e : dpi.GPUs.entrySet()) {
            String gpuId = e.getKey();
            DynamicPCInfo.GPU cur = e.getValue();
            if (cur == null) continue;

            DynamicPCInfo.GPU maxGpu = maxDpi.GPUs.get(gpuId);
            if (maxGpu == null) {
                maxDpi.GPUs.put(gpuId, copyGpu(cur));
            } else {
                maxGpu.LoadPercent  = Math.max(maxGpu.LoadPercent,  cur.LoadPercent);
                maxGpu.UsedMemory   = Math.max(maxGpu.UsedMemory,   cur.UsedMemory);
                maxGpu.TotalMemory  = Math.max(maxGpu.TotalMemory,  cur.TotalMemory);
                maxGpu.TemperatureC = Math.max(maxGpu.TemperatureC, cur.TemperatureC);
            }
        }

        if (maxDpi.NetworkCards == null) maxDpi.NetworkCards = new HashMap<>();
        for (Map.Entry<String, DynamicPCInfo.NetworkCard> e : dpi.NetworkCards.entrySet()) {
            String iface = e.getKey();
            DynamicPCInfo.NetworkCard cur = e.getValue();
            if (cur == null) continue;

            DynamicPCInfo.NetworkCard maxCard = maxDpi.NetworkCards.get(iface);
            if (maxCard == null) {
                maxDpi.NetworkCards.put(iface, copyNetCard(cur));
            } else {
                maxCard.UploadSpeed   = Math.max(nullSafe(maxCard.UploadSpeed),   nullSafe(cur.UploadSpeed));
                maxCard.DownloadSpeed = Math.max(nullSafe(maxCard.DownloadSpeed), nullSafe(cur.DownloadSpeed));
                maxCard.SentByte      = Math.max(nullSafe(maxCard.SentByte),      nullSafe(cur.SentByte));
                maxCard.ReceivedByte  = Math.max(nullSafe(maxCard.ReceivedByte),  nullSafe(cur.ReceivedByte));
            }
        }
    }

    private static void updateMin(DynamicPCInfo minDpi, DynamicPCInfo dpi) {
        minDpi.LoadAverage = Math.min(minDpi.LoadAverage, dpi.LoadAverage);

        if (minDpi.CPU == null) minDpi.CPU = new DynamicPCInfo.CPU();
        minDpi.CPU.ClockSpeed = Math.min(minDpi.CPU.ClockSpeed, dpi.CPU.ClockSpeed);
        minDpi.CPU.LoadPercentByMXBean = Math.min(minDpi.CPU.LoadPercentByMXBean, dpi.CPU.LoadPercentByMXBean);

        minDpi.FreeMemory = Math.min(minDpi.FreeMemory, dpi.FreeMemory);
        minDpi.AgentsNum = Math.min(minDpi.AgentsNum, dpi.AgentsNum);

        if (minDpi.GPUs == null) minDpi.GPUs = new HashMap<>();
        for (Map.Entry<String, DynamicPCInfo.GPU> e : dpi.GPUs.entrySet()) {
            String gpuId = e.getKey();
            DynamicPCInfo.GPU cur = e.getValue();
            if (cur == null) continue;

            DynamicPCInfo.GPU minGpu = minDpi.GPUs.get(gpuId);
            if (minGpu == null) {
                minDpi.GPUs.put(gpuId, copyGpu(cur));
            } else {
                minGpu.LoadPercent  = Math.min(minGpu.LoadPercent,  cur.LoadPercent);
                minGpu.UsedMemory   = Math.min(minGpu.UsedMemory,   cur.UsedMemory);
                minGpu.TotalMemory  = Math.min(minGpu.TotalMemory,  cur.TotalMemory);
                minGpu.TemperatureC = Math.min(minGpu.TemperatureC, cur.TemperatureC);
            }
        }

        if (minDpi.NetworkCards == null) minDpi.NetworkCards = new HashMap<>();
        for (Map.Entry<String, DynamicPCInfo.NetworkCard> e : dpi.NetworkCards.entrySet()) {
            String iface = e.getKey();
            DynamicPCInfo.NetworkCard cur = e.getValue();
            if (cur == null) continue;

            DynamicPCInfo.NetworkCard minCard = minDpi.NetworkCards.get(iface);
            if (minCard == null) {
                minDpi.NetworkCards.put(iface, copyNetCard(cur));
            } else {
                minCard.UploadSpeed   = Math.min(nullSafe(minCard.UploadSpeed),   nullSafe(cur.UploadSpeed));
                minCard.DownloadSpeed = Math.min(nullSafe(minCard.DownloadSpeed), nullSafe(cur.DownloadSpeed));
                minCard.SentByte      = Math.min(nullSafe(minCard.SentByte),      nullSafe(cur.SentByte));
                minCard.ReceivedByte  = Math.min(nullSafe(minCard.ReceivedByte),  nullSafe(cur.ReceivedByte));
            }
        }
    }

    // =========================================================
    // 結果文字列組み立て
    // =========================================================
    private static String buildResultText(
            long startTime, long endTime, long elapsed,
            Map<String, String> masterIdToClass,
            int expectedMasters,
            Set<String> currentIPs
    ) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        sb.append("========== Thesis Demo Result ==========\n");
        sb.append("StartTime : ").append(sdf.format(new Date(startTime)))
          .append(" , EndTime : ").append(sdf.format(new Date(endTime))).append("\n");
        sb.append("ElapsedTime : ").append(elapsed).append(" ms\n");
        sb.append("Strategy : ").append(strategy).append("\n\n");

        sb.append("---- Masters ----\n");
        sb.append("Expected : ").append(expectedMasters).append("\n");
        sb.append("Finished : ").append(mastersFinished.get()).append("\n");
        for (Map.Entry<String, String> e : masterIdToClass.entrySet()) {
            sb.append(" - ").append(e.getValue())
              .append(" id=").append(e.getKey()).append("\n");
        }
        sb.append("\n");

        sb.append("---- Master Results ----\n");
        if (masterResults.isEmpty()) {
            sb.append("(no master results reported)\n");
        } else {
            for (Map.Entry<String, String> e : masterResults.entrySet()) {
                sb.append("[").append(e.getKey()).append("]\n");
                sb.append(e.getValue()).append("\n\n");
            }
        }

        sb.append("---- Master Events (optional) ----\n");
        if (masterEvents.isEmpty()) {
            sb.append("(no master events)\n");
        } else {
            for (Map.Entry<String, List<String>> e : masterEvents.entrySet()) {
                sb.append("[").append(e.getKey()).append("]\n");
                for (String line : e.getValue()) sb.append("  ").append(line).append("\n");
                sb.append("\n");
            }
        }

        sb.append("---- Agent Routes (optional) ----\n");
        if (routes.isEmpty()) {
            sb.append("(no routes)\n");
        } else {
            for (String r : routes) sb.append(r).append("\n");
        }
        sb.append("\n");

        sb.append("---- Agent Migration History (Master + Slave) ----\n");
        if (agentHistories.isEmpty()) {
            sb.append("(no agent history reported)\n");
            sb.append("NOTE: Each agent (master/slave) should call demo.reportAgentHistory(agentId, agentName, historyText) on finish.\n");
        } else {
            for (Map.Entry<String, String> e : agentHistories.entrySet()) {
                sb.append("[").append(e.getKey()).append("]\n");
                sb.append(e.getValue()).append("\n\n");
            }
        }
        sb.append("\n");

        sb.append("---- Agent Class Characteristics (from DHT) ----\n");

        Set<String> classNames = new java.util.LinkedHashSet<>();
        for (String cls : masterIdToClass.values()) classNames.add(cls);

        for (String key : agentHistories.keySet()) {
            int idx = key.indexOf(" id=");
            if (idx > 0) classNames.add(key.substring(0, idx));
        }

        if (classNames.isEmpty()) {
            sb.append("(no agent classes found)\n");
        } else {
            for (String cn : classNames) {
                AgentClassInfo info = DHTutil.getAgentInfo(cn);
                sb.append("[").append(cn).append("]\n");
                sb.append(info != null ? info.toString() : "(no AgentClassInfo in DHT)\n");
                sb.append("\n");
            }
        }
        sb.append("\n");

        sb.append("---- PC Information (Static + Max/Min Dynamic) ----\n");
        for (String ip : currentIPs) {
            StaticPCInfo spi = staticPCInfos.get(ip);
            DynamicPCInfo max = Max.get(ip);
            DynamicPCInfo min = Min.get(ip);

            sb.append("IP : ").append(ip).append("\n");
            sb.append("Static : ").append(spi != null ? spi.toString() : "(no StaticPCInfo)").append("\n");
            sb.append("Max Dynamic : ").append(max != null ? max.toString() : "(no Max)").append("\n");
            sb.append("Min Dynamic : ").append(min != null ? min.toString() : "(no Min)").append("\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    private static void saveToFile(long startTime, String text) {
        SimpleDateFormat filesdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String logTime = filesdf.format(new Date(startTime));

        File dir = new File("DemoLog");
        if (!dir.exists()) dir.mkdirs();

        String fileName = "DemoLog/" + logTime + "_ThesisDemoResult.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(text);
            System.out.println("[demo] Saved: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static DynamicPCInfo.GPU copyGpu(DynamicPCInfo.GPU s) {
        DynamicPCInfo.GPU d = new DynamicPCInfo.GPU();
        d.Name = s.Name;
        d.LoadPercent = s.LoadPercent;
        d.UsedMemory = s.UsedMemory;
        d.TotalMemory = s.TotalMemory;
        d.TemperatureC = s.TemperatureC;
        return d;
    }

    private static DynamicPCInfo.NetworkCard copyNetCard(DynamicPCInfo.NetworkCard s) {
        DynamicPCInfo.NetworkCard d = new DynamicPCInfo.NetworkCard();
        d.UploadSpeed = s.UploadSpeed;
        d.DownloadSpeed = s.DownloadSpeed;
        d.SentByte = s.SentByte;
        d.ReceivedByte = s.ReceivedByte;
        return d;
    }

    private static long nullSafe(Long v) {
        return v == null ? 0L : v;
    }

    private Object loadAgentInstance(String className) {
        Object obj = null;
        Class<?> cls;
        ChainContainer cc;
        GhostClassLoader gcl;
        String path = ".\\bin";

        gcl = GhostClassLoader.unique;
        cc = gcl.getChainContainer();

        try {
            cc.resistNewClassLoader(new StringSelector(path), new File(path));
        } catch (IOException e) {
            Logger.getLogger(getClass()).log(Level.TRACE, null, e);
        }

        try {
            cls = gcl.loadClass(className);
            obj = cls.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return obj;
    }
}