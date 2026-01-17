package scheduler2022.strategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import primula.agent.AbstractAgent;
import primula.api.core.agent.AgentClassInfo;
import primula.api.core.agent.AgentInstanceInfo;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;
import scheduler2022.util.DHTutil;

public class ScoreBasedStrategy implements SchedulerStrategy {

    private static final Object PREDICTION_LOCK = new Object();

    /* ===== 安定化用パラメータ ===== */
    private static final long PCINFO_TTL_MS = 5_000;      // 情報の鮮度
    private static final long IP_CACHE_TTL_MS = 5_000;
    private static final long BLACKLIST_MS = 10_000;

    private Set<String> cachedIPs = new HashSet<>();
    private long lastIPFetchTime = 0;

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public boolean shouldMove(AbstractAgent agent) {
        return true;
    }

    @Override
    public String getDestination(AbstractAgent agent) {
        synchronized (PREDICTION_LOCK) {

            String selfIP = IPAddress.myIPAddress;
            String bestIP = selfIP;
            double bestScore = Double.NEGATIVE_INFINITY;

            DynamicPCInfo myDyn = DHTutil.getPcInfo(selfIP);
            StaticPCInfo mySta = DHTutil.getStaticPCInfo(selfIP);

            if (!isValid(myDyn) || mySta == null) {
                return selfIP;
            }

            double myScore = calculateMatchScore(agent, myDyn, mySta);

            for (String ip : getAliveIPs()) {
                if (ip.equals(selfIP)) continue;
                if (isBlacklisted(ip)) continue;

                try {
                    DynamicPCInfo dyn = DHTutil.getPcInfo(ip);
                    StaticPCInfo sta = DHTutil.getStaticPCInfo(ip);

                    if (!isValid(dyn) || sta == null) continue;
                    if (!hasMeetDemand(agent, dyn, sta)) continue;

                    double score = calculateMatchScore(agent, dyn, sta);
                    if (score > bestScore) {
                        bestScore = score;
                        bestIP = ip;
                    }

                } catch (Exception e) {
                    blacklist(ip);
                    System.out.println("[SCORE-SKIP] " + ip + " " + e.getClass().getSimpleName());
                }
            }

            if (bestScore > myScore + Scheduler.scoreThreshold) {
                setTemporaryPrediction(agent, bestIP);
                return bestIP;
            }

            return selfIP;
        }
    }

    /* ================= ユーティリティ ================= */

    private boolean isValid(DynamicPCInfo dpi) {
        if (dpi == null) return false;
        if (System.currentTimeMillis() - dpi.timeStanp > PCINFO_TTL_MS) return false;
        return true;
    }

    private Set<String> getAliveIPs() {
        long now = System.currentTimeMillis();
        if (now - lastIPFetchTime < IP_CACHE_TTL_MS && !cachedIPs.isEmpty()) {
            return cachedIPs;
        }
        try {
            cachedIPs = DHTutil.getAllSuvivalIPaddresses();
            lastIPFetchTime = now;
        } catch (Exception e) {
            System.out.println("[IP-CACHE] fallback");
        }
        return cachedIPs;
    }

