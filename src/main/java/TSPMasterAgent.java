import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Scanner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.assh.command.demo; // ★ demo へ結果を返す
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class TSPMasterAgent extends AbstractAgent implements IMessageListener {

    private static final int MSG_PORT = 55878;
    
    private String homeIP = IPAddress.myIPAddress;

    /** TSP インスタンスファイルパス（相対パス） */
    private String fileName = "others/tsp16.txt";

    /** 全ワーカー終了確認用のポーリング間隔(ms) */
    private long sleepMillis = 300L;

    /** 距離行列 */
    private int[][] distanceMatrix;

    /** 都市数 */
    private int numCities;

    /** ワーカー数 */
    private int numWorkers = 8;

    /** スレーブ */
    private AbstractAgent[] workers;

    /** workerRanges[w] = [start,end) */
    private int[][] workerRanges;

    /** スレーブクラス名 */
    private String slaveAgentClassName = "TSPSlaveAgent";

    /** 完了フラグ */
    private boolean[] taskHasFinished;

    /** 最良解 */
    private int bestCost = Integer.MAX_VALUE;
    private int[] bestPath;

    /** 全ワーカー終了 */
    private volatile boolean finished = false;

    /* =========================
     * メッセージ定義
     * ========================= */

    public static class TSPMasterMessage implements Serializable {
        public int numCities;
        public int workerId;
        public int start;
        public int end;
        public int[][] distanceMatrix;
        public String masterId; // Master AgentID
    }

    public static class TSPResultMessage implements Serializable {
        public int workerId;
        public int bestCost;
        public int[] bestPath;
    }

    @Override
    public void run() {

        long totalStart = System.currentTimeMillis();

        System.out.println("[TSPMaster] START id=" + getAgentID());

        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
            safeReportToDemo("TSPMaster FAILED: Message listener register exception: " + e);
            return;
        }

        // ---- load tsp ----
        try {
            distanceMatrix = loadTSPProblem(fileName);
            numCities = distanceMatrix.length;
            System.out.println("[TSPMaster] TSP loaded cities=" + numCities + " file=" + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            safeReportToDemo("TSPMaster FAILED: load tsp exception: " + e);
            return;
        }

        // ---- init ----
        workers = new AbstractAgent[numWorkers];
        taskHasFinished = new boolean[numWorkers];
        workerRanges = divideSearchSpace(distanceMatrix);

        // ---- spawn slaves ----
        for (int w = 0; w < numWorkers; w++) {

            Object agentInstance = loadAgentInstance(slaveAgentClassName);
            if (!(agentInstance instanceof AbstractAgent)) {
                System.err.println("[TSPMaster] Failed to instantiate slave w=" + w);
                continue;
            }

            AbstractAgent agent = (AbstractAgent) agentInstance;
            AgentAPI.runAgent(agent);
            workers[w] = agent;

            TSPMasterMessage msg = new TSPMasterMessage();
            msg.numCities = numCities;
            msg.workerId = w;
            msg.start = workerRanges[w][0];
            msg.end   = workerRanges[w][1];
            msg.distanceMatrix = distanceMatrix;
            msg.masterId = getAgentID();

            sendToAgent(agent.getAgentID(), agent.getmyIP(), msg);

            System.out.println("[TSPMaster] Slave started w=" + w
                    + " slaveId=" + agent.getAgentID()
                    + " range=[" + msg.start + "," + msg.end + ")");
        }

        // ---- wait ----
        while (!finished) {
            try { Thread.sleep(sleepMillis); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

        long end = System.currentTimeMillis();

        StringBuilder report = new StringBuilder();
        report.append("==== TSP Result ====\n");
        report.append("MasterAgentID : ").append(getAgentID()).append("\n");
        report.append("cities        : ").append(numCities).append("\n");
        report.append("workers       : ").append(numWorkers).append("\n");
        report.append("bestCost      : ").append(bestCost).append("\n");
        report.append("bestPath      : ").append(bestPath == null ? "(null)" : Arrays.toString(bestPath)).append("\n");
        report.append("totalTime(ms) : ").append(end - totalStart).append("\n");

        System.out.println(report.toString());
        safeReportToDemo(report.toString());

        System.out.println("[TSPMaster] END id=" + getAgentID());
        

        //migrate(homeIP);
        demo.reportAgentHistory(getAgentID(), buildHistoryText());
    }

    /* =========================
     * Receive
     * ========================= */
    @Override
    public void receivedMessage(AbstractEnvelope envelope) {
        try {
            Object raw = ((StandardContentContainer) envelope.getContent()).getContent();
            if (!(raw instanceof TSPResultMessage)) return;

            TSPResultMessage msg = (TSPResultMessage) raw;
            int w = msg.workerId;

            if (w < 0 || w >= numWorkers) {
                System.err.println("[TSPMaster] invalid workerId=" + w);
                return;
            }

            taskHasFinished[w] = true;
            compareBestCost(msg.bestPath, msg.bestCost);

            for (boolean f : taskHasFinished) {
                if (!f) return;
            }
            finished = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void compareBestCost(int[] newPath, int newCost) {
        if (newPath == null) return;
        if (newCost < bestCost) {
            bestCost = newCost;
            bestPath = newPath.clone();
            System.out.println("[TSPMaster] Best updated cost=" + bestCost);
        }
    }

    /* =========================
     * Send
     * ========================= */
    private void sendToAgent(String agentId, InetAddress ip, Serializable msg) {
        try {
            if (agentId == null || ip == null) return;
            KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<>(ip, MSG_PORT);
            StandardEnvelope env = new StandardEnvelope(
                    new AgentAddress(agentId),
                    new StandardContentContainer(msg)
            );
            MessageAPI.send(dst, env);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================
     * Load agent class
     * ========================= */
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
            System.err.println("[TSPMaster] Failed to load class: " + className);
            e.printStackTrace();
        }

        return obj;
    }

    /* =========================
     * Load TSP file
     * ========================= */
    private int[][] loadTSPProblem(String fileName) throws Exception {
        File f = new File(fileName);
        if (!f.exists()) {
            throw new IllegalArgumentException("TSP file not found: " + f.getAbsolutePath());
        }

        try (FileInputStream in = new FileInputStream(f);
             Scanner sc = new Scanner(in)) {

            int n = sc.nextInt();
            int[][] d = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    d[i][j] = sc.nextInt();
                }
            }
            return d;
        }
    }

    /* =========================
     * Divide search space
     * ========================= */
    private int[][] divideSearchSpace(int[][] dist) {
        int cities = dist.length;
        int choices = cities - 1;

        int[][] ranges = new int[numWorkers][2];

        int base = choices / numWorkers;
        int rem  = choices % numWorkers;

        int cur = 1;
        for (int w = 0; w < numWorkers; w++) {
            int size = base + (w < rem ? 1 : 0);
            ranges[w][0] = cur;
            ranges[w][1] = cur + size;
            cur += size;
        }
        return ranges;
    }

    private void safeReportToDemo(String resultText) {
        try {
            demo.reportMasterFinished(getAgentID(), resultText);
        } catch (Throwable t) {
            System.err.println("[TSPMaster] report to demo failed: " + t);
        }
    }

    @Override public void requestStop() {}
    @Override public String getStrictName() { return getAgentID(); }
    @Override public String getSimpleName() { return getAgentName(); }
}