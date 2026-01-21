package primula.api.core.agent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import scheduler2022.Scheduler;

public class AgentClassInfo implements Serializable {

    private String name;

    private int cpuChange = 0;
    private int gpuChange = 0;
    private long networkUpChange = 0;
    private long networkDownChange = 0;

    private long heapChange = 0;
    private int gcCountChange = 0;

    private long diskReadChange = 0;
    private long diskWriteChange = 0;

    private long migrateTime = 0;

    // ===============================
    // Records
    // ===============================
    private final Map<Long, Integer> cpuChangeRecords = new HashMap<>();
    private final Map<Long, Integer> gpuChangeRecords = new HashMap<>();
    private final Map<Long, Long> networkUpChangeRecords = new HashMap<>();
    private final Map<Long, Long> networkDownChangeRecords = new HashMap<>();
    private final Map<Long, Long> heapChangeRecords = new HashMap<>();
    private final Map<Long, Integer> gcCountChangeRecords = new HashMap<>();
    private final Map<Long, Long> diskReadChangeRecords = new HashMap<>();
    private final Map<Long, Long> diskWriteChangeRecords = new HashMap<>();
    private final Map<Long, Long> migrateTimeRecords = new HashMap<>();

    // ===============================
    // Constructor
    // ===============================

    public AgentClassInfo(String agentName) {
        this.name = agentName;
    }

    public AgentClassInfo(
            String agentName,
            int cpuChange,
            int gpuChange,
            long networkUpChange,
            long networkDownChange,
            long heapChange,
            int gcChange,
            long diskReadChange, 
            long diskWriteChange,
            long migrateTime
    ) {
        this.name = agentName;
        this.cpuChange = cpuChange;
        this.gpuChange = gpuChange;
        this.networkUpChange = networkUpChange;
        this.networkDownChange = networkDownChange;
        this.heapChange = heapChange;
        this.gcCountChange = gcChange;
        this.diskReadChange = diskReadChange;
        this.diskWriteChange = diskWriteChange;
        this.migrateTime = migrateTime;
    }

    @JsonIgnore
    public AgentClassInfo deepCopy() {
        AgentClassInfo copy = new AgentClassInfo(this.name);

        // ==== primitive / immutable fields ====
        copy.cpuChange = this.cpuChange;
        copy.gpuChange = this.gpuChange;
        copy.networkUpChange = this.networkUpChange;
        copy.networkDownChange = this.networkDownChange;
        copy.heapChange = this.heapChange;
        copy.gcCountChange = this.gcCountChange;
        copy.diskReadChange = this.diskReadChange;
        copy.diskWriteChange = this.diskWriteChange;
        copy.migrateTime = this.migrateTime;

        // ==== deep copy maps ====
        synchronized (this.cpuChangeRecords) {
            copy.cpuChangeRecords.putAll(this.cpuChangeRecords);
        }
        synchronized (this.gpuChangeRecords) {
            copy.gpuChangeRecords.putAll(this.gpuChangeRecords);
        }
        synchronized (this.networkUpChangeRecords) {
            copy.networkUpChangeRecords.putAll(this.networkUpChangeRecords);
        }
        synchronized (this.networkDownChangeRecords) {
            copy.networkDownChangeRecords.putAll(this.networkDownChangeRecords);
        }
        synchronized (this.heapChangeRecords) {
            copy.heapChangeRecords.putAll(this.heapChangeRecords);
        }
        synchronized (this.gcCountChangeRecords) {
            copy.gcCountChangeRecords.putAll(this.gcCountChangeRecords);
        }
        synchronized (this.diskReadChangeRecords) {
            copy.diskReadChangeRecords.putAll(this.diskReadChangeRecords);
        }
        synchronized (this.diskWriteChangeRecords) {
            copy.diskWriteChangeRecords.putAll(this.diskWriteChangeRecords);
        }
        synchronized (this.migrateTimeRecords) {
            copy.migrateTimeRecords.putAll(this.migrateTimeRecords);
        }

        return copy;
    }
    
    // ===============================
    // Getters / Setters（全て record*** 経由）
    // ===============================

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public int getCpuChange() { return cpuChange; }

