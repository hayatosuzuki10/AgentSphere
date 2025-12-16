package scheduler2022.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import primula.agent.AbstractAgent;
import primula.api.core.agent.AgentInfo;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;
import scheduler2022.util.DHTutil;

public class ScoreBasedStrategy implements SchedulerStrategy {

    // ★ 予測更新の同時実行を防ぐグローバルロック
    private static final Object PREDICTION_LOCK = new Object();

    @Override
    public void initialize() {}

    @Override
    public void excuteMainLogic() {}

    @Override
    public void cleanUp() {}

    @Override
    public boolean shouldMove(AbstractAgent agent) {
        // とりあえず常に「移動候補」とする（実際に移動するかは getDestination 側の判定）
        return true;
    }

    @Override
    public String getDestination(AbstractAgent agent) {
        synchronized (PREDICTION_LOCK) {

            String qualifiedBestMatchIP   = IPAddress.myIPAddress;
            String unqualifiedBestMatchIP = IPAddress.myIPAddress;
            double qualifiedMaxMatchScore   = Double.NEGATIVE_INFINITY;
            double unqualifiedMaxMatchScore = Double.NEGATIVE_INFINITY;

            DynamicPCInfo myDynamicPCInfo = Scheduler.getDpis().get(IPAddress.myIPAddress);
            StaticPCInfo  myStaticPCInfo  = Scheduler.getSpis().get(IPAddress.myIPAddress);

            if (myDynamicPCInfo == null || myStaticPCInfo == null) {
                // 自ノードの情報すらない場合は安全側で「移動しない」
                System.out.println("[ScoreBased] my Dynamic/Static info is null, stay.");
                return IPAddress.myIPAddress;
            }

            double myMatchScore = calculateMatchScore(agent, myDynamicPCInfo, myStaticPCInfo);

            Set<String> ips = Scheduler.getAliveIPs();
            Map<String, Double> qualifiedHosts   = new HashMap<>();
            Map<String, Double> unqualifiedHosts = new HashMap<>();

            for (String ip : ips) {
                DynamicPCInfo dynamicPCInfo = Scheduler.getDpis().get(IPAddress.myIPAddress);
                StaticPCInfo  staticPCInfo  = Scheduler.getSpis().get(ip);

                // ★ dpi / spi が揃っていないノードはスキップ
                if (dynamicPCInfo == null || staticPCInfo == null) {
                    System.out.printf("[ScoreBased] skip ip=%s dpi=%s spi=%s%n",
                            ip, dynamicPCInfo, staticPCInfo);
                    continue;
                }

                // 需要を満たせるかどうか
                boolean meet = hasMeetDemand(agent, dynamicPCInfo, staticPCInfo);
                double matchScore = calculateMatchScore(agent, dynamicPCInfo, staticPCInfo);

                if (meet) {
                    qualifiedHosts.put(ip, matchScore);
                    if (qualifiedMaxMatchScore < matchScore) {
                        qualifiedMaxMatchScore = matchScore;
                        qualifiedBestMatchIP   = ip;
                    }
                } else {
                    unqualifiedHosts.put(ip, matchScore);
                    if (unqualifiedMaxMatchScore < matchScore) {
                        unqualifiedMaxMatchScore = matchScore;
                        unqualifiedBestMatchIP   = ip;
                    }
                }
            }

            // ログ出力（どのノードがどのくらいのスコアか）
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            System.out.println("[ScoreBased] self IP   : " + IPAddress.myIPAddress);
            System.out.println("[ScoreBased] selfScore: " + myMatchScore);

            System.out.println("~~~~~~~~QUALIFIED~~~~~~~~~");
            for (Map.Entry<String, Double> e : qualifiedHosts.entrySet()) {
                System.out.println("score " + e.getValue() + " : IP " + e.getKey());
            }

            System.out.println("~~~~~~~~UNQUALIFIED~~~~~~~~~");
            for (Map.Entry<String, Double> e : unqualifiedHosts.entrySet()) {
                System.out.println("score " + e.getValue() + " : IP " + e.getKey());
            }
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            String bestMatchIP = IPAddress.myIPAddress;

            // ★ しきい値を超えるノードだけ移動候補にする
            if (!qualifiedHosts.isEmpty()) {
                if (qualifiedMaxMatchScore > myMatchScore + Scheduler.scoreThreshold
                        && !qualifiedBestMatchIP.equals(IPAddress.myIPAddress)) {
                    bestMatchIP = qualifiedBestMatchIP;
                }
            } else if (!unqualifiedHosts.isEmpty()) {
                if (unqualifiedMaxMatchScore > myMatchScore + Scheduler.scoreThreshold
                        && !unqualifiedBestMatchIP.equals(IPAddress.myIPAddress)) {
                    bestMatchIP = unqualifiedBestMatchIP;
                }
            }

            // ★ 自ノード以外に移動する場合だけ予測を反映
            if (!bestMatchIP.equals(IPAddress.myIPAddress)) {
                DynamicPCInfo before = Scheduler.getDpis().get(bestMatchIP);
                if (before != null) {
                    before = before.deepCopy();
                    setTemporaryPrediction(agent, bestMatchIP);
                    DynamicPCInfo after = Scheduler.getDpis().get(bestMatchIP);

                    if (after != null) {
                        System.out.printf(
                                "[CHECK] dst=%s CPU: %.3f -> %.3f  Agents: %d -> %d%n",
                                bestMatchIP,
                                before.CPU != null ? before.CPU.LoadPercentByMXBean : -1.0,
                                after.CPU != null ? after.CPU.LoadPercentByMXBean : -1.0,
                                before.AgentsNum,
                                after.AgentsNum
                        );
                    }
                }
            }

            return bestMatchIP;
        }
    }

