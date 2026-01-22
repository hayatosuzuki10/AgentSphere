package scheduler2022;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import primula.api.core.agent.AgentClassInfo;
import primula.api.core.agent.AgentInstanceInfo;
import scheduler2022.util.DHTutil;

/**
 * 動的な PC 情報（DynamicPCInfo）を一定期間貯めておき、
 * ある時点の「前後」を比較して負荷の変化を検出するクラス。
 *
 * ・add(...) でサンプルを蓄積
 * ・analyze(id) で前後の平均値を比較して Result を生成
 * ・Result は AgentInfo に変化を反映したり、ログ出力したりする
 */
public class DynamicPCInfoDetector {

    // 解析に使う時間など、しきい値群（必要になったら setter などで外から変えられるようにしても良い）
    private long analyzeTime = 10000;
    private int cpuPerfThreshold = 2000;
    private double cpuProcThreshold = 4;
    private long heapMemoryThreshold = 1000;
    private int gcCountThreshold = 10;
    private int gpuPerfThreshold = 2000;
    private int gpuMemThreshold = 1000;
    private double netThresholdMbps = 2000000;
    private long diskThreshold = 1_000_000; // bytes/sec (例: 1MB/s)
    

    // 固定スペック情報（ベンチマークなど）
    public final StaticPCInfo spi;

    // DynamicPCInfo のバッファ（analyze/add/poll/size は synchronized で保護）
    private final java.util.Queue<DynamicPCInfo> queue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    // 直近の解析結果を ID ごとに保持
    private final Map<String, Result> results = new HashMap<>();

    public DynamicPCInfoDetector(StaticPCInfo spi) {
        this.spi = Objects.requireNonNull(spi);
    }

    /**
     * 動的情報を蓄積する。呼び出しは synchronized で直列化。
     */
    public void add(DynamicPCInfo dpi) {
        queue.add(dpi);

    }

    /**
     * 先頭の DynamicPCInfo を取り出す（必要なら使用）。
     */
    public DynamicPCInfo poll() {
        return queue.poll();
    }

    /**
     * 現在溜まっている DynamicPCInfo の数。
     */
    public int size() {
        return queue.size();
    }

    /**
     * 現在のバッファをスナップショットして、
     * 「change〜after」の前後で統計値を比較し Result を生成する。
     */
    public void analyze(String id) {
        long analyzeStartMillis = System.currentTimeMillis();

        // change〜after の間に挟まれたサンプルは解析対象から外す想定
        long change = System.currentTimeMillis();
        sleepQuiet(3000);
        long after = System.currentTimeMillis();

        // もう少しデータが溜まるのを待つ
        sleepQuiet(analyzeTime);

        // 現時点でのキューをディープコピー（以降の処理はこのスナップショットに対して行う）
        Queue<DynamicPCInfo> snapshot = copyQueue(queue);
        Queue<DynamicPCInfo> beforeCopy = new LinkedList<>();
        Queue<DynamicPCInfo> afterCopy = new LinkedList<>();

        for (DynamicPCInfo dpi : snapshot) {
            if (dpi.timeStanp <= change) {
                beforeCopy.add(dpi);
            } else if (dpi.timeStanp >= after) {
                afterCopy.add(dpi);
            }
        }

        Result result = new Result(id, beforeCopy, afterCopy, analyzeStartMillis);
        results.put(id, result);

        // ログと AgentInfo 更新はここでまとめて行う
        //result.print();
        result.updateAgentInfo(id);
        //result.print();
    }

    /**
     * InterruptedException を握りつぶしつつ sleep するユーティリティ。
     */
    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * DynamicPCInfo を deepCopy したキューを作る。
     * （元の queue に後から追加される要素の影響を受けないためのスナップショット）
     */
    private static Queue<DynamicPCInfo> copyQueue(Queue<DynamicPCInfo> src) {
        Queue<DynamicPCInfo> dst = new LinkedList<>();
        for (DynamicPCInfo dpi : src) {
            if (dpi != null) dst.add(dpi.deepCopy());
        }
        return dst;
    }

    public Result getResult(String id) {
        return results.get(id);
    }

    public void updateAgentInfo(String id) {
        Result result = results.get(id);
        if (result != null) {
            result.updateAgentInfo(id);
        }
    }