    public void setCpuChange(int val) { recordCPUChange(val, now()); }

    public int getGpuChange() { return gpuChange; }

    public void setGpuChange(int val) { recordGPUChange(val, now()); }

    public long getNetworkUpChange() { return networkUpChange; }

    public void setNetworkUpChange(long val) { recordNetworkUpChange(val, now()); }

    public long getNetworkDownChange() { return networkDownChange; }

    public void setNetworkDownChange(long val) { recordNetworkDownChange(val, now()); }

    public long getHeapChange() { return heapChange; }

    public void setHeapChange(long val) { recordHeapChange(val, now()); }

    public int getGCCountChange() { return gcCountChange; }

    public void setGCCountChange(int val) { recordGCCountChange(val, now()); }

    public long getDiskReadChange() { return diskReadChange; }

    public void setDiskReadChange(long val) { recordDiskReadChange(val, now()); }

    public long getDiskWriteChange() { return diskWriteChange; }

    public void setDiskWriteChange(long val) { recordDiskWriteChange(val, now()); }

    public double getMigrateTime() { return migrateTime; }

    public void setMigrateTime(long val) { recordMigrateTime(val, now()); }

    private static long now() { return System.currentTimeMillis(); }

    // ===============================
    // Record 系（EMA + Records）
    // ===============================

    @JsonIgnore private void recordCPUChange(int v, long t) {
        cpuChange = cpuChangeRecords.isEmpty() ? v : ema(cpuChange, v);
        cpuChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordGPUChange(int v, long t) {
        gpuChange = gpuChangeRecords.isEmpty() ? v : ema(gpuChange, v);
        gpuChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordNetworkUpChange(long v, long t) {
        networkUpChange = networkUpChangeRecords.isEmpty() ? v : ema(networkUpChange, v);
        networkUpChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordNetworkDownChange(long v, long t) {
        networkDownChange = networkDownChangeRecords.isEmpty() ? v : ema(networkDownChange, v);
        networkDownChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordHeapChange(long v, long t) {
        heapChange = heapChangeRecords.isEmpty() ? v : ema(heapChange, v);
        heapChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordGCCountChange(int v, long t) {
        gcCountChange = gcCountChangeRecords.isEmpty() ? v : ema(gcCountChange, v);
        gcCountChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordDiskReadChange(long v, long t) {
        diskReadChange = diskReadChangeRecords.isEmpty() ? v : ema(diskReadChange, v);
        diskReadChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordDiskWriteChange(long v, long t) {
        diskWriteChange = diskWriteChangeRecords.isEmpty() ? v : ema(diskWriteChange, v);
        diskWriteChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordMigrateTime(long v, long t) {
        migrateTime = migrateTimeRecords.isEmpty() ? v : ema(migrateTime, v);
        migrateTimeRecords.put(t, v);
    }

    // ===============================
    // EMA Utils
    // ===============================

    @JsonIgnore private static double ema(double oldVal, double newVal) {
        return Scheduler.getEmaAlpha() * newVal + (1 - Scheduler.getEmaAlpha()) * oldVal;
    }

    @JsonIgnore private static long ema(long oldVal, long newVal) {
        return Math.round(ema((double) oldVal, (double) newVal));
    }

    @JsonIgnore private static int ema(int oldVal, int newVal) {
        return (int) Math.round(ema((double) oldVal, (double) newVal));
    }

    // ===============================
    // toString for debug
    // ===============================
    @Override
    public String toString() {
        return "AgentClassInfo{" +
                "name='" + name + '\'' +
                ", cpuChange=" + cpuChange +
                ", gpuChange=" + gpuChange +
                ", networkUpChange=" + networkUpChange +
                ", networkDownChange=" + networkDownChange +
                ", heapChange=" + heapChange +
                ", gcCountChange=" + gcCountChange +
                ", diskReadChange=" + diskReadChange +
                ", diskWriteChange=" + diskWriteChange +
                ", migrateTime=" + migrateTime +
                '}';
    }
}