    /**
     * 予測値を DynamicPCInfo に一時的に反映する。
     */
    private void setTemporaryPrediction(AbstractAgent agent, String destination) {

        StaticPCInfo  spi     = Scheduler.getSpis().get(destination);
        DynamicPCInfo prevDPI = Scheduler.getDpis().get(destination);
        AgentInfo     agentInfo = DHTutil.getAgentInfo(agent.getAgentID());

        if (spi == null || prevDPI == null || agentInfo == null || prevDPI.CPU == null) {
            System.out.printf(
                    "[PREDICT-SKIP] dst=%s spi=%s dpi=%s agentInfo=%s cpu=%s%n",
                    destination, spi, prevDPI, agentInfo, (prevDPI != null ? prevDPI.CPU : null)
            );
            return;
        }

        System.out.printf(
                "[PREDICT] dst=%s agent=%s beforeCPU=%.6f cpuChange=%d bench=%d%n",
                destination, agent.getAgentID(),
                prevDPI.CPU.LoadPercentByMXBean,
                agentInfo.cpuChange,
                spi.CPU.BenchMarkScore
        );

        // ★ CPU 予測
        if (agentInfo.cpuChange > 0) {
            double delta = (double) agentInfo.cpuChange / (double) spi.CPU.BenchMarkScore;

            double factor = 2.0; // 効きを強めたいなら調整
            delta *= factor;

            System.out.printf(
                    "[PREDICT-CPU] dst=%s agent=%s delta=%.6f before=%.6f",
                    destination, agent.getAgentID(), delta, prevDPI.CPU.LoadPercentByMXBean
            );

            prevDPI.CPU.LoadPercentByMXBean += delta;

            System.out.printf(" after=%.6f%n", prevDPI.CPU.LoadPercentByMXBean);
        }

        // ★ メモリ（ここでは heapChange を代表値として使う）
        if (agentInfo.heapChange > 0 && prevDPI.Memory != null) {
            prevDPI.Memory.JvmHeapUsed += agentInfo.heapChange;
        }

        // ★ GPU
        if (agentInfo.gpuChange > 0 && prevDPI.mainGPU != null && spi.GPUs != null) {
            StaticPCInfo.GPU staticGpu = spi.GPUs.get(prevDPI.mainGPU.Name);
            if (staticGpu != null) {
                prevDPI.mainGPU.LoadPercent += agentInfo.gpuChange / staticGpu.BenchMarkScore;
            }
        }

        // ★ ネットワーク
        if (agentInfo.networkUpChange > 0) {
            prevDPI.allNetworkUp += agentInfo.networkUpChange;
        }
        if (agentInfo.networkDownChange > 0) {
            prevDPI.allNetworkDown += agentInfo.networkDownChange;
        }

        // ★ エージェント数（予測上 +1）
        prevDPI.AgentsNum = prevDPI.AgentsNum + 1;

        prevDPI.isForecast = true;
        prevDPI.timeStanp  = System.currentTimeMillis();

        Scheduler.getDpis().put(destination, prevDPI);
    }