    /**
     * analyze の結果（前後の平均値の差分＋しきい値判定）を保持するクラス。
     * 外側の Detector とは「計算済みの値」でのみやりとりする。
     */
    public class Result {

        private long timeStamp;

        private boolean hasChangeCPU;
        private boolean hasChangeMemory;
        private boolean hasChangeGPU;
        private boolean hasChangeNetworkUp;
        private boolean hasChangeNetworkDown;

        private int cpuPerfChange;
        private double cpuProcChange;
        private int gcCountChange;
        private long heapMemoryChange;
        private int gpuPerfChange;
        private int gpuMemChange;
        private double netUpChangeMbps;
        private double netDownChangeMbps;
        private boolean hasChangeDiskRead;
        private boolean hasChangeDiskWrite;
        private long diskReadChange;
        private long diskWriteChange;

        public Result(String id, Queue<DynamicPCInfo> before, Queue<DynamicPCInfo> after, long timeStamp) {

            this.timeStamp = timeStamp;

            // ---- 集計 ----
            Aggregate bef = aggregate(before);
            Aggregate aft = aggregate(after);

            // before または after にデータが無いなら、変化なし扱い
            if (bef.count == 0 || aft.count == 0) {
                this.hasChangeCPU = false;
                this.hasChangeMemory = false;
                this.hasChangeGPU = false;
                this.hasChangeNetworkUp = false;
                this.hasChangeNetworkDown = false;
                return;
            }

            // ===== CPU =====
            int beforeCPUPerf = bef.cpuPerf / bef.count;
            int afterCPUPerf = aft.cpuPerf / aft.count;
            this.cpuPerfChange = afterCPUPerf - beforeCPUPerf;
            boolean cpuPerf = this.cpuPerfChange >= cpuPerfThreshold;

            double beforeCPUProc = bef.cpuProc / bef.count;
            double afterCPUProc = aft.cpuProc / aft.count;
            this.cpuProcChange = afterCPUProc - beforeCPUProc;
            boolean cpuProc = this.cpuProcChange >= cpuProcThreshold;

            this.hasChangeCPU = cpuPerf && cpuProc;

            // ===== Memory =====
            int beforeGC = bef.gcCount / bef.count;
            int afterGC = aft.gcCount / aft.count;
            this.gcCountChange = afterGC - beforeGC;

            long beforeHeap = bef.heap / bef.count;
            long afterHeap = aft.heap / aft.count;
            this.heapMemoryChange = afterHeap - beforeHeap;

            boolean heap = (heapMemoryChange / 1024 / 1024) >= heapMemoryThreshold;
            boolean gc = this.gcCountChange >= gcCountThreshold;

            this.hasChangeMemory = heap || gc;

            // ===== GPU =====
            int beforeGPUPerf = bef.gpuPerf / bef.count;
            int afterGPUPerf = aft.gpuPerf / aft.count;
            this.gpuPerfChange = afterGPUPerf - beforeGPUPerf;
            boolean gpuPerf = this.gpuPerfChange >= gpuPerfThreshold;

            int beforeGPUMem = bef.gpuMem / bef.count;
            int afterGPUMem = aft.gpuMem / aft.count;
            this.gpuMemChange = afterGPUMem - beforeGPUMem;
            boolean gpuMem = this.gpuMemChange >= gpuMemThreshold;

            this.hasChangeGPU = gpuPerf && gpuMem;

            // ===== Network =====
            if (bef.networkCardCount == 0 || aft.networkCardCount == 0) {
                this.hasChangeNetworkUp = false;
                this.hasChangeNetworkDown = false;
            } else {
                double beforeUp = bef.netUp / (double) bef.count / bef.networkCardCount;
                double afterUp = aft.netUp / (double) aft.count / aft.networkCardCount;
                this.netUpChangeMbps = afterUp - beforeUp;
                this.hasChangeNetworkUp = this.netUpChangeMbps >= netThresholdMbps;

                double beforeDown = bef.netDown / (double) bef.count / bef.networkCardCount;
                double afterDown = aft.netDown / (double) aft.count / aft.networkCardCount;
                this.netDownChangeMbps = afterDown - beforeDown;
                this.hasChangeNetworkDown = this.netDownChangeMbps >= netThresholdMbps;

            }
            
            long beforeRead  = bef.diskRead / bef.count;
            long afterRead   = aft.diskRead / aft.count;
            this.diskReadChange = afterRead - beforeRead;
            this.hasChangeDiskRead = this.diskReadChange >= diskThreshold;

            long beforeWrite = bef.diskWrite / bef.count;
            long afterWrite  = aft.diskWrite / aft.count;
            this.diskWriteChange = afterWrite - beforeWrite;
            this.hasChangeDiskWrite = this.diskWriteChange >= diskThreshold;
        }

