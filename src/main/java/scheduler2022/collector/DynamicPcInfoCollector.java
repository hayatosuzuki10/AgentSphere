package scheduler2022.collector;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;

import com.sun.management.OperatingSystemMXBean;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.VirtualMemory;
import primula.api.AgentAPI;
import primula.api.core.agent.AgentInstanceInfo;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.DynamicPCInfo.Agent;
import scheduler2022.DynamicPCInfo.CPU;
import scheduler2022.DynamicPCInfo.GC;
import scheduler2022.DynamicPCInfo.GPU;
import scheduler2022.DynamicPCInfo.Memory;
import scheduler2022.DynamicPCInfo.NetworkCard;
import scheduler2022.DynamicPCInfo.NetworkSpeed;
import scheduler2022.JudgeOS;
import scheduler2022.network.NetworkSpeedManager;

public class DynamicPcInfoCollector {

    private final SystemInfo si = new SystemInfo();
    private final HardwareAbstractionLayer hal = si.getHardware();

    // 差分計算用に前回値を覚えておく（必要なら）
    private Map<String, NetworkCard> prevNetworkCards = new HashMap<>();
    private DynamicPCInfo previousDpi = null;

    public DynamicPCInfo collect(Set<String> aliveIPs,
            int receiverPort,
            boolean first,
            long jfrGcCount,
            long jfrGcPauseMillis) {

DynamicPCInfo dpi = new DynamicPCInfo();

// 1. LoadAverage
dpi.LoadAverage = collectLoadAverage();

// 2. CPU
dpi.CPU = collectCpu();

// 3. Memory
dpi.Memory = collectMemory();

// 4. FreeMemory (互換用・旧API用)
dpi.FreeMemory = (dpi.Memory != null)
? dpi.Memory.HostAvailableBytes
: Runtime.getRuntime().freeMemory();

// 5. GC
dpi.GCStats = collectGc(previousDpi, jfrGcCount, jfrGcPauseMillis);

// 6. LoadedClass
dpi.LoadedClass = collectLoadedClassCount();

// 7. GPUs + mainGPU
Map<String, GPU> gpus = collectGpus();
dpi.GPUs = gpus;
dpi.mainGPU = pickMainGpu(gpus);   // ★ 追加

// 8. NetworkCards（差分込み）+ 全 NIC 合計
Map<String, NetworkCard> nics = collectNetworkCards(prevNetworkCards, first);
dpi.NetworkCards = nics;
long[] totals = sumNetworkThroughput(nics);   // ★ 追加
dpi.allNetworkUp   = totals[0];
dpi.allNetworkDown = totals[1];

prevNetworkCards = nics;

// 9. NetworkSpeeds（IP 間）
dpi.NetworkSpeeds = collectNetworkSpeeds(aliveIPs, receiverPort);

// 10. Agents
dpi.Agents = collectAgents();
dpi.AgentsNum = (dpi.Agents != null) ? dpi.Agents.size() : 0;

// 11. ソケットバイトは、外から埋めてもらう or 別の collector で
// dpi.socketReadBytes  = ...
// dpi.socketWriteBytes = ...

dpi.timeStanp = System.currentTimeMillis();
previousDpi = dpi.deepCopy();   // 差分計算用に保持（必要なら）

return dpi;
}

    /* ======================== 各種収集メソッド ======================== */

    private double collectLoadAverage() {
        CentralProcessor proc = hal.getProcessor();

        // OSHI のロードアベレージ（1分値）を使う
        double value = -1.0;
        double[] la = proc.getSystemLoadAverage(1);
        if (la != null && la.length > 0) {
            value = la[0];
        }

        // OSHI が取れなかった場合だけ MXBean の値にフォールバック
        if (value < 0) {
            OperatingSystemMXBean osMx =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            value = osMx.getSystemLoadAverage();
        }

        if (JudgeOS.isWindows() && value >= 0) {
            // 以前の「+1.0 ハック」を残したいならここ
            value += 1.0;
        }

        return value;
    }

