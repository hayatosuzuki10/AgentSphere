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

public class ScoreBasedStrategy implements SchedulerStrategy{
	private static final Object PREDICTION_LOCK = new Object();

    
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

	@Override
	public boolean shouldMove(AbstractAgent agent) {
		return true;
        
	}
	@Override
    public String getDestination(AbstractAgent agent) {
        synchronized (PREDICTION_LOCK) {
        	String qualifiedBestMatchIP = IPAddress.myIPAddress;
    		String unqualifiedBestMatchIP = IPAddress.myIPAddress;
    		double qualifiedMaxMatchScore = 0;
    		double unqualifiedMaxMatchScore = 0;

    		DynamicPCInfo myDynamicPCInfo = DHTutil.getPcInfo(IPAddress.myIPAddress);
    		StaticPCInfo myStaticPCInfo = DHTutil.getStaticPCInfo(IPAddress.myIPAddress);
    		double myMatchScore = calculateMatchScore(agent, myDynamicPCInfo, myStaticPCInfo);
    		Set<String> ips = DHTutil.getAllSuvivalIPaddresses();
    		Map<String, Double> qualifiedHosts = new HashMap<>();
    		Map<String, Double> unqualifiedHosts = new HashMap<>();
    		
    		for(String ip: ips) {
    			DynamicPCInfo dynamicPCInfo = DHTutil.getPcInfo(ip);
    			StaticPCInfo staticPCInfo = DHTutil.getStaticPCInfo(ip);
    			if(hasMeetDemand(agent, dynamicPCInfo, staticPCInfo)) {
    				double matchScore = calculateMatchScore(agent, dynamicPCInfo, staticPCInfo);
    				qualifiedHosts.put(ip, matchScore);
    				if(qualifiedMaxMatchScore < matchScore) {
    					qualifiedBestMatchIP = ip;
    					qualifiedMaxMatchScore = matchScore;
    				}
    			} else {
    				double matchScore = calculateMatchScore(agent, dynamicPCInfo, staticPCInfo);
    				unqualifiedHosts.put(ip, matchScore);

    				if(unqualifiedMaxMatchScore < matchScore) {
    					unqualifiedBestMatchIP = ip;
    					unqualifiedMaxMatchScore = matchScore;
    				}
    			}
    		}
    		System.out.println("~~~~~~~~QUALIFIED~~~~~~~~~");
    		for(Map.Entry<String, Double> e: qualifiedHosts.entrySet()) {
    			System.out.println("score "+ e.getValue()+ " : IP "+ e.getKey());
    		}

    		System.out.println("~~~~~~~~UNQUALIFIED~~~~~~~~~");
    		for(Map.Entry<String, Double> e: unqualifiedHosts.entrySet()) {
    			System.out.println("score "+ e.getValue()+ " : IP "+ e.getKey());
    		}
    		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    		
    		String bestMatchIP = IPAddress.myIPAddress;

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
    		if(bestMatchIP != IPAddress.myIPAddress) {
    			DynamicPCInfo before = DHTutil.getPcInfo(bestMatchIP).deepCopy();
    			setTemporaryPrediction(agent, bestMatchIP);
    			DynamicPCInfo after = DHTutil.getPcInfo(bestMatchIP);

    			System.out.printf(
    			  "[CHECK] dst=%s CPU: %.3f -> %.3f  Agents: %d -> %d%n",
    			  bestMatchIP,
    			  before.CPU.LoadPercentByMXBean,
    			  after.CPU.LoadPercentByMXBean,
    			  before.AgentsNum,
    			  after.AgentsNum
    			);
    		}
    		System.out.printf(
    				  "[DEST] agent=%s myScore=%.3f best=%s bestScore=%.3f self=%s%n",
    				  agent.getAgentID(),
    				  myMatchScore,
    				  bestMatchIP,
    				  (!qualifiedHosts.isEmpty() ? qualifiedMaxMatchScore : unqualifiedMaxMatchScore),
    				  IPAddress.myIPAddress
    				);

    		
    		return bestMatchIP;
        }
    }
	private void setTemporaryPrediction(AbstractAgent agent, String destination) {
	    StaticPCInfo spi = DHTutil.getStaticPCInfo(destination);
	    DynamicPCInfo prevDPI = DHTutil.getPcInfo(destination);
	    AgentInfo agentInfo = DHTutil.getAgentInfo(agent.getAgentID());

	    if (spi == null || prevDPI == null || agentInfo == null) {
	        System.out.printf("[PREDICT-SKIP] dst=%s spi=%s dpi=%s agentInfo=%s%n",
	                destination, spi, prevDPI, agentInfo);
	        return;
	    }

	    // CPU
	    if (agentInfo.cpuChange > 0) {
	        double delta = (double) agentInfo.cpuChange /
	                       (double) spi.CPU.BenchMarkScore;

	        // 効きが弱いようなら係数を掛けてみる（例: ×2.0 とか）
	        double factor = 2.0;
	        delta *= factor;

	        System.out.printf(
	            "[PREDICT-CPU] dst=%s agent=%s cpuChange=%d bench=%d delta=%.6f before=%.6f",
	            destination, agent.getAgentID(),
	            agentInfo.cpuChange, spi.CPU.BenchMarkScore,
	            delta, prevDPI.CPU.LoadPercentByMXBean
	        );

	        prevDPI.CPU.LoadPercentByMXBean += delta;

	        System.out.printf(" after=%.6f%n", prevDPI.CPU.LoadPercentByMXBean);
	    }

	
		if(agentInfo.memoryChange > 0) {
			prevDPI.Memory.JvmHeapUsed += agentInfo.heapChange;
		}
		if(agentInfo.gpuChange > 0 && prevDPI.mainGPU != null) {
			prevDPI.mainGPU.LoadPercent += agentInfo.gpuChange/ spi.GPUs.get(prevDPI.mainGPU.Name).BenchMarkScore;
		}
		if(agentInfo.networkUpChange > 0) {
			prevDPI.allNetworkUp += agentInfo.networkUpChange;
		}
		if(agentInfo.networkDownChange > 0) {
			prevDPI.allNetworkDown += agentInfo.networkDownChange;
		}

	    // AgentsNum も増やすなら
	    prevDPI.AgentsNum = prevDPI.AgentsNum + 1;

	    prevDPI.isForecast = true;
	    prevDPI.timeStanp = System.currentTimeMillis();
	    DHTutil.setPcInfo(destination, prevDPI);
		
	}
	
