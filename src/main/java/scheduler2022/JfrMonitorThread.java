package scheduler2022;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

public class JfrMonitorThread extends Thread {

    private final Consumer<Snapshot> onTick;
    private final Duration samplePeriod;

    private volatile Recording recording;
    private volatile boolean running = true;

    // ===== 集計用 =====
    private final AtomicLong tickNanos = new AtomicLong();
    private final AtomicLong gcCount = new AtomicLong();
    private final AtomicLong gcPauseMillis = new AtomicLong();
    private final AtomicLong socketReadBytes = new AtomicLong();
    private final AtomicLong socketWriteBytes = new AtomicLong();

    private final Map<Long, AtomicLong> perThreadSamples = new ConcurrentHashMap<>();

    private volatile double jvmUser = 0.0;
    private volatile double jvmSystem = 0.0;
    private volatile double machineTotal = 0.0;

    private final ConcurrentHashMap<Long, AtomicLong> allocNewTLAB = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> allocOutsideTLAB = new ConcurrentHashMap<>();

    // 読み出しの進捗（最後に処理したイベント時刻）
    private volatile Instant lastSeen = Instant.EPOCH;

    public JfrMonitorThread(Consumer<Snapshot> onTick) {
        this(onTick, Duration.ofMillis(20));
    }
    public JfrMonitorThread(Consumer<Snapshot> onTick, Duration samplePeriod) {
        super("JfrMonitorThread");
        this.onTick = onTick;
        this.samplePeriod = samplePeriod;
        setDaemon(true);
    }

    public void shutdown() {
        running = false;
        Recording r = recording;
        if (r != null) try { r.close(); } catch (Throwable ignore) {}
        interrupt();
    }

    @Override
    public void run() {
        try {
            // ★ JDK11: Recording を使う
            recording = new Recording(Configuration.getConfiguration("profile"));
            // 必要イベントを enable（withPeriod は JDK11でも可）
            recording.enable("jdk.CPULoad").withPeriod(Duration.ofMillis(200));
            recording.enable("jdk.GarbageCollection");
            recording.enable("jdk.ExecutionSample").withPeriod(samplePeriod);
            recording.enable("jdk.JavaMonitorEnter");
            recording.enable("jdk.JavaMonitorWait");
            recording.enable("jdk.SocketRead");
            recording.enable("jdk.SocketWrite");
            recording.enable("jdk.ObjectAllocationInNewTLAB");
            recording.enable("jdk.ObjectAllocationOutsideTLAB");

            recording.setToDisk(true);
            recording.start();

            // 1秒ごとに dump → 読み取り → 集計
            while (running) {
                Thread.sleep(1000);
                pollOnce();
                // 1秒スナップショット完成・通知
                Snapshot s = buildAndResetSnapshot();
                if (onTick != null) onTick.accept(s);
            }

        } catch (InterruptedException ignored) {
        } catch (Throwable t) {
            System.err.println("[JFR] monitor error: " + t);
        } finally {
            Recording r = recording;
            recording = null;
            if (r != null) try { r.close(); } catch (Throwable ignore) {}
        }
    }

    /** 録画ファイルを一時出力して、lastSeen 以降のイベントだけ処理 */
    private void pollOnce() {
        Recording r = recording;
        if (r == null) return;

        Path tmp = null;
        try {
            tmp = Files.createTempFile("jfr-snap", ".jfr");
            r.dump(tmp); // 現在のバッファをファイルへ
            Instant newLast = lastSeen;
            for (RecordedEvent e : RecordingFile.readAllEvents(tmp)) {
                // JDK11 では startTime/endTime を見てフィルタ
                Instant t = e.getEndTime(); // endTime のほうが確実
                if (t == null || !t.isAfter(lastSeen)) continue;
                newLast = (newLast.isBefore(t) ? t : newLast);

                String type = e.getEventType().getName();
                switch (type) {
                    case "jdk.CPULoad":
                        jvmUser = safeGetDouble(e, "jvmUser", 0.0);
                        jvmSystem = safeGetDouble(e, "jvmSystem", 0.0);
                        machineTotal = safeGetDouble(e, "machineTotal", 0.0);
                        break;

                    case "jdk.GarbageCollection":
                        gcCount.incrementAndGet();
                        long pauseNs = safeGetDurationNanos(e, "sumOfPaused", 0L);
                        gcPauseMillis.addAndGet(pauseNs / 1_000_000L);
                        break;

                    case "jdk.ExecutionSample": {
                        RecordedThread tinfo = e.getThread("sampledThread");
                        if (tinfo != null) {
                            long tid = tinfo.getJavaThreadId();
                            perThreadSamples.computeIfAbsent(tid, k -> new AtomicLong()).incrementAndGet();
                        }
                        tickNanos.addAndGet(samplePeriod.toNanos());
                        break;
                    }

                    case "jdk.SocketRead":
                        socketReadBytes.addAndGet(safeGetLong(e, "bytesRead", 0L));
                        break;

                    case "jdk.SocketWrite":
                        socketWriteBytes.addAndGet(safeGetLong(e, "bytesWritten", 0L));
                        break;

                    case "jdk.ObjectAllocationInNewTLAB": {
                        RecordedThread tinfo = e.getThread();
                        if (tinfo != null) {
                            long tid = tinfo.getJavaThreadId();
                            long sz = safeGetLong(e, "allocationSize", 0L);
                            allocNewTLAB.computeIfAbsent(tid, k -> new AtomicLong()).addAndGet(sz);
                        }
                        break;
                    }
                    case "jdk.ObjectAllocationOutsideTLAB": {
                        RecordedThread tinfo = e.getThread();
                        if (tinfo != null) {
                            long tid = tinfo.getJavaThreadId();
                            long sz = safeGetLong(e, "allocationSize", 0L);
                            allocOutsideTLAB.computeIfAbsent(tid, k -> new AtomicLong()).addAndGet(sz);
                        }
                        break;
                    }
                }
            }
            lastSeen = newLast;
        } catch (IOException ioe) {
            System.err.println("[JFR] dump/read error: " + ioe);
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
        }
    }