    /* ===================== 需要判定 ===================== */

    private boolean hasMeetDemand(
            AbstractAgent agent,
            DynamicPCInfo dynamicPCInfo,
            StaticPCInfo staticPCInfo
    ) {
        if (dynamicPCInfo == null || staticPCInfo == null) {
            return false;
        }

        AgentInfo info = DHTutil.getAgentInfo(agent.getAgentID());
        if (info == null) return false;

        return hasMeetCPUDemand(info.cpuChange, dynamicPCInfo.CPU, staticPCInfo.CPU)
                && hasMeetGPUDemand(info.gpuChange, dynamicPCInfo.GPUs, staticPCInfo.GPUs)
                && hasMeetNetDemand(info.networkUpChange, dynamicPCInfo.NetworkCards, staticPCInfo.NetworkCards);
    }

    private boolean hasMeetCPUDemand(
            int cpuPerfChange,
            DynamicPCInfo.CPU dynamicCPUInfo,
            StaticPCInfo.CPU staticCPUInfo
    ) {
        if (dynamicCPUInfo == null || staticCPUInfo == null) return false;

        int cpuPerfLimit = staticCPUInfo.BenchMarkScore;
        int cpuPerfNow   = (int) (dynamicCPUInfo.LoadPercentByMXBean * cpuPerfLimit);
        return cpuPerfNow + cpuPerfChange < cpuPerfLimit;
    }