	private boolean hasMeetDemand(
			AbstractAgent agent,
			DynamicPCInfo dynamicPCInfo,
			StaticPCInfo staticPCInfo
			) {
		
		boolean meet = true;
		
		AgentInfo info = DHTutil.getAgentInfo(agent.getAgentID());
		if(info == null) return false;
		meet = meet && hasMeetCPUDemand(info.cpuChange, dynamicPCInfo.CPU, staticPCInfo.CPU);
		meet = meet && hasMeetGPUDemand(info.gpuChange, dynamicPCInfo.GPUs, staticPCInfo.GPUs);
		meet = meet && hasMeetNetDemand(info.networkUpChange, dynamicPCInfo.NetworkCards, staticPCInfo.NetworkCards);
		
		return meet;
	}
	
	private boolean hasMeetCPUDemand(
			int cpuPerfChange,
			DynamicPCInfo.CPU dynamicCPUInfo,
			StaticPCInfo.CPU staticCPUInfo
			) {
		int cpuPerfLimit = staticCPUInfo.BenchMarkScore;
		int cpuPerfNow = (int) (dynamicCPUInfo.LoadPercentByMXBean * cpuPerfLimit);
		return cpuPerfNow + cpuPerfChange < cpuPerfLimit;
	}
	
	private boolean hasMeetGPUDemand(
	        int gpuPerfChange,
	        Map<String, DynamicPCInfo.GPU> dynamicGPUInfos,
	        Map<String, StaticPCInfo.GPU> staticGPUInfos
	) {
	    // GPU 情報がどちらかでも無ければ、GPU 要求は満たせないとみなす
	    if (dynamicGPUInfos == null || dynamicGPUInfos.isEmpty()) {
	        return false;
	    }
	    if (staticGPUInfos == null || staticGPUInfos.isEmpty()) {
	        return false;
	    }

	    for (Map.Entry<String, DynamicPCInfo.GPU> entry : dynamicGPUInfos.entrySet()) {
	        if (entry == null) {
	            continue;
	        }

	        String gpuId = entry.getKey();
	        DynamicPCInfo.GPU dyn = entry.getValue();
	        if (gpuId == null || dyn == null) {
	            continue;
	        }

	        // static 側に対応する GPU 情報が無ければスキップ
	        StaticPCInfo.GPU stat = staticGPUInfos.get(gpuId);
	        if (stat == null) {
	            continue;
	        }

	        int gpuPerfLimit = stat.BenchMarkScore;
	        if (gpuPerfLimit <= 0) {
	            // 0 以下なら意味のある制約として扱えないのでスキップ
	            continue;
	        }

	        int gpuPerfNow = (int) (dyn.LoadPercent * gpuPerfLimit);

	        // ★元のロジックを維持：どれか 1 つでも条件を満たせば true/false を即返す
	        return gpuPerfNow + gpuPerfChange < gpuPerfLimit;
	    }

	    // 有効な GPU 情報が 1 個も無かった場合は満たしていない扱い
	    return false;
	}
	