    private CPU collectCpu() {
        CPU cpu = new CPU();
        OperatingSystemMXBean osMx =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        cpu.AvailableProcessors = osMx.getAvailableProcessors();

        CentralProcessor proc = hal.getProcessor();
        long freq = proc.getProcessorIdentifier().getVendorFreq();
        if (freq <= 0) {
            freq = proc.getMaxFreq();
        }
        cpu.ClockSpeed = freq;

        cpu.LoadPercentByMXBean = osMx.getSystemCpuLoad();
        try {
            cpu.ProcessCpuLoad = osMx.getProcessCpuLoad();
        } catch (Throwable ignore) {}

        // JFR 側から渡されるなら、その値をセットするようにしてもOK
        // cpu.jvmSystem = ...
        // cpu.jvmUser   = ...
        // cpu.total     = ...

        return cpu;
    }

    private Memory collectMemory() {
        Memory out = new Memory();

        // Host
        GlobalMemory gm = hal.getMemory();
        out.HostTotalBytes = gm.getTotal();
        out.HostAvailableBytes = gm.getAvailable();

        VirtualMemory vm = gm.getVirtualMemory();
        out.SwapTotalBytes = vm.getSwapTotal();
        out.SwapUsedBytes = vm.getSwapUsed();

        // JVM
        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = m.getHeapMemoryUsage();
        MemoryUsage non  = m.getNonHeapMemoryUsage();
        out.JvmHeapUsed = heap.getUsed();
        out.JvmHeapCommitted = heap.getCommitted();
        out.JvmHeapMax = heap.getMax();
        out.JvmNonHeapUsed = non.getUsed();
        out.JvmNonHeapCommitted = non.getCommitted();

        return out;
    }

    private GC collectGc(DynamicPCInfo previous,
                         long jfrGcCount,
                         long jfrGcPauseMillis) {
        return GC.collect(previous, jfrGcCount, jfrGcPauseMillis);
    }

    private int collectLoadedClassCount() {
        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();
        return cl.getLoadedClassCount();
    }

    private Map<String, GPU> collectGpus() {
        Map<String, GPU> gpus = new HashMap<>();
        List<GraphicsCard> cards = hal.getGraphicsCards();

        for (GraphicsCard card : cards) {
            // OS やベンダでフィルタするなら既存ロジックに合わせる
            if (JudgeOS.OSname().contains("macOS")) {
                continue;
            }
            if (card.getVendor() == null || !card.getVendor().contains("NVIDIA")) {
                continue;
            }

            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "nvidia-smi",
                        "--query-gpu=index,utilization.gpu,memory.used,memory.total,memory.free,temperature.gpu",
                        "--format=csv,noheader,nounits"
                );
                Process process = pb.start();
                try (java.io.BufferedReader reader =
                             new java.io.BufferedReader(
                                     new java.io.InputStreamReader(process.getInputStream())
                             )) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] t = line.trim().split(",\\s*");
                        if (t.length < 6) continue;