    private boolean hasMeetGPUDemand(
            int gpuPerfChange,
            Map<String, DynamicPCInfo.GPU> dynamicGPUInfos,
            Map<String, StaticPCInfo.GPU> staticGPUInfos
    ) {
        // GPU 情報がどちらかでも無ければ、GPU 要求は満たせないとみなす
        if (dynamicGPUInfos == null || dynamicGPUInfos.isEmpty()) return false;
        if (staticGPUInfos == null || staticGPUInfos.isEmpty()) return false;

        for (Map.Entry<String, DynamicPCInfo.GPU> entry : dynamicGPUInfos.entrySet()) {
            if (entry == null) continue;

            String id = entry.getKey();
            DynamicPCInfo.GPU dyn = entry.getValue();
            if (id == null || dyn == null) continue;

            StaticPCInfo.GPU stat = staticGPUInfos.get(id);
            if (stat == null) continue;

            int gpuPerfLimit = stat.BenchMarkScore;
            if (gpuPerfLimit <= 0) continue;

            int gpuPerfNow = dyn.LoadPercent * gpuPerfLimit;

            // どれか 1 つでも条件を満たせば OK という設計
            if (gpuPerfNow + gpuPerfChange < gpuPerfLimit) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMeetNetDemand(
            long netChange,
            Map<String, DynamicPCInfo.NetworkCard> dynamicNetInfos,
            Map<String, StaticPCInfo.NetworkCard> staticNetInfos
    ) {
        if (dynamicNetInfos == null || staticNetInfos == null) return false;

        long netAllNow   = 0;
        long netAllLimit = 0;

        for (Map.Entry<String, DynamicPCInfo.NetworkCard> card : dynamicNetInfos.entrySet()) {
            String iface = card.getKey();
            DynamicPCInfo.NetworkCard dyn = card.getValue();
            if (iface == null || dyn == null) continue;

            StaticPCInfo.NetworkCard stat = staticNetInfos.get(iface);
            if (stat == null) continue;

            netAllLimit += (long) (stat.Bandwidth * 0.9);
            if (dyn.UploadSpeed != null) {
                netAllNow += dyn.UploadSpeed;
            }
        }

        return netAllNow + netChange < netAllLimit;
    }

    /* ===================== マッチングスコア ===================== */

    private double calculateMatchScore(
            AbstractAgent agent,
            DynamicPCInfo dynamicPCInfo,
            StaticPCInfo staticPCInfo
    ) {
        if (dynamicPCInfo == null || staticPCInfo == null
                || dynamicPCInfo.CPU == null || staticPCInfo.CPU == null) {
            return Double.NEGATIVE_INFINITY;
        }

        AgentInfo info = DHTutil.getAgentInfo(agent.getAgentID());
        if (info == null) return 0.0;

        double matchScore = 0.0;

        // CPU
        double normalizedCPUBench = (staticPCInfo.CPU.BenchMarkScore - 8000.0) / 60000.0;
        matchScore += info.cpuChange
                * (1.0 - dynamicPCInfo.CPU.LoadPercentByMXBean)
                * normalizedCPUBench;

        // GPU（簡単な例：全 GPU の空き度合いを足し込む）
        if (dynamicPCInfo.GPUs != null && staticPCInfo.GPUs != null) {
            double gpuScore = 0.0;
            for (Map.Entry<String, DynamicPCInfo.GPU> e : dynamicPCInfo.GPUs.entrySet()) {
                String id = e.getKey();
                DynamicPCInfo.GPU dyn = e.getValue();
                if (id == null || dyn == null) continue;

                StaticPCInfo.GPU stat = staticPCInfo.GPUs.get(id);
                if (stat == null || stat.BenchMarkScore <= 0) continue;

                double gpuLoad = dyn.LoadPercent / 100.0; // 0〜1 に正規化している前提
                gpuScore += (1.0 - gpuLoad) * stat.BenchMarkScore;
            }
            matchScore += gpuScore;
        }

        // Memory
        double normalizedFreeMemory =
                (dynamicPCInfo.FreeMemory - 1_000_000_000L) / 64_000_000_000.0;
        double normalizedMemoryDemand =
                (info.memoryChange - 1_000_000_000L) / 64_000_000_000.0; // ★ networkUpChange から修正

        matchScore += normalizedMemoryDemand * (1.0 - normalizedFreeMemory);

        // Network
        double networkSpeedScore = 0.0;
        if (dynamicPCInfo.NetworkCards != null) {
            for (String name : dynamicPCInfo.NetworkCards.keySet()) {
                DynamicPCInfo.NetworkCard card = dynamicPCInfo.NetworkCards.get(name);
                if (card == null) continue;

                if (card.UploadSpeed != null && card.UploadSpeed != 0) {
                    networkSpeedScore += card.UploadSpeed;
                    if (card.DownloadSpeed != null) {
                        networkSpeedScore += card.DownloadSpeed;
                    }
                }
            }
        }

        double normalizedNetworkSpeed  = networkSpeedScore / 100000.0;
        double normalizedNetworkDemand = info.networkUpChange / 100000.0;
        matchScore += normalizedNetworkDemand * (1.0 - normalizedNetworkSpeed);

        // LoadAverage と CPU コア数
        matchScore += info.priority
                * (1.0 - dynamicPCInfo.LoadAverage / staticPCInfo.CPU.LogicalCore);

        // 移動コスト
        double migrateCostWeight     = 0.5;
        double normalizedMigrateCost = (info.migrateTime - 10) / 5000.0;

        double networkMigrateSpeedScore = 0.0;
        if (dynamicPCInfo.NetworkSpeeds != null) {
            for (Map.Entry<String, DynamicPCInfo.NetworkSpeed> e : dynamicPCInfo.NetworkSpeeds.entrySet()) {
                if (IPAddress.myIPAddress.equals(e.getKey())) { // ★ == から equals へ
                    DynamicPCInfo.NetworkSpeed ns = e.getValue();
                    if (ns.DownloadSpeedByOriginal != null) {
                        networkMigrateSpeedScore += ns.DownloadSpeedByOriginal;
                    }
                    if (ns.UploadSpeedByOriginal != null) {
                        networkMigrateSpeedScore += ns.UploadSpeedByOriginal;
                    }
                }
            }
        }

        double normalizedMigrateSpeed = (networkMigrateSpeedScore - 20.0) / 10000.0;
        matchScore -= migrateCostWeight * normalizedMigrateCost * (1.0 - normalizedMigrateSpeed);

        // エージェント数ペナルティ
        double agentCountWeight     = 0.9;
        double normalizedAgentCount = dynamicPCInfo.AgentsNum / 100.0; // ★ 100.0 に修正
        matchScore -= agentCountWeight * normalizedAgentCount;

        return matchScore;
    }

}