    private Snapshot buildAndResetSnapshot() {
        var hotThreads = new ConcurrentHashMap<Long, Long>();
        perThreadSamples.forEach((tid, cnt) -> hotThreads.put(tid, cnt.getAndSet(0)));

        Map<Long, Long> newTLABMap = new HashMap<>();
        long newTLABTotal = 0L;
        for (Map.Entry<Long, AtomicLong> e : allocNewTLAB.entrySet()) {
            long bytes = e.getValue().getAndSet(0L);
            if (bytes > 0) { newTLABMap.put(e.getKey(), bytes); newTLABTotal += bytes; }
        }

        Map<Long, Long> outTLABMap = new HashMap<>();
        long outTLABTotal = 0L;
        for (Map.Entry<Long, AtomicLong> e : allocOutsideTLAB.entrySet()) {
            long bytes = e.getValue().getAndSet(0L);
            if (bytes > 0) { outTLABMap.put(e.getKey(), bytes); outTLABTotal += bytes; }
        }

        Snapshot s = new Snapshot();
        s.jvmUser = jvmUser;
        s.jvmSystem = jvmSystem;
        s.machineTotal = machineTotal;

        s.gcCount = gcCount.getAndSet(0);
        s.gcPauseMillis = gcPauseMillis.getAndSet(0);

        s.socketReadBytes = socketReadBytes.getAndSet(0);
        s.socketWriteBytes = socketWriteBytes.getAndSet(0);

        s.executedSampleNanos = tickNanos.getAndSet(0);
        s.perThreadSamples = hotThreads;

        s.allocNewTLABBytes = newTLABMap;
        s.allocOutsideTLABBytes = outTLABMap;
        s.allocNewTLABTotal = newTLABTotal;
        s.allocOutsideTLABTotal = outTLABTotal;

        return s;
    }

    private static double safeGetDouble(RecordedEvent e, String field, double def) {
        try { return e.getDouble(field); } catch (Throwable t) { return def; }
    }
    private static long safeGetLong(RecordedEvent e, String field, long def) {
        try { return e.getLong(field); } catch (Throwable t) { return def; }
    }
    private static long safeGetDurationNanos(RecordedEvent e, String field, long def) {
        try { return e.getDuration(field).toNanos(); } catch (Throwable t) { return def; }
    }

    public static class Snapshot {
        public double jvmUser, jvmSystem, machineTotal;
        public long gcCount, gcPauseMillis;
        public long socketReadBytes, socketWriteBytes;
        public long executedSampleNanos;
        public Map<Long, Long> perThreadSamples;
        public Map<Long, Long> allocNewTLABBytes;
        public Map<Long, Long> allocOutsideTLABBytes;
        public long allocNewTLABTotal, allocOutsideTLABTotal;

        @Override public String toString() {
            return String.format(
                "CPU jvm(usr=%.2f,sys=%.2f) machine=%.2f | GC count=%d pause=%dms | NET r=%dB w=%dB | samples=%d | alloc: newTLAB=%.2f MB, outside=%.2f MB",
                jvmUser, jvmSystem, machineTotal,
                gcCount, gcPauseMillis,
                socketReadBytes, socketWriteBytes,
                executedSampleNanos,
                allocNewTLABTotal / 1e6, allocOutsideTLABTotal / 1e6
            );
        }
    }
}