                        GPU gpu = new GPU();
                        gpu.Name        = card.getName();
                        gpu.LoadPercent = parseIntSafe(t[1]);
                        gpu.UsedMemory  = parseIntSafe(t[2]);
                        gpu.TotalMemory = parseIntSafe(t[3]);
                        gpu.TemperatureC= parseIntSafe(t[5]);
                        gpus.put(gpu.Name, gpu);
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace(); // ログライブラリでWARNにしても良い
            }
        }
        return gpus;
    }
    
    private GPU pickMainGpu(Map<String, GPU> gpus) {
        if (gpus == null || gpus.isEmpty()) return null;

        GPU best = null;
        for (GPU gpu : gpus.values()) {
            if (gpu == null) continue;
            if (best == null || gpu.LoadPercent > best.LoadPercent) {
                best = gpu;
            }
        }
        return best;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private Map<String, NetworkCard> collectNetworkCards(Map<String, NetworkCard> previous,
                                                         boolean first) {
        Map<String, NetworkCard> out = new HashMap<>();
        List<NetworkIF> nifs = hal.getNetworkIFs(true);

        for (NetworkIF nif : nifs) {
            NetworkCard nc = new NetworkCard();

            long prevTx = 0, prevRx = 0;
            if (!first && previous != null) {
                NetworkCard p = previous.get(nif.getName());
                if (p != null) {
                    prevTx = p.SentByte != null ? p.SentByte : 0;
                    prevRx = p.ReceivedByte != null ? p.ReceivedByte : 0;
                }
            }

            long curTx = nif.getBytesSent();
            long curRx = nif.getBytesRecv();

            nc.SentByte      = curTx;
            nc.ReceivedByte  = curRx;
            nc.UploadSpeed   = Math.max(0, curTx - prevTx);
            nc.DownloadSpeed = Math.max(0, curRx - prevRx);

            out.put(nif.getName(), nc);
        }

        return out;
    }
    private long[] sumNetworkThroughput(Map<String, NetworkCard> nics) {
        long up = 0L;
        long down = 0L;
        if (nics == null) return new long[]{0L, 0L};

        for (NetworkCard nc : nics.values()) {
            if (nc == null) continue;
            if (nc.UploadSpeed != null) {
                up += nc.UploadSpeed;
            }
            if (nc.DownloadSpeed != null) {
                down += nc.DownloadSpeed;
            }
        }
        return new long[]{up, down};
    }

    private Map<String, NetworkSpeed> collectNetworkSpeeds(Set<String> aliveIPs,
                                                           int receiverPort) {
        Map<String, NetworkSpeed> speeds = new HashMap<>();
        if (aliveIPs == null || aliveIPs.isEmpty()) return speeds;

        for (String ip : aliveIPs) {
            if (Objects.equals(ip, IPAddress.myIPAddress)) continue;
            NetworkSpeed ns = new NetworkSpeed();
            ns.Sender   = IPAddress.myIPAddress;
            ns.Receiver = ip;

            double[] speed = NetworkSpeedManager.getSpeed(ip, receiverPort);
            ns.UploadSpeedByOriginal   = speed != null && speed.length > 0 ? speed[0] : null;
            ns.DownloadSpeedByOriginal = speed != null && speed.length > 1 ? speed[1] : null;
            // iperf3 も使うならここで埋める
            speeds.put(ns.Receiver, ns);
        }

        return speeds;
    }
    

    private Map<String, Agent> collectAgents() {
        Map<String, Agent> agents = new HashMap<>();

        Map<String, List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();

        List<String> bootAgents = new ArrayList<>();
        try {
            S2Container factory =
                    S2ContainerFactory.create("setting/StartupAgent.dicon"); // ← "./" を外すのもポイント
            for (int i = 0; i < factory.getComponentDefSize(); i++) {
                Object agent = factory.getComponentDef(i).getComponent();
                bootAgents.add(agent.getClass().getName());
            }
        } catch (org.seasar.framework.exception.ResourceNotFoundRuntimeException e) {
            // dicon が無い環境（Maven 版など）ではここに来る
            System.err.println(
                "[WARN] StartupAgent.dicon not found. bootAgents filter is disabled."
            );
            // bootAgents は空のまま → 何もフィルタしない or 必要なら別ロジック
        }

        Map<Long, Long> allocNew = Collections.emptyMap();
        Map<Long, Long> allocOutside = Collections.emptyMap();

        for (String key : agentInfos.keySet()) {
            for (AgentInstanceInfo info : agentInfos.get(key)) {
                if (bootAgents.contains(info.getAgentName())) continue;

                Agent a = new Agent();
                long threadId = info.getThreadId();
                a.ID        = info.getAgentId();
                a.Name      = info.getAgentName();
                a.StartTime = info.getTime();
                a.allocNew      = allocNew.getOrDefault(threadId, 0L);
                a.allocOutside  = allocOutside.getOrDefault(threadId, 0L);
                agents.put(a.ID, a);
            }
        }
        return agents;
    }
}