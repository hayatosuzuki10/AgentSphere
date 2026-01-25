package scheduler2022.strategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import primula.agent.AbstractAgent;
import primula.api.core.agent.AgentClassInfo;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.InformationCenter;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;
import scheduler2022.util.DHTutil;

public class ScoreBasedStrategy implements SchedulerStrategy {

    private static final Object PREDICTION_LOCK = new Object();

    /* ===== 安定化用パラメータ ===== */
    private static final long IP_CACHE_TTL_MS = 5_000;
    
    private final double MEMORY_LIMIT = 0.8;
    
    private static double cpuWeight = 4;
    private static double gpuWeight = 4;
    private static double memWeight = 3;
    private static double netWeight = 1;
    private static double ioWeight = 2;
    private static double laWeight = 10;
    private static double conWeight = 15;
    private static double migTimeWeight = 3;
    private static double migSpeedWeight = 1;
    private static double migCountWeight = 5;
    
    
    private class ScoreResult {
    	String ip;
    	double score;
    	String reason;
    	
    	public ScoreResult(String ip, double score, String reason) {
    		this.ip = ip;
    		this.score = score;
    		this.reason = reason;
    	}
    }
    

    private Set<String> cachedIPs = new HashSet<>();
    private long lastIPFetchTime = 0;


    @Override
    public boolean shouldMove(AbstractAgent agent) {
        return true;
    }

    @Override
    public String getDestination(AbstractAgent agent) {
        synchronized (PREDICTION_LOCK) {

            String selfIP = IPAddress.myIPAddress;
            ScoreResult bestResult = new ScoreResult(selfIP, Double.NEGATIVE_INFINITY, "negative");
            ScoreResult notBadResult = new ScoreResult(selfIP, Double.NEGATIVE_INFINITY, "negative");
            ScoreResult analyzingResult = new ScoreResult(selfIP, Double.NEGATIVE_INFINITY, "negative");
            
            
            DynamicPCInfo myDyn = InformationCenter.getMyDPI();
            StaticPCInfo mySta = InformationCenter.getMySPI();

            AgentClassInfo info = DHTutil.getAgentInfo(agent.getAgentName());
            if(info == null) {
            	info = new AgentClassInfo(agent.getAgentName());
            }
            
            
            if (myDyn == null|| mySta == null) {
                return selfIP;
            }

            boolean agentNeedsGPU = info.getGpuChange() > 0;
        	boolean clusterHasGpuAgents = checkIfGpuAgentsExist();
        	boolean isThisGpuPc = (mySta != null && mySta.GPUs != null && !mySta.GPUs.isEmpty());

            ScoreResult myScoreResult = calculateMatchScore(agent, myDyn, mySta, IPAddress.myIPAddress);
            if(!agentNeedsGPU && clusterHasGpuAgents && isThisGpuPc) {
            	myScoreResult.score -= 2.0;
            }
            boolean needAnalyze = !info.isAccurate() || info.isExpired();
            for (String ip : getAliveIPs()) {
            	
                if (ip.equals(selfIP)) continue;
                
                if(!DHTutil.canAccept(ip))
                	continue;

                try {
                    DynamicPCInfo dyn = InformationCenter.getOtherDPI(ip);
                    StaticPCInfo sta = InformationCenter.getOtherSPI(ip);
                	boolean isGpuPc = (sta != null && sta.GPUs != null && !sta.GPUs.isEmpty());

                	
                    if (hasMeetDemand(agent, dyn, sta)) {
                    	ScoreResult result = calculateMatchScore(agent, dyn, sta, ip);
                    	
                    	
                    	// スコアにGPUポリシー調整
                    	if (agentNeedsGPU && !isGpuPc) {
                    	    continue; // GPUが必要なのに搭載されていない → スキップ
                    	} else if (!agentNeedsGPU && clusterHasGpuAgents && isGpuPc) {
                    	    result.score -= 2.0; // 避けられるなら避ける程度の減点
                    	}
                    	if(needAnalyze && isGpuPc && result.score > analyzingResult.score) {
                    		analyzingResult = result;
                    	}
                        if (result.score > bestResult.score) {
                            bestResult = result;
                        }
                    }else {
                    	ScoreResult result = calculateMatchScore(agent, dyn, sta, ip);
                    	if (!agentNeedsGPU && clusterHasGpuAgents && isGpuPc) {
                    	    result.score -= 1.0; // 避けられるなら避ける程度の減点
                    	}
                        if (result.score > notBadResult.score) {
                            notBadResult = result;
                        }
                        
                    }

                    

                } catch (Exception e) {
                    System.out.println("[SCORE-SKIP] " + ip + " " + e.getClass().getSimpleName());
                }
            }
            if(analyzingResult.score > myScoreResult.score + Scheduler.scoreThreshold) {
            	setTemporaryPrediction(agent, analyzingResult.ip);
                agent.RegistarHistory(analyzingResult.ip, myScoreResult.reason + analyzingResult.reason);
                if(InformationCenter.getOtherDPI(analyzingResult.ip).AgentsNum == 0) {
                	DHTutil.setCondition(analyzingResult.ip, false);
                }
            }else if (bestResult.score > myScoreResult.score + Scheduler.scoreThreshold) {
            	setTemporaryPrediction(agent, bestResult.ip);
                agent.RegistarHistory(bestResult.ip, myScoreResult.reason + bestResult.reason);
                if(InformationCenter.getOtherDPI(bestResult.ip).AgentsNum == 0) {
                	DHTutil.setCondition(bestResult.ip, false);
                
                }
                return bestResult.ip;
            } else if(notBadResult.score > myScoreResult.score + Scheduler.scoreThreshold){
            	setTemporaryPrediction(agent, notBadResult.ip);
            	agent.RegistarHistory(notBadResult.ip, myScoreResult.reason + notBadResult.reason);
            	return notBadResult.ip;
            }
            
            if(isOutOfMemory(myDyn)) {
            	if(bestResult != null) {

                    agent.RegistarHistory(bestResult.ip, "[OUT OF MEMORY]"+ myScoreResult.reason + bestResult.reason);
            		return bestResult.ip;
            	}
            	agent.RegistarHistory(notBadResult.ip, "[OUT OF MEMORY]"+ myScoreResult.reason + notBadResult.reason);
            	return notBadResult.ip;
            }

            return selfIP;
        }
    }

