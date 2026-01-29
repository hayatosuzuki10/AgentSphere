package scheduler2022.strategy;

import java.util.HashMap;
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

public class ScoreBasedStrategy2 implements SchedulerStrategy {

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
            
            
            DynamicPCInfo myDyn = DHTutil.getPcInfo(selfIP);
            StaticPCInfo mySta = InformationCenter.getMySPI();

            AgentClassInfo info = DHTutil.getAgentInfo(agent.getAgentName());
            if(info == null) {
            	info = new AgentClassInfo(agent.getAgentName());
            }
            
            
            if (myDyn == null|| mySta == null) {
                return selfIP;
            }
            Map<String, DynamicPCInfo> otherDPIs = new HashMap<>();
            for (String ip : getAliveIPs()) {
                DynamicPCInfo dpi = DHTutil.getPcInfo(ip);
                otherDPIs.put(ip, dpi);
               
                
            }

            boolean agentNeedsGPU = info.getGpuChange() > 0;
        	boolean clusterHasGpuAgents = checkIfGpuAgentsExist(myDyn, otherDPIs);
        	boolean isThisGpuPc = (mySta != null && mySta.GPUs != null && !mySta.GPUs.isEmpty());

            ScoreResult myScoreResult = calculateMatchScore(agent, myDyn, mySta, IPAddress.myIPAddress, myDyn, otherDPIs);
            if(!agentNeedsGPU && clusterHasGpuAgents && isThisGpuPc) {
            	myScoreResult.score -= 2.0;
            }
            boolean needAnalyze = !info.isAccurate() || info.isExpired() || !info.hasAnalyze();
            for (String ip : getAliveIPs()) {
            	
                if (ip.equals(selfIP)) continue;
                
                if(!DHTutil.canAccept(ip))
                	continue;

                try {
                    DynamicPCInfo dyn = DHTutil.getPcInfo(ip);
                    StaticPCInfo sta = InformationCenter.getOtherSPI(ip);
                	boolean isGpuPc = (sta != null && sta.GPUs != null && !sta.GPUs.isEmpty());

                	
                    if (hasMeetDemand(agent, dyn, sta)) {
                    	ScoreResult result = calculateMatchScore(agent, dyn, sta, ip, myDyn, otherDPIs);
                    	
                    	
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
                    	ScoreResult result = calculateMatchScore(agent, dyn, sta, ip, myDyn, otherDPIs);
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
            if(analyzingResult.score > myScoreResult.score) {
            	if(DHTutil.getPcInfo(analyzingResult.ip).AgentsNum == 0) {
                	DHTutil.setCondition(analyzingResult.ip, false);
                }
            	setTemporaryPrediction(agent, analyzingResult.ip, myDyn, otherDPIs);
                agent.RegistarHistory(analyzingResult.ip, myScoreResult.reason + analyzingResult.reason);
                
            }else if (bestResult.score > myScoreResult.score + Scheduler.scoreThreshold) {
            	if(DHTutil.getPcInfo(bestResult.ip).AgentsNum == 0) {
                	DHTutil.setCondition(bestResult.ip, false);
                
                }
            	setTemporaryPrediction(agent, bestResult.ip, myDyn, otherDPIs);
                agent.RegistarHistory(bestResult.ip, myScoreResult.reason + bestResult.reason);
                
                return bestResult.ip;
            } else if(notBadResult.score > myScoreResult.score + Scheduler.scoreThreshold){
            	setTemporaryPrediction(agent, notBadResult.ip, myDyn, otherDPIs);
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
    private boolean checkIfGpuAgentsExist(DynamicPCInfo myDPI ,Map<String, DynamicPCInfo> dpis) {
        for (var e : dpis.entrySet()) {
        	DynamicPCInfo dpi = e.getValue();
            if (dpi == null || dpi.Agents == null) continue;

            for (var agent : dpi.Agents.values()) {
                AgentClassInfo info = DHTutil.getAgentInfo(agent.Name);
                if (info != null && info.getGpuChange() > 0) {
                    return true;
                }
            }
            
        }
        
        for(var agent: myDPI.Agents.values()) {
        	AgentClassInfo info = DHTutil.getAgentInfo(agent.Name);
        	if(info != null && info.getGpuChange() > 0) {
        		return true;
        	}
        }
        return false;
    }
    
    private boolean isOutOfMemory(DynamicPCInfo dpi) {
        if (dpi == null || dpi.Memory == null) return false;
        if (dpi.Memory.JvmHeapMax <= 0) return false;

        double ratio = (double) dpi.Memory.JvmHeapUsed / (double) dpi.Memory.JvmHeapMax;
        return ratio > MEMORY_LIMIT;
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

    private void setTemporaryPrediction(AbstractAgent agent, String dst, DynamicPCInfo myDpi, Map<String, DynamicPCInfo> dpis) {
        try {
            StaticPCInfo dstSpi = InformationCenter.getOtherSPI(dst);
            StaticPCInfo mySpi  = InformationCenter.getMySPI();

            DynamicPCInfo dstDpi = dpis.get(dst);
            AgentClassInfo info = DHTutil.getAgentInfo(agent.getAgentName());

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

 
    
    private ScoreResult calculateMatchScore(AbstractAgent agent,
            DynamicPCInfo dyn,
            StaticPCInfo sta,
            String ip,
            DynamicPCInfo mydpi,
            Map<String, DynamicPCInfo> dpis
            ) {

        AgentClassInfo info = DHTutil.getAgentInfo(agent.getAgentName());
        if (info == null || dyn == null || sta == null)
            return new ScoreResult(ip, Double.NEGATIVE_INFINITY, "NULL");

        // ---- 各資源スコア（まず raw 値を計算） ----
        double rawCpu = Math.max(0.0, scoreCPU(info, dyn, sta));
        double rawGpu = Math.max(0.0, scoreGPU(info, dyn, sta));
        double rawMem = Math.max(0.0, scoreMemory(info, dyn, sta));
        double rawNet = Math.max(0.0, scoreNetworkTotal(info, dyn, sta));
        double rawIO  = Math.max(0.0, scoreIO(info, dyn, sta));

        // log1p でスケール圧縮（0〜数十ぐらいに収まる想定）
        double cpu = Math.log1p(rawCpu) * cpuWeight;
        double gpu = Math.log1p(rawGpu) * gpuWeight;
        double mem = Math.log1p(rawMem) * memWeight;
        double net = Math.log1p(rawNet) * netWeight;
        double io  = Math.log1p(rawIO)  * ioWeight;

        // 優先度 × (1 - LA/コア数)
        double priorityScore = 0.0;
        if (sta.CPU != null) {
            int cores = Math.max(1, sta.CPU.LogicalCore);
            double laRatio = dyn.LoadAverage / (double) cores;
            priorityScore = agent.priority * clamp01(1.0 - laRatio) * laWeight;
        }

        // ---- 移動コスト（時間 + 推定データ量） ----
        double migrateCost = 0.0;

        // ネットワーク正規化（0〜1）
        double netNorm = 0.0;
        if (mydpi != null && mydpi.NetworkSpeeds != null && mydpi.NetworkSpeeds.get(ip) != null) {
            netNorm = clamp01(mydpi.NetworkSpeeds.get(ip).UploadSpeedByOriginal / 900.0);
        }

        // ベースは AgentClassInfo の migrateTime
        double migrateTimeBase = Math.max(0.0, info.getMigrateTime());

        // migrateTime が 0 の場合、ヒープサイズからざっくり補正
        if (migrateTimeBase == 0.0) {
            double heapMB = Math.max(0L, info.getHeapChange()) / 1024.0 / 1024.0;
            // 16MB 当たり 1 単位ぐらいのコストを適当に載せる
            migrateTimeBase = heapMB / 16.0;
        }

        migrateCost = migrateTimeBase * (1.0 - netNorm) * migTimeWeight;

        // congestion も少しだけペナルティに載せる（詰まってるノードは避けたい）
        dpis.put(IPAddress.myIPAddress, mydpi);
        double congestion = calcCongestion(ip, dyn, dpis);
        migrateCost += congestion * migCountWeight;

        // ---- 総合スコア ----
        double total = cpu + gpu + mem + net + io + priorityScore - migrateCost;

        String reason = String.format(
            "CPU(raw=%.2f) GPU(raw=%.2f) MEM(raw=%.2f) NET(raw=%.2f) IO(raw=%.2f) -> " +
            "CPU=%.2f GPU=%.2f MEM=%.2f NET=%.2f IO=%.2f PRIO=%.2f MIG=%.2f => %.2f",
            rawCpu, rawGpu, rawMem, rawNet, rawIO,
            cpu, gpu, mem, net, io, priorityScore, migrateCost, total
        );

        return new ScoreResult(ip, total, reason);
    }

    

    /* -------------------------
     * GPU: 複数GPUなら「一番余裕が残るGPU」を採用
     * ------------------------- */
    private double scoreCPU(AgentClassInfo info, DynamicPCInfo dyn, StaticPCInfo sta) {
        if (dyn.CPU == null || sta.CPU == null) return 0.0;

        double req = Math.max(0, info.getCpuChange()); // Agent要求
        double bench = Math.max(1, sta.CPU.BenchMarkScore);

        double usedRatio = clamp01(dyn.CPU.LoadPercentByMXBean); // 0〜1
        double unused = 1.0 - usedRatio;

        return req * unused * bench;
    }

    
    private double scoreGPU(AgentClassInfo info, DynamicPCInfo dyn, StaticPCInfo sta) {

        int req = Math.max(0, info.getGpuChange());
        if (req <= 0) return 0.0;

        if (dyn.GPUs == null || sta.GPUs == null) return 0.0;

        double best = 0.0;

        for (var e : dyn.GPUs.entrySet()) {
            String key = e.getKey();
            DynamicPCInfo.GPU d = e.getValue();
            StaticPCInfo.GPU s = sta.GPUs.get(key);
            if (d == null || s == null) continue;

            double bench = Math.max(1, s.BenchMarkScore);
            double usedRatio = clamp01(d.LoadPercent / 100.0);
            double unused = 1.0 - usedRatio;

            double score = req * unused * bench;
            if (score > best) best = score;
        }
        return best;
    }
    
    /* -------------------------
     * Memory: 追加後にどれだけ余裕が残るか（0〜1）と要求サイズ(log)を掛ける
     * ------------------------- */
    private double scoreMemory(AgentClassInfo info, DynamicPCInfo dyn, StaticPCInfo sta) {

        if (dyn == null || dyn.Memory == null) return 0.0;

        long heapChange = Math.max(0L, info.getHeapChange());
        if (heapChange == 0L) return 0.0;

        long committed = dyn.Memory.JvmHeapCommitted;
        long used      = dyn.Memory.JvmHeapUsed;
        long max       = dyn.Memory.JvmHeapMax > 0 ? dyn.Memory.JvmHeapMax : committed;

        if (max <= 0L) return 0.0;

        long free      = Math.max(0L, committed - used);
        long afterFree = Math.max(0L, free - heapChange);

        // 追加後の「空きの割合」 (0〜1 に clamp)
        double freeRatioAfter = clamp01((double) afterFree / (double) max);

        // 要求ヒープ量を MB に変換して log で圧縮
        double reqMB = heapChange / 1024.0 / 1024.0;
        if (reqMB <= 0.0) return 0.0;

        // freeRatioAfter が大きく、要求もそこそこ大きいほどスコアが高い
        return freeRatioAfter * Math.log1p(reqMB);
    }

    /* -------------------------
     * Network(合計):
     *  未使用率(0〜1) × log(要求帯域MB/s + 1)
     * ------------------------- */
    private double scoreNetworkTotal(AgentClassInfo info, DynamicPCInfo dyn, StaticPCInfo sta) {
        if (dyn == null || sta == null || dyn.NetworkCards == null || sta.NetworkCards == null)
            return 0.0;

        long upNeed   = Math.max(0L, info.getNetworkUpChange());
        long downNeed = Math.max(0L, info.getNetworkDownChange());
        long req      = upNeed + downNeed;
        if (req <= 0L) return 0.0;

        long used  = 0L;
        long total = 0L;

        for (DynamicPCInfo.NetworkCard d : dyn.NetworkCards.values()) {
            if (d == null) continue;
            used += safeLong(d.UploadSpeed) + safeLong(d.DownloadSpeed);
        }

        for (StaticPCInfo.NetworkCard s : sta.NetworkCards.values()) {
            if (s == null) continue;
            if (s.Bandwidth > 0L) {
                total += s.Bandwidth;
            }
        }

        if (total <= 0L) {
            // 帯域が測れない環境 → スコア0にして無視
            return 0.0;
        }

        double usedRatio   = clamp01((double) used / (double) total);
        double unusedRatio = 1.0 - usedRatio;

        // 要求帯域を MB/s 相当に（だいたいのスケールでOK）
        double reqMBps = req / 1_000_000.0;
        if (reqMBps <= 0.0) return 0.0;

        return unusedRatio * Math.log1p(reqMBps);
    }
	
    private double scoreIO(AgentClassInfo info, DynamicPCInfo dyn, StaticPCInfo sta) {

        if (info == null || dyn == null || dyn.diskIO == null || sta == null) return 0.0;

        long readReq  = Math.max(0L, info.getDiskReadChange());
        long writeReq = Math.max(0L, info.getDiskWriteChange());
        long req      = readReq + writeReq;

        if (req <= 0L) return 0.0;

        // ディスク性能の基準値（暫定）
        double maxRead  = sta.hasSSD ? 500_000_000.0 : 100_000_000.0;  // bytes/s 相当
        double maxWrite = sta.hasSSD ? 500_000_000.0 : 100_000_000.0;

        double usedReadRatio  = clamp01(dyn.diskIO.ReadSpeed  / maxRead);
        double usedWriteRatio = clamp01(dyn.diskIO.WriteSpeed / maxWrite);
        double usedRatio      = (usedReadRatio + usedWriteRatio) / 2.0;

        double unusedRate = 1.0 - usedRatio;

        // 要求IOを MB/s にざっくり
        double reqMBps = req / 1_000_000.0;
        if (reqMBps <= 0.0) return 0.0;

        return unusedRate * Math.log1p(reqMBps);
    }
	
	private double calcCongestion(String ip, DynamicPCInfo dynForIp, Map<String, DynamicPCInfo> allDPIs) {

    	int myAgents = 0;
	    if(dynForIp != null && dynForIp.Agents != null) {
	    	for(var e: dynForIp.Agents.entrySet()) {
	        	if(!e.getValue().Name.contains("Messenger")) {
	        		myAgents ++;
	        	}
	        }
	    }

	    int totalAgents = 0;
	    for (var e : allDPIs.entrySet()) {
	        DynamicPCInfo dpi = e.getValue();
	        if (dpi == null || dpi.Agents == null) continue;
	        for(var e2: dpi.Agents.entrySet()) {
	        	if(!e2.getValue().Name.contains("Messenger")) {
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