        /**
         * 解析結果を AgentInfo に反映する。
         * 「変化あり」の場合だけ値を記録し、それ以外は 0 を記録する方針。
         */
        public void updateAgentInfo(String id) {
            AgentInstanceInfo info = Scheduler.agentInfo.get(id);
            
            if (info == null) {
                // Agent が既に死んでいる / DHT にいないケースの保険
                System.err.println("AgentInstanceInfo is null for id = " + id);
                return;
            }
            
            String agentName = info.getAgentName();
            if (agentName == null) {
                System.err.println("Agent name is null for id = " + id);
                return;
            }

            AgentClassInfo classInfo;
            if (DHTutil.containsAgent(agentName)) {
                classInfo = DHTutil.getAgentInfo(agentName);
            } else {
                classInfo = new AgentClassInfo(agentName);
            }

            // DHTutil内部でnullチェックがないならここでやる
            if (classInfo == null) {
                System.err.println("AgentClassInfo is null for name = " + agentName);
                return;
            }

            DHTutil.setAgentInfo(agentName, classInfo);
            if (info == null) {
                // Agent が既に死んでいる / DHT にいないケースの保険
                return;
            }

            // ---- CPU ----
            if (this.hasChangeCPU) {
                info.recordCPUChange(cpuPerfChange, this.timeStamp);
            } else {
                info.recordCPUChange(0, this.timeStamp);
            }

            // ---- メモリ（総合 / 内訳）----
            if (this.hasChangeMemory) {

                // 内訳も記録
                info.recordHeapChange(heapMemoryChange, this.timeStamp);
                info.recordGCCountChange(gcCountChange, this.timeStamp);
            } else {
                info.recordHeapChange(0L, this.timeStamp);
                info.recordGCCountChange(0, this.timeStamp);
            }

            // ---- GPU ----
            if (this.hasChangeGPU) {
                info.recordGPUChange(gpuPerfChange, timeStamp);
            } else {
                info.recordGPUChange(0, timeStamp);
            }

            // ---- Network ----
            if (this.hasChangeNetworkUp) {
                info.recordNetworkUpChange((long) netUpChangeMbps, timeStamp);
            } else {
                info.recordNetworkUpChange(0L, timeStamp);
            }
            if (this.hasChangeNetworkDown) {
                info.recordNetworkDownChange((long) netDownChangeMbps, timeStamp);
            } else {
                info.recordNetworkDownChange(0L, timeStamp);
            }
            
            if (this.hasChangeDiskRead || this.hasChangeDiskWrite) {
                info.recordDiskIOChange(diskReadChange, diskWriteChange, timeStamp);
            } else {
                info.recordDiskIOChange(0L, 0L, timeStamp);
            }

            Scheduler.agentInfo.put(id, info);
            
        }