    /* ================= ユーティリティ ================= */

    
 // GPU要求エージェントがクラスタ内に存在するか？
    private boolean checkIfGpuAgentsExist() {
        for (String ip : getAliveIPs()) {
            DynamicPCInfo dpi = InformationCenter.getOtherDPI(ip);
            if (dpi == null || dpi.Agents == null) continue;

            for (var agent : dpi.Agents.values()) {
                AgentClassInfo info = DHTutil.getAgentInfo(agent.Name);
                if (info != null && info.getGpuChange() > 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isOutOfMemory(DynamicPCInfo dpi) {
    	return dpi.Memory.JvmHeapUsed / dpi.Memory.JvmHeapMax > MEMORY_LIMIT;
    }
    

    private Set<String> getAliveIPs() {
        long now = System.currentTimeMillis();
        if (now - lastIPFetchTime < IP_CACHE_TTL_MS && !cachedIPs.isEmpty()) {
            return cachedIPs;
        }
        try {
            cachedIPs = InformationCenter.getOthersIPs();
            lastIPFetchTime = now;
        } catch (Exception e) {
            System.out.println("[IP-CACHE] fallback");
        }
        return cachedIPs;
    }


    /* ================= 予測 ================= */

    private void setTemporaryPrediction(AbstractAgent agent, String dst) {
        try {
            StaticPCInfo dstSpi = InformationCenter.getOtherSPI(dst);
            StaticPCInfo mySpi  = InformationCenter.getMySPI();

            DynamicPCInfo dstDpi = InformationCenter.getOtherDPI(dst);
            DynamicPCInfo myDpi  = InformationCenter.getMyDPI();

            AgentClassInfo info = DHTutil.getAgentInfo(agent.getClass().getName());

            if (dstSpi == null || mySpi == null || dstDpi == null || myDpi == null || info == null) return;

            long now = System.currentTimeMillis();



            // =========================
            // CPU: Detector に効くのは LoadPercentByMXBean と ProcessCpuLoad
            // =========================
            if (info.getCpuChange() > 0
                    && dstSpi.CPU != null && mySpi.CPU != null
                    && dstDpi.CPU != null && myDpi.CPU != null
                    && dstSpi.CPU.BenchMarkScore > 0 && mySpi.CPU.BenchMarkScore > 0) {

                double addDst = (double) info.getCpuChange() / dstSpi.CPU.BenchMarkScore;
                double subMy  = (double) info.getCpuChange() / mySpi.CPU.BenchMarkScore;

                // cpuPerf 用
                dstDpi.CPU.LoadPercentByMXBean += addDst;
                myDpi.CPU.LoadPercentByMXBean  -= subMy;


                // clamp
                dstDpi.CPU.LoadPercentByMXBean = clamp01(dstDpi.CPU.LoadPercentByMXBean);
                myDpi.CPU.LoadPercentByMXBean  = clamp01(myDpi.CPU.LoadPercentByMXBean);
            }

            // =========================
            // Memory: Detector は JvmHeapUsed / HostAvailableBytes / gcCountByJFR を見る
            // =========================
            if(dstDpi.Memory != null && myDpi.Memory != null) {
            if (info.getHeapChange() > 0) {
                dstDpi.Memory.JvmHeapUsed += info.getHeapChange();
                myDpi.Memory.JvmHeapUsed  -= info.getHeapChange();
                if (dstDpi.Memory.JvmHeapUsed < 0) dstDpi.Memory.JvmHeapUsed = 0;
                if (myDpi.Memory.JvmHeapUsed  < 0) myDpi.Memory.JvmHeapUsed  = 0;
            }


            if (info.getGCCountChange() > 0
            		&& dstDpi.GCStats != null && myDpi.GCStats != null) {
                dstDpi.GCStats.gcCountByJFR += info.getGCCountChange();
                myDpi.GCStats.gcCountByJFR  -= info.getGCCountChange();
                if (myDpi.GCStats.gcCountByJFR < 0) myDpi.GCStats.gcCountByJFR = 0;
            }
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
                    if(dstGpu != null && myGpu != null) {
                    int dstBench = getGpuBench(dstSpi, dstGpuKey);
                    int myBench  = getGpuBench(mySpi,  myGpuKey);

                    if (dstBench > 0 && myBench > 0) {
                        int addLoad = (int) Math.round((double) info.getGpuChange() / dstBench);
                        int subLoad = (int) Math.round((double) info.getGpuChange() / myBench);

                        dstGpu.LoadPercent += addLoad;
                        myGpu.LoadPercent  -= subLoad;
                    } else {
                        // bench 不明なら安全側：固定の小さめ変化（暴れ防止）
                        dstGpu.LoadPercent += 100;
                        myGpu.LoadPercent  -= 100;
                    }

                    dstGpu.LoadPercent = clampInt(dstGpu.LoadPercent, 0, 100);
                    myGpu.LoadPercent  = clampInt(myGpu.LoadPercent, 0, 100);
                    }
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
        	return null;
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

    	AgentClassInfo info = DHTutil.getAgentInfo(agent.getAgentName());
    	if (info == null) return false;

    	// ---- CPU ----
    	if (!hasMeetCPUDemand(info.getCpuChange(),dyn.CPU,sta.CPU)) {
    		return false;
    	}

    	// ---- GPU ----
    	if (!hasMeetGPUDemand(info.getGpuChange(),dyn,sta)) {
    		return false;
    	}

    	// ---- Memory ----
    	if (!hasMeetMemoryDemand(info,dyn,sta)) {
    		return false;
    	}

    	// ---- Network ----
    	if (!hasMeetNetworkDemand(info,dyn,sta)) {
    		return false;
    	}

    	return true;
    }

    private boolean hasMeetCPUDemand(int cpuChange,
            DynamicPCInfo.CPU dynCPU,
            StaticPCInfo.CPU staCPU) {

    	if (staCPU == null || dynCPU == null) return false;
    	if (staCPU.BenchMarkScore <= 0) return false; // ★重要ガード

    	// 現在使用率（0.0〜1.0）→ ベンチマーク換算
    	double usedPerf =
    			dynCPU.LoadPercentByMXBean * staCPU.BenchMarkScore;

    	double afterPerf = usedPerf + cpuChange;

    	return afterPerf <= staCPU.BenchMarkScore;
    }
    
    private boolean hasMeetGPUDemand(int gpuChange,
            DynamicPCInfo dyn,
            StaticPCInfo sta) {

    	if (gpuChange <= 0) return true; // GPU使わないエージェント

    	if (dyn.GPUs == null || sta.GPUs == null) return false;

    	for (Map.Entry<String, StaticPCInfo.GPU> e : sta.GPUs.entrySet()) {
    		String id = e.getKey();
    		StaticPCInfo.GPU sGpu = e.getValue();
    		DynamicPCInfo.GPU dGpu = dyn.GPUs.get(id);

    		if (sGpu == null || dGpu == null) continue;
    		if (sGpu.BenchMarkScore <= 0) continue;

    		int usedPerf =
    				dGpu.LoadPercent * sGpu.BenchMarkScore / 100;

    		int afterPerf = usedPerf + gpuChange;

    		if (afterPerf <= sGpu.BenchMarkScore) {
    			return true; // ★ 1枚でも条件を満たせばOK
    		}
    	}

    	return false;
    }
    
    private boolean hasMeetMemoryDemand(AgentClassInfo info,
            DynamicPCInfo dyn,
            StaticPCInfo sta) {

    	if (dyn.Memory == null) return false;

    	// ---- Heap ----
    	long heapAfter =
    			dyn.Memory.JvmHeapUsed + info.getHeapChange();

    	long heapLimit =
    			dyn.Memory.JvmHeapMax;

    	if (heapLimit > 0 && heapAfter > heapLimit) {
    		return false;
    	}


    	return true;
    }
    
    private boolean hasMeetNetworkDemand(AgentClassInfo info,
            DynamicPCInfo dyn,
            StaticPCInfo sta) {

    	if (info == null) return true;

    	long upNeed   = info.getNetworkUpChange();
    	long downNeed = info.getNetworkDownChange();

    	if (upNeed <= 0 && downNeed <= 0) return true;

    	// dyn/sta が取れないなら「厳しめ」に false でもいいが、
    	// 既存設計に合わせて true(判定スキップ)にするなら下のようにする
    	if (dyn == null || sta == null) return true;
    	if (dyn.NetworkCards == null || sta.NetworkCards == null) return true;

    	long dynUp = 0L;
    	long dynDown = 0L;

    	for (DynamicPCInfo.NetworkCard nic : dyn.NetworkCards.values()) {
    		if (nic == null) continue;
    		dynUp   += (nic.UploadSpeed   != null ? nic.UploadSpeed   : 0L);
    		dynDown += (nic.DownloadSpeed != null ? nic.DownloadSpeed : 0L);
    	}

    	long totalBw = 0L;
    	for (StaticPCInfo.NetworkCard nic : sta.NetworkCards.values()) {
    		if (nic == null) continue;
    		if (nic.Bandwidth > 0) totalBw += nic.Bandwidth;
    	}

    	// Bandwidth が取れてない環境だと totalBw=0 になりがちなのでガード
    	// ここは運用方針で選ぶ：
    	//  (A) 帯域不明なら判定スキップして true
    	//  (B) 帯域不明なら安全側で false
    	if (totalBw <= 0) return true; // ←おすすめ（ログに警告出すと良い）

    	long upAfter   = dynUp   + upNeed;
    	long downAfter = dynDown + downNeed;

    	return upAfter <= totalBw && downAfter <= totalBw;
    }

 
    
    private ScoreResult calculateMatchScore(
            AbstractAgent agent,
            DynamicPCInfo dyn,
            StaticPCInfo sta,
            String ip
            ) {

        AgentClassInfo info = DHTutil.getAgentInfo(agent.getAgentName());
        if (info == null || dyn == null || sta == null)
            return new ScoreResult(ip, Double.NEGATIVE_INFINITY, "info/dyn/sta null for IP=" + ip);

        // ---- 各スコア計算 ----
        double cpuScore = scoreCPU(info, dyn, sta);
        double gpuScore = scoreGPU(info, dyn, sta);
        double memScore = scoreMemory(info, dyn, sta);
        double netScore = scoreNetworkTotal(info, dyn, sta);

        int cores = Math.max(1, sta.CPU.LogicalCore);
        double laNorm = clamp01(dyn.LoadAverage / cores);
        double laScore = clamp01(1.0 - laNorm);

        

        double congestion = calcCongestion(ip, dyn);

        DynamicPCInfo myDPI = InformationCenter.getMyDPI();
        double networkSpeedNorm = 0;
        if (myDPI.NetworkSpeeds != null && myDPI.NetworkSpeeds.get(ip) != null) {
            networkSpeedNorm = clamp01(myDPI.NetworkSpeeds.get(ip).UploadSpeedByOriginal / 900);
        }

        double slowPenalty = 1.0 - networkSpeedNorm;
        double migrateTimeNorm = clamp01(info.getMigrateTime() / 10_000_000L);
        double migratePenalty = clamp01(agent.migrateCount / 10.0);

        double ioScore = scoreIO(dyn); // DPIに追加されたI/Oスループットからスコア化
        double ioPenalty = sta.hasSSD ? 0.0 : 1; // SSD未搭載ならペナルティ

        double score =
                (cpuScore * cpuWeight)
                + (gpuScore * gpuWeight)
                + (memScore * memWeight)
                + (netScore * netWeight)
                + (laScore * laWeight)
                + (ioScore * ioWeight) // IOスコアの重み（適宜調整）
                - (congestion * conWeight)
                - (migrateTimeNorm * migTimeWeight)
                - (slowPenalty * migSpeedWeight)
                - (migratePenalty * migCountWeight)
                - (ioPenalty); // SSDでないなら固定ペナルティ

        // ---- reason ログ（IP付き）----
        StringBuilder reason = new StringBuilder();
        reason.append("[Score] IP=").append(ip).append(" -> ")
              .append("CPU=").append(cpuScore).append("*").append(cpuWeight).append(", ")
              .append("GPU=").append(gpuScore).append("*").append(gpuWeight).append(", ")
              .append("MEM=").append(memScore).append("*").append(memWeight).append(", ")
              .append("NET=").append(netScore).append("*").append(netWeight).append(", ")
              .append(", IO=").append(ioScore).append("*2.0")
              .append(", hasSSD=").append(sta.hasSSD ? "✔" : "✘")
              .append("LA=").append(laScore).append("*").append(laWeight).append(", ")
              .append("congestion=").append(congestion).append("*").append(conWeight).append(", ")
              .append("slowPen=").append(slowPenalty).append("*").append(migSpeedWeight).append(", ")
              .append("migTime=").append(migrateTimeNorm).append("*").append(migTimeWeight).append(", ")
              .append("migCount=").append(migratePenalty).append("*").append(migCountWeight)
              .append(" => total=").append(score);
       

        return new ScoreResult(ip, score, reason.toString());
    }

    /* -------------------------
     * CPU: dynの使用率 + info.cpuChange(要求) が bench を超えないほど高得点
     * ------------------------- */
    private double scoreCPU(AgentClassInfo info, DynamicPCInfo dyn, StaticPCInfo sta) {

    	if (dyn.CPU == null || sta.CPU == null) return 0.0;

    	int bench = Math.max(1, sta.CPU.BenchMarkScore);	
    	double usedPerf = dyn.CPU.LoadPercentByMXBean * bench;

    	double headroom = (bench - usedPerf) / bench; // 0..1
    	return headroom;
    }

    /* -------------------------
     * GPU: 複数GPUなら「一番余裕が残るGPU」を採用
     * ------------------------- */
    private double scoreGPU(AgentClassInfo info, DynamicPCInfo dyn, StaticPCInfo sta) {
    	

    	int need = Math.max(0, info.getGpuChange());
    	if (need <= 0) return 0.0;

    	if (dyn.GPUs == null || sta.GPUs == null || dyn.GPUs.isEmpty() || sta.GPUs.isEmpty()) {
    		return 0;
    	}

    	double best = 0;

    	for (var e : dyn.GPUs.entrySet()) {
    		String key = e.getKey();
    		DynamicPCInfo.GPU d = e.getValue();
    		StaticPCInfo.GPU s = sta.GPUs.get(key);
    		if (d == null || s == null) continue;
    		
    		int bench = Math.max(1, s.BenchMarkScore);

    		// dyn LoadPercent (0..100) -> 使用perf換算
    		double usedPerf = (Math.max(0, d.LoadPercent) / 100.0) * bench;
    		double afterPerf = usedPerf + need;

    		if (afterPerf > bench) continue;

    		double headroom = (bench - usedPerf) / bench;
    		double score = headroom;
    		if (score > best) best = score;
    	}

    	return best;
    }

	/* -------------------------
	* Memory: host available を食いつぶす想定で余裕を点数化
	* ------------------------- */
	private double scoreMemory(AgentClassInfo info, DynamicPCInfo dyn, StaticPCInfo sta) {
	
		if (dyn.Memory == null) return 0.0;
	
		long total = Math.max(1L, dyn.Memory.JvmHeapCommitted);
		long used = Math.max(0L, dyn.Memory.JvmHeapUsed);
		long need  = Math.max(0L, info.getHeapChange());
		
		if (need + used > total) return 0;
		
		double headroom = (double) (total - used) / (double) total;
		return headroom;
	}

	/* -------------------------
	 * Network(合計): 全NICの帯域合計に対する未使用率を返す
	 * 戻り値: 0.0 ～ 1.0（1.0 = 完全に空いている）
	 * ------------------------- */
	private double scoreNetworkTotal(
	        AgentClassInfo info,
	        DynamicPCInfo dyn,
	        StaticPCInfo sta) {

	    if (dyn == null || sta == null ||
	        dyn.NetworkCards == null || sta.NetworkCards == null) {
	        return 0.0;
	    }

	    // --- 使用中帯域（合計） ---
	    long usedUp = 0L;
	    long usedDown = 0L;
	    for (DynamicPCInfo.NetworkCard d : dyn.NetworkCards.values()) {
	        if (d == null) continue;
	        usedUp   += (d.UploadSpeed   != null ? d.UploadSpeed   : 0L);
	        usedDown += (d.DownloadSpeed != null ? d.DownloadSpeed : 0L);
	    }

	    // --- 総帯域（合計） ---
	    long totalBandwidth = 0L;
	    for (StaticPCInfo.NetworkCard s : sta.NetworkCards.values()) {
	        if (s == null) continue;
	        if (s.Bandwidth > 0) {
	            totalBandwidth += s.Bandwidth;
	        }
	    }

	    if (totalBandwidth <= 0) return 0.0;

	    // --- 未使用率 ---
	    double upFreeRate   = 1.0 - clamp01((double) usedUp   / totalBandwidth);
	    double downFreeRate = 1.0 - clamp01((double) usedDown / totalBandwidth);

	    // 上り・下りの平均未使用率
	    return clamp01((upFreeRate + downFreeRate) / 2.0);
	}
	
	private double scoreIO(DynamicPCInfo dyn) {
	    if (dyn == null || dyn.diskIO == null) return 0.0;
	    
	    double readNorm  = clamp01(dyn.diskIO.ReadSpeed  / 100_000_000.0);  // 100MB/s基準
	    double writeNorm = clamp01(dyn.diskIO.WriteSpeed / 100_000_000.0);

	    return (readNorm + writeNorm) / 2.0;  // 平均 I/O スコア
	}

	
	private double calcCongestion(String ip, DynamicPCInfo dynForIp) {

    	int myAgents = 0;
	    if(dynForIp != null && dynForIp.Agents != null) {
	    	for(var e: dynForIp.Agents.entrySet()) {
	        	if(!e.getValue().Name.contains("Messenger")) {
	        		myAgents ++;
	        	}
	        }
	    }

	    int totalAgents = 0;
	    for (String addr : InformationCenter.getAllIPs()) {
	        DynamicPCInfo dpi = InformationCenter.getOtherDPI(addr);
	        if (dpi == null || dpi.Agents == null) continue;
	        for(var e: dpi.Agents.entrySet()) {
	        	if(!e.getValue().Name.contains("Messenger")) {
	        		totalAgents ++;
	        	}
	        }
	    }

	    if (totalAgents == 0) return 0.0;

	    double ratio = (double) myAgents / (double) totalAgents; // 0〜1
	    return clamp01(ratio);
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