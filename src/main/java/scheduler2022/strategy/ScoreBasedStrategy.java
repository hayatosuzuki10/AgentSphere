package scheduler2022.strategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import primula.agent.AbstractAgent;
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
            StaticPCInfo spi = DHTutil.getStaticPCInfo(dst);
            DynamicPCInfo dpi = DHTutil.getPcInfo(dst);
            AgentInstanceInfo info = Scheduler.agentInfo.get(agent.getAgentID());

            if (spi == null || dpi == null || info == null) return;

            if (info.getCpuChange() > 0) {
                dpi.CPU.LoadPercentByMXBean +=
                        (double) info.getCpuChange() / spi.CPU.BenchMarkScore;
            }

            dpi.AgentsNum++;
            dpi.isForecast = true;
            dpi.timeStanp = System.currentTimeMillis();
            DHTutil.setPcInfo(dst, dpi);

        } catch (Exception e) {
            blacklist(dst);
        }
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