    private boolean isBlacklisted(String ip) {
        Long until = blacklist.get(ip);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            blacklist.remove(ip);
            return false;
        }
        return true;
    }

    private void blacklist(String ip) {
        blacklist.put(ip, System.currentTimeMillis() + BLACKLIST_MS);
    }

    /* ================= 予測 ================= */

    private void setTemporaryPrediction(AbstractAgent agent, String dst) {
        try {
            StaticPCInfo dstSpi = DHTutil.getStaticPCInfo(dst);
            StaticPCInfo mySpi  = DHTutil.getStaticPCInfo(IPAddress.myIPAddress);

            DynamicPCInfo dstDpi = DHTutil.getPcInfo(dst);
            DynamicPCInfo myDpi  = DHTutil.getPcInfo(IPAddress.myIPAddress);

            AgentClassInfo info = DHTutil.getAgentInfo(agent.getClass().getName());

            if (dstSpi == null || mySpi == null || dstDpi == null || myDpi == null || info == null) return;

            long now = System.currentTimeMillis();

            // ---- null safety ----
            if (dstDpi.CPU == null) dstDpi.CPU = new DynamicPCInfo.CPU();
            if (myDpi.CPU  == null) myDpi.CPU  = new DynamicPCInfo.CPU();

            if (dstDpi.Memory == null) dstDpi.Memory = new DynamicPCInfo.Memory();
            if (myDpi.Memory  == null) myDpi.Memory  = new DynamicPCInfo.Memory();

            if (dstDpi.GCStats == null) dstDpi.GCStats = new DynamicPCInfo.GC();
            if (myDpi.GCStats  == null) myDpi.GCStats  = new DynamicPCInfo.GC();

            if (dstDpi.GPUs == null) dstDpi.GPUs = new java.util.HashMap<>();
            if (myDpi.GPUs  == null) myDpi.GPUs  = new java.util.HashMap<>();

            if (dstDpi.NetworkCards == null) dstDpi.NetworkCards = new java.util.HashMap<>();
            if (myDpi.NetworkCards  == null) myDpi.NetworkCards  = new java.util.HashMap<>();

            // =========================
            // CPU: Detector に効くのは LoadPercentByMXBean と ProcessCpuLoad
            // =========================
            if (info.getCpuChange() > 0
                    && dstSpi.CPU != null && mySpi.CPU != null
                    && dstSpi.CPU.BenchMarkScore > 0 && mySpi.CPU.BenchMarkScore > 0) {

                double addDst = (double) info.getCpuChange() / dstSpi.CPU.BenchMarkScore;
                double subMy  = (double) info.getCpuChange() / mySpi.CPU.BenchMarkScore;

                // cpuPerf 用
                dstDpi.CPU.LoadPercentByMXBean += addDst;
                myDpi.CPU.LoadPercentByMXBean  -= subMy;

                // cpuProc 用（cpuProcThreshold を満たすために同方向で動かす）
                dstDpi.CPU.ProcessCpuLoad += addDst;
                myDpi.CPU.ProcessCpuLoad  -= subMy;

                // clamp
                dstDpi.CPU.LoadPercentByMXBean = clamp01(dstDpi.CPU.LoadPercentByMXBean);
                myDpi.CPU.LoadPercentByMXBean  = clamp01(myDpi.CPU.LoadPercentByMXBean);
                dstDpi.CPU.ProcessCpuLoad      = clamp01(dstDpi.CPU.ProcessCpuLoad);
                myDpi.CPU.ProcessCpuLoad       = clamp01(myDpi.CPU.ProcessCpuLoad);
            }

            // =========================
            // Memory: Detector は JvmHeapUsed / HostAvailableBytes / gcCountByJFR を見る
            // =========================
            if (info.getHeapChange() > 0) {
                dstDpi.Memory.JvmHeapUsed += info.getHeapChange();
                myDpi.Memory.JvmHeapUsed  -= info.getHeapChange();
                if (dstDpi.Memory.JvmHeapUsed < 0) dstDpi.Memory.JvmHeapUsed = 0;
                if (myDpi.Memory.JvmHeapUsed  < 0) myDpi.Memory.JvmHeapUsed  = 0;
            }

            if (info.getRealMemoryChange() > 0) {
                // HostAvailableBytes は「空き」なので、負荷増→空き減
                dstDpi.Memory.HostAvailableBytes -= info.getRealMemoryChange();
                myDpi.Memory.HostAvailableBytes  += info.getRealMemoryChange();
                if (dstDpi.Memory.HostAvailableBytes < 0) dstDpi.Memory.HostAvailableBytes = 0;
                if (myDpi.Memory.HostAvailableBytes  < 0) myDpi.Memory.HostAvailableBytes  = 0;
            }

            if (info.getGCCountChange() > 0) {
                dstDpi.GCStats.gcCountByJFR += info.getGCCountChange();
                myDpi.GCStats.gcCountByJFR  -= info.getGCCountChange();
                if (myDpi.GCStats.gcCountByJFR < 0) myDpi.GCStats.gcCountByJFR = 0;
            }

            // =========================
            // GPU: Detector は LoadPercent*Bench と UsedMemory を見る
            //      → gpuChange は LoadPercent 側に反映（*100しない）
            // =========================
            if (info.getGpuChange() > 0) {
                String dstGpuKey = pickGpuKey(dstDpi, dstSpi);
                String myGpuKey  = pickGpuKey(myDpi,  mySpi);

                if (dstGpuKey != null && myGpuKey != null) {
                    DynamicPCInfo.GPU dstGpu = ensureGpu(dstDpi, dstGpuKey);
                    DynamicPCInfo.GPU myGpu  = ensureGpu(myDpi,  myGpuKey);

                    int dstBench = getGpuBench(dstSpi, dstGpuKey);
                    int myBench  = getGpuBench(mySpi,  myGpuKey);

                    if (dstBench > 0 && myBench > 0) {
                        int addLoad = (int) Math.round((double) info.getGpuChange() / dstBench);
                        int subLoad = (int) Math.round((double) info.getGpuChange() / myBench);

                        dstGpu.LoadPercent += addLoad;
                        myGpu.LoadPercent  -= subLoad;
                    } else {
                        // bench 不明なら安全側：固定の小さめ変化（暴れ防止）
                        dstGpu.LoadPercent += 1;
                        myGpu.LoadPercent  -= 1;
                    }

                    dstGpu.LoadPercent = clampInt(dstGpu.LoadPercent, 0, 100);
                    myGpu.LoadPercent  = clampInt(myGpu.LoadPercent, 0, 100);
                }
            }

            // =========================
            // Network: Detector は UploadSpeed/DownloadSpeed を見る
            // =========================
            if (info.getNetworkUpChange() > 0 || info.getNetworkDownChange() > 0) {
                int dstNicCount = dstDpi.NetworkCards.size();
                int myNicCount  = myDpi.NetworkCards.size();

                if (dstNicCount > 0) {
                    long upEach = info.getNetworkUpChange() > 0 ? info.getNetworkUpChange() / dstNicCount : 0;
                    long dnEach = info.getNetworkDownChange() > 0 ? info.getNetworkDownChange() / dstNicCount : 0;
                    for (Map.Entry<String, DynamicPCInfo.NetworkCard> e : dstDpi.NetworkCards.entrySet()) {
                        DynamicPCInfo.NetworkCard nc = e.getValue();
                        if (nc == null) { nc = new DynamicPCInfo.NetworkCard(); dstDpi.NetworkCards.put(e.getKey(), nc); }
                        nc.UploadSpeed   = safeLong(nc.UploadSpeed)   + upEach;
                        nc.DownloadSpeed = safeLong(nc.DownloadSpeed) + dnEach;
                    }
                }

                if (myNicCount > 0) {
                    long upEach = info.getNetworkUpChange() > 0 ? info.getNetworkUpChange() / myNicCount : 0;
                    long dnEach = info.getNetworkDownChange() > 0 ? info.getNetworkDownChange() / myNicCount : 0;
                    for (Map.Entry<String, DynamicPCInfo.NetworkCard> e : myDpi.NetworkCards.entrySet()) {
                        DynamicPCInfo.NetworkCard nc = e.getValue();
                        if (nc == null) { nc = new DynamicPCInfo.NetworkCard(); myDpi.NetworkCards.put(e.getKey(), nc); }
                        nc.UploadSpeed   = Math.max(0L, safeLong(nc.UploadSpeed)   - upEach);
                        nc.DownloadSpeed = Math.max(0L, safeLong(nc.DownloadSpeed) - dnEach);
                    }
                }
            }

            // =========================
            // AgentsNum / forecast flags
            // =========================
            dstDpi.AgentsNum++;
            myDpi.AgentsNum--;

            dstDpi.isForecast = true;
            myDpi.isForecast  = true;
            dstDpi.timeStanp  = now;
            myDpi.timeStanp   = now;

            DHTutil.setPcInfo(dst, dstDpi);
            DHTutil.setPcInfo(IPAddress.myIPAddress, myDpi);

        } catch (Exception e) {
            blacklist(dst);
        }
    }

    /* ===== helpers ===== */

    private static double clamp01(double v){
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static int clampInt(int v, int lo, int hi){
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private static long safeLong(Long v){
        return v == null ? 0L : v;
    }

    private static String pickGpuKey(DynamicPCInfo dpi, StaticPCInfo spi){
        if (dpi != null && dpi.mainGPU != null && dpi.mainGPU.Name != null) return dpi.mainGPU.Name;
        if (dpi != null && dpi.GPUs != null && !dpi.GPUs.isEmpty()) return dpi.GPUs.keySet().iterator().next();
        if (spi != null && spi.GPUs != null && !spi.GPUs.isEmpty()) return spi.GPUs.keySet().iterator().next();
        return null;
    }

    private static DynamicPCInfo.GPU ensureGpu(DynamicPCInfo dpi, String key){
        DynamicPCInfo.GPU g = dpi.GPUs.get(key);
        if (g == null) {
            g = new DynamicPCInfo.GPU();
            g.Name = key;
            dpi.GPUs.put(key, g);
        }
        if (dpi.mainGPU == null) dpi.mainGPU = g;
        return g;
    }

    private static int getGpuBench(StaticPCInfo spi, String key){
        if (spi == null || spi.GPUs == null) return 0;
        StaticPCInfo.GPU g = spi.GPUs.get(key);
        return (g == null) ? 0 : g.BenchMarkScore;
    }

    /* ================= 判定系 ================= */

    private boolean hasMeetDemand(AbstractAgent agent,
                                  DynamicPCInfo dyn,
                                  StaticPCInfo sta) {

        AgentInstanceInfo info = Scheduler.agentInfo.get(agent.getAgentID());
        if (info == null) return false;

        return hasMeetCPUDemand(info.getCpuChange(), dyn.CPU, sta.CPU);
    }

    private boolean hasMeetCPUDemand(int cpuChange,
                                     DynamicPCInfo.CPU dyn,
                                     StaticPCInfo.CPU sta) {

        if (dyn == null || sta == null) return false;
        int now = (int) (dyn.LoadPercentByMXBean * sta.BenchMarkScore);
        return now + cpuChange < sta.BenchMarkScore;
    }

    /* ================= スコア ================= */

    private double calculateMatchScore(AbstractAgent agent,
                                       DynamicPCInfo dyn,
                                       StaticPCInfo sta) {

        AgentInstanceInfo info = Scheduler.agentInfo.get(agent.getAgentID());
        if (info == null) return 0;

        double score = 0;
        score += info.getCpuChange() * (1 - dyn.CPU.LoadPercentByMXBean);
        score -= dyn.AgentsNum * 0.9;
        score += info.getPriority() * (1 - dyn.LoadAverage / sta.CPU.LogicalCore);
        return score;
    }

	@Override
	public void initialize() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void excuteMainLogic() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void cleanUp() {
		// TODO 自動生成されたメソッド・スタブ
		
	}
}