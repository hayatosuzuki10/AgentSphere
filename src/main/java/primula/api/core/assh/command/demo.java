package primula.api.core.assh.command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;

public class demo extends AbstractCommand {
	
	public static int agentsAlive = 0;
    public static List<String> routes = new ArrayList<>();
    public int agentsNum = 20;
	public static Map<String, DynamicPCInfo> Max = new HashMap<>();
	public static Map<String, DynamicPCInfo> Min = new HashMap<>();
	public static Map<String, StaticPCInfo> staticPCInfos = new HashMap<>();

    @Override
    public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
    	routes.clear();
    	Max = new HashMap<>();
    	Min = new HashMap<>();
        List<AbstractAgent> agents = new ArrayList<>();
        List<Object> resultList = new ArrayList<>();
        String agentClassName = "DemoAgent";


//		Scheduler sch = new Scheduler();
//		Thread th = new Thread(sch);
//		th.setName("AgentSchedule2022");
//		th.start();
//		System.out.println("スケジューラ起動！！");
		Set<String> currentIPs = Scheduler.getAliveIPs();
		currentIPs.add(IPAddress.myIPAddress);
		for(String ip : currentIPs) {
			StaticPCInfo spi = Scheduler.getSpis().get(ip);
			staticPCInfos.put(ip, spi);
		}
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < agentsNum; i++) {
        	
            Object agentInstance = loadAgentInstance(agentClassName);
            if (agentInstance instanceof AbstractAgent) {
                AbstractAgent agent = (AbstractAgent) agentInstance;
                AgentAPI.runAgent(agent);
                agents.add(agent);
                resultList.add(agent);
                System.out.println("DemoAgent " + (i + 1) + " started.");
            } else {
                System.err.println("Failed to instantiate agent at index: " + i);
            }
        }

        // 全エージェント終了待ち（isAliveがfalseになるまでループ）
        while (agentsAlive < agentsNum) {
            for (String ip : currentIPs) {
                DynamicPCInfo dpi = Scheduler.getDpis().get(ip);

                // まず null & 明らかに無効なものはスキップ
                if (dpi == null || dpi.LoadAverage < 0) {
                    continue;
                }
                if (dpi.CPU == null) {
                    // CPU 情報がないノードは統計対象外にしてしまう
                    continue;
                }
                if (dpi.GPUs == null) {
                    dpi.GPUs = new HashMap<>();
                }
                if (dpi.NetworkCards == null) {
                    dpi.NetworkCards = new HashMap<>();
                }

                // ===== Max 更新 =====
                DynamicPCInfo maxDpi = Max.get(ip);
                if (maxDpi == null) {
                    Max.put(ip, dpi.deepCopy());
                } else {
                    // 基本情報
                    maxDpi.LoadAverage = Math.max(maxDpi.LoadAverage, dpi.LoadAverage);
                    if (maxDpi.CPU == null) maxDpi.CPU = new DynamicPCInfo.CPU();
                    maxDpi.CPU.ClockSpeed =
                            Math.max(maxDpi.CPU.ClockSpeed, dpi.CPU.ClockSpeed);
                    maxDpi.CPU.LoadPercentByMXBean =
                            Math.max(maxDpi.CPU.LoadPercentByMXBean, dpi.CPU.LoadPercentByMXBean);
                    maxDpi.FreeMemory = Math.max(maxDpi.FreeMemory, dpi.FreeMemory);
                    maxDpi.AgentsNum = Math.max(maxDpi.AgentsNum, dpi.AgentsNum);

                    // GPU (Map ベース)
                    if (maxDpi.GPUs == null) maxDpi.GPUs = new HashMap<>();
                    for (Map.Entry<String, DynamicPCInfo.GPU> e : dpi.GPUs.entrySet()) {
                        String gpuId = e.getKey();
                        DynamicPCInfo.GPU cur = e.getValue();
                        if (cur == null) continue;

                        DynamicPCInfo.GPU maxGpu = maxDpi.GPUs.get(gpuId);
                        if (maxGpu == null) {
                            maxDpi.GPUs.put(gpuId, copyGpu(cur));
                        } else {
                            maxGpu.LoadPercent = Math.max(maxGpu.LoadPercent, cur.LoadPercent);
                            maxGpu.UsedMemory  = Math.max(maxGpu.UsedMemory,  cur.UsedMemory);
                            maxGpu.TotalMemory = Math.max(maxGpu.TotalMemory, cur.TotalMemory);
                            maxGpu.TemperatureC= Math.max(maxGpu.TemperatureC, cur.TemperatureC);
                        }
                    }

                    // NetworkCard
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

                    Max.put(ip, maxDpi.deepCopy());
                }

                // ===== Min 更新 =====
                DynamicPCInfo minDpi = Min.get(ip);
                if (minDpi == null) {
                    Min.put(ip, dpi.deepCopy());
                } else {
                    minDpi.LoadAverage = Math.min(minDpi.LoadAverage, dpi.LoadAverage);
                    if (minDpi.CPU == null) minDpi.CPU = new DynamicPCInfo.CPU();
                    minDpi.CPU.ClockSpeed =
                            Math.min(minDpi.CPU.ClockSpeed, dpi.CPU.ClockSpeed);
                    minDpi.CPU.LoadPercentByMXBean =
                            Math.min(minDpi.CPU.LoadPercentByMXBean, dpi.CPU.LoadPercentByMXBean);
                    minDpi.FreeMemory = Math.min(minDpi.FreeMemory, dpi.FreeMemory);
                    minDpi.AgentsNum  = Math.min(minDpi.AgentsNum, dpi.AgentsNum);

                    if (minDpi.GPUs == null) minDpi.GPUs = new HashMap<>();
                    for (Map.Entry<String, DynamicPCInfo.GPU> e : dpi.GPUs.entrySet()) {
                        String gpuId = e.getKey();
                        DynamicPCInfo.GPU cur = e.getValue();
                        if (cur == null) continue;

                        DynamicPCInfo.GPU minGpu = minDpi.GPUs.get(gpuId);
                        if (minGpu == null) {
                            minDpi.GPUs.put(gpuId, copyGpu(cur));
                        } else {
                            minGpu.LoadPercent = Math.min(minGpu.LoadPercent, cur.LoadPercent);
                            minGpu.UsedMemory  = Math.min(minGpu.UsedMemory,  cur.UsedMemory);
                            minGpu.TotalMemory = Math.min(minGpu.TotalMemory, cur.TotalMemory);
                            minGpu.TemperatureC= Math.min(minGpu.TemperatureC, cur.TemperatureC);
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

                    Min.put(ip, minDpi.deepCopy());
                }
            }

            try {
                Thread.sleep(Scheduler.UPDATE_SPAN);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
//        sch.Stop = true;

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("All DemoAgents finished. Elapsed time: " + elapsed + " ms");
        
        String result = "==========Demo Result========\n";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startFormatted = sdf.format(new Date(startTime));
        String endFormatted = sdf.format(new Date(endTime));

        result += "StartTime : " + startFormatted + " , EndTime : " + endFormatted + "\n";
        
        result += "ElapsedTime : " + elapsed + " ms\n";
        
        result += "-------AgentRoutes--------\n";
        int agentNum = 1;
        for(String route : routes) {
        	result += route + "\n";
        }
        
        result += "-------PCInformation-------\n";
        for (Map.Entry<String, StaticPCInfo> entry : staticPCInfos.entrySet()) {
            String ip = entry.getKey();
            StaticPCInfo spi = entry.getValue();
            DynamicPCInfo max = Max.get(ip);
            DynamicPCInfo min = Min.get(ip);

            result += "IP : " + ip + "\n";
            if (spi != null) {
                result += spi.toString() + "\n";
            } else {
                result += "(no StaticPCInfo)\n";
            }
            result += "Max DynamicPCInfo : " + (max != null ? max.toString() : "(no Max)") + "\n";
            result += "Min DynamicPCInfo : " + (min != null ? min.toString() : "(no Min)") + "\n";
        }

        SimpleDateFormat filesdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String logTime = filesdf.format(new Date(startTime));
        String fileName = "DemoLog/" + logTime + "DemoResult.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return resultList;
    }
    private static DynamicPCInfo.GPU copyGpu(DynamicPCInfo.GPU s) {
        DynamicPCInfo.GPU d = new DynamicPCInfo.GPU();
        d.Name        = s.Name;
        d.LoadPercent = s.LoadPercent;
        d.UsedMemory  = s.UsedMemory;
        d.TotalMemory = s.TotalMemory;
        d.TemperatureC= s.TemperatureC;
        return d;
    }

    private static DynamicPCInfo.NetworkCard copyNetCard(DynamicPCInfo.NetworkCard s) {
        DynamicPCInfo.NetworkCard d = new DynamicPCInfo.NetworkCard();
        d.UploadSpeed   = s.UploadSpeed;
        d.DownloadSpeed = s.DownloadSpeed;
        d.SentByte      = s.SentByte;
        d.ReceivedByte  = s.ReceivedByte;
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