        /**
         * 解析結果の人間向けログ出力。
         */
        public void print() {
            System.out.println("===== DynamicPCInfoDetector Result =====");

            // ---- CPU ----
            System.out.println("CPU:");
            System.out.printf("  Load Change: %d %%  (Threshold: %d) -> %s%n",
                    cpuPerfChange, cpuPerfThreshold, hasChangeCPU ? "⚠️ Significant" : "OK");
            System.out.printf("  Process Load Change: %.2f %%  (Threshold: %.2f)%n",
                    cpuProcChange, cpuProcThreshold);

            // ---- メモリ関連（ヒープ / 実メモリ / GC）----
            double heapChangeMB = heapMemoryChange / 1024.0 / 1024.0;
            boolean heapFlag = heapChangeMB >= heapMemoryThreshold;

            boolean gcFlag = gcCountChange >= gcCountThreshold;

            System.out.println("\nMemory:");
            System.out.printf("  Heap Change: %.2f MB (Threshold: %d MB) -> %s%n",
                    heapChangeMB,
                    heapMemoryThreshold,
                    heapFlag ? "⚠️ Significant" : "OK"
            );


            System.out.println("\nGC:");
            System.out.printf("  GC Count Change: %d (Threshold: %d) -> %s%n",
                    gcCountChange, gcCountThreshold,
                    gcFlag ? "⚠️ Significant" : "OK"
            );

            System.out.printf("  Overall Memory Status: %s%n",
                    hasChangeMemory ? "⚠️ High Load (Heap/GC/Real)" : "Stable");

            // ---- GPU ----
            System.out.println("\nGPU:");
            System.out.printf("  Load Change: %d %%  (Threshold: %d)%n",
                    gpuPerfChange, gpuPerfThreshold);
            System.out.printf("  Memory Change: %d MB  (Threshold: %d) -> %s%n",
                    gpuMemChange, gpuMemThreshold,
                    hasChangeGPU ? "⚠️ Significant" : "OK");

            // ---- Network ----
            System.out.println("\nNetwork:");
            System.out.printf("  Upload Change: %.2f Mbps%n", netUpChangeMbps);
            System.out.printf("  Download Change: %.2f Mbps%n", netDownChangeMbps);
            System.out.printf("  Network Status: %s%n",
                    hasChangeNetworkUp ? "⚠️ High Change" : "Stable");
            
            System.out.println("\nDiskIO:");
            System.out.printf("  Read  Change: %.2f MB/s (Threshold: %.2f MB/s) -> %s%n",
                diskReadChange / 1024.0 / 1024.0,
                diskThreshold / 1024.0 / 1024.0,
                hasChangeDiskRead ? "⚠️ Significant" : "OK"
            );
            System.out.printf("  Write Change: %.2f MB/s (Threshold: %.2f MB/s) -> %s%n",
                diskWriteChange / 1024.0 / 1024.0,
                diskThreshold / 1024.0 / 1024.0,
                hasChangeDiskWrite ? "⚠️ Significant" : "OK"
            );

            System.out.println("========================================\n");
        }
    }

    /**
     * before / after それぞれの DynamicPCInfo 群の「合計値」を持つための内部クラス。
     * 平均は Result 側で count で割って算出する。
     */
    private static class Aggregate {
        int count;
        int cpuPerf;
        double cpuProc;
        long heap;
        int gcCount;
        int gpuPerf;
        int gpuMem;
        int networkCardCount;
        double netUp;
        double netDown;

        long diskRead;   // ★追加
        long diskWrite;  // ★追加
    }

    /**
     * DynamicPCInfo のキューを走査して Aggregate に集計する。
     */
    private Aggregate aggregate(Queue<DynamicPCInfo> src) {
        Aggregate agg = new Aggregate();

        for (DynamicPCInfo dpi : src) {
            if (dpi == null) continue;

            agg.count++;

            // --- CPU ---
            agg.cpuPerf += dpi.CPU.LoadPercentByMXBean * spi.CPU.BenchMarkScore;
            agg.cpuProc += dpi.CPU.ProcessCpuLoad * 100.0;
            // --- Memory ---
            agg.heap += dpi.Memory.JvmHeapUsed;
            agg.gcCount += dpi.GCStats.gcCountByJFR;

            // --- GPU ---
            for (Map.Entry<String, DynamicPCInfo.GPU> e : dpi.GPUs.entrySet()) {
                DynamicPCInfo.GPU gpu = e.getValue();
                var staticGpu = spi.GPUs.get(e.getKey());
                if (staticGpu == null) {
                    // static 情報が取れない GPU はスキップ（想定外の ID など）
                    continue;
                }
                agg.gpuPerf += gpu.LoadPercent * staticGpu.BenchMarkScore;
                agg.gpuMem += gpu.UsedMemory;
            }

            // --- Network ---
            for (Map.Entry<String, DynamicPCInfo.NetworkCard> e : dpi.NetworkCards.entrySet()) {
                DynamicPCInfo.NetworkCard nic = e.getValue();
                agg.networkCardCount++;
                agg.netUp += nic.UploadSpeed;
                agg.netDown += nic.DownloadSpeed;
            }
            agg.diskRead  += dpi.diskIO.ReadSpeed;
            agg.diskWrite += dpi.diskIO.WriteSpeed;
        }

        return agg;
    }
}