	private boolean hasMeetNetDemand(
			long netChange,
			Map<String, DynamicPCInfo.NetworkCard> dynamicNetInfos,
			Map<String, StaticPCInfo.NetworkCard> staticNetInfos
			) {
		long netAllNow = 0;
		long netAllLimit = 0;
		
		for(Map.Entry<String, DynamicPCInfo.NetworkCard> card : dynamicNetInfos.entrySet()) {
			netAllLimit += (long) (staticNetInfos.get(card.getKey()).Bandwidth * 0.9);
			netAllNow += card.getValue().UploadSpeed;
		}

		return netAllNow + netChange < netAllLimit;
	}
	

	private double calculateMatchScore(
			AbstractAgent agent,
			DynamicPCInfo dynamicPCInfo,
			StaticPCInfo staticPCInfo
			) {
		
		double matchScore = 0;
		AgentInfo info = DHTutil.getAgentInfo(agent.getAgentID());
		double nomalizedCPUBenchmarkScore = (staticPCInfo.CPU.BenchMarkScore - 8000.0) / 60000;
		if(info == null) {
			return 0;
		}
		matchScore += info.cpuChange * (1- dynamicPCInfo.CPU.LoadPercentByMXBean) * nomalizedCPUBenchmarkScore;
		
        Map<String,DynamicPCInfo.GPU> dynamicGPUInfos = dynamicPCInfo.GPUs;
        Map<String,StaticPCInfo.GPU> StaticGPUInfos = staticPCInfo.GPUs;
        for(Map.Entry<String, DynamicPCInfo.GPU> entry : dynamicGPUInfos.entrySet()) {
        	
        }
		
        double nomalizedFreeMemory = (dynamicPCInfo.FreeMemory - 1_000_000_000L) / 64_000_000_000.0;
        double nomalizedMemoryDemand = (info.networkUpChange - 1_000_000_000L) / 64_000_000_000.0;
        matchScore += nomalizedMemoryDemand * (1- nomalizedFreeMemory);
        
        double networkSpeedScore = 0;
        for(String networkName : dynamicPCInfo.NetworkCards.keySet()) {
        	if(dynamicPCInfo.NetworkCards.get(networkName).UploadSpeed != 0)
        		networkSpeedScore += dynamicPCInfo.NetworkCards.get(networkName).UploadSpeed;
    			networkSpeedScore += dynamicPCInfo.NetworkCards.get(networkName).DownloadSpeed;
        	
        }
        double nomalizedNetworkSpeed = networkSpeedScore / 100000;
        double nomalizedNetworkDemand = info.networkUpChange / 100000;
        matchScore += nomalizedNetworkDemand * (1- nomalizedNetworkSpeed);
        
        matchScore += info.priority * (1- dynamicPCInfo.LoadAverage / staticPCInfo.CPU.LogicalCore);
        
        double migrateCostWeight = 0.5;
        double nomalizedMigrateCost = (info.migrateTime - 10) / 5000;
        double networkMigrateSpeedScore = 0;
        for(Map.Entry<String, DynamicPCInfo.NetworkSpeed> entry : dynamicPCInfo.NetworkSpeeds.entrySet()) {
        	if(entry.getKey() == IPAddress.myIPAddress) {
        		networkMigrateSpeedScore += entry.getValue().DownloadSpeedByOriginal;
        		networkMigrateSpeedScore += entry.getValue().UploadSpeedByOriginal;
        	}
        }
        	
        double nomalizedMigrateSpeed = (networkMigrateSpeedScore - 20) / 10000;
        matchScore -= migrateCostWeight * nomalizedMigrateCost * (1- nomalizedMigrateSpeed);
        
        double agentCountWeight = 0.9;
        double nomalizedAgentCount = dynamicPCInfo.AgentsNum / 100;
        matchScore -= agentCountWeight * nomalizedAgentCount;
        
		return matchScore;
	}
	

}
