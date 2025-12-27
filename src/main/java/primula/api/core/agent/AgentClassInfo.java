package primula.api.core.agent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import scheduler2022.Scheduler;

public class AgentClassInfo implements Serializable {

    // ===============================
    // 基本情報
    // ===============================
    private String name;

    private int cpuChange = 5000;
    private int gpuChange = 5000;
    private long memoryChange = 5000;
    private long networkUpChange = 5000;
    private long networkDownChange = 5000;

    private long heapChange = 0;
    private long realMemoryChange = 0;
    private int gcCountChange = 0;

    private double migrateTime;

    // ===============================
    // 記録用
    // ===============================
    private final Map<Long, Integer> cpuChangeRecords = new HashMap<>();
    private final Map<Long, Integer> gpuChangeRecords = new HashMap<>();
    private final Map<Long, Long> memoryChangeRecords = new HashMap<>();
    private final Map<Long, Long> networkUpChangeRecords = new HashMap<>();
    private final Map<Long, Long> networkDownChangeRecords = new HashMap<>();
    private final Map<Long, Long> heapChangeRecords = new HashMap<>();
    private final Map<Long, Long> realMemoryChangeRecords = new HashMap<>();
    private final Map<Long, Integer> gcCountChangeRecords = new HashMap<>();

    // ===============================
    // Setter / Getter
    // ===============================

    
    public AgentClassInfo(String agentName) {
    	this.name = agentName;
    }
    

	public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCpuChange() {
        return cpuChange;
    }

    public void setCpuChange(int cpuChange) {
        recordCPUChange(cpuChange, System.currentTimeMillis());
    }

    public int getGpuChange() {
        return gpuChange;
    }

    public void setGpuChange(int gpuChange) {
        recordGPUChange(gpuChange, System.currentTimeMillis());
    }

    public long getMemoryChange() {
        return memoryChange;
    }

    public void setMemoryChange(long memoryChange) {
        recordMemoryChange(memoryChange, System.currentTimeMillis());
    }

    public long getNetworkUpChange() {
        return networkUpChange;
    }

    public void setNetworkUpChange(long networkUpChange) {
        recordNetworkUpChange(networkUpChange, System.currentTimeMillis());
    }

    public long getNetworkDownChange() {
        return networkDownChange;
    }

    public void setNetworkDownChange(long networkDownChange) {
        recordNetworkDownChange(networkDownChange, System.currentTimeMillis());
    }

    public long getHeapChange() {
        return heapChange;
    }

    public void setHeapChange(long heapChange) {
        recordHeapChange(heapChange, System.currentTimeMillis());
    }

    public long getRealMemoryChange() {
        return realMemoryChange;
    }

    public void setRealMemoryChange(long realMemoryChange) {
        recordRealMemoryChange(realMemoryChange, System.currentTimeMillis());
    }

    public int getGCCountChange() {
        return gcCountChange;
    }

    public void setGCCountChange(int gcCountChange) {
        recordGCCountChange(gcCountChange, System.currentTimeMillis());
    }

    public double getMigrateTime() {
        return migrateTime;
    }

    public void setMigrateTime(double migrateTime) {
        this.migrateTime = migrateTime;
    }

    // ===============================
    // Record 系（EMA付き）
    // ===============================

    @JsonIgnore
    private void recordCPUChange(int value, long time) {
        cpuChange = cpuChangeRecords.isEmpty()
                ? value
                : ema(cpuChange, value, Scheduler.getEmaAlpha());
        cpuChangeRecords.put(time, value);
    }

    @JsonIgnore
    private void recordGPUChange(int value, long time) {
        gpuChange = gpuChangeRecords.isEmpty()
                ? value
                : ema(gpuChange, value, Scheduler.getEmaAlpha());
        gpuChangeRecords.put(time, value);
    }

    @JsonIgnore
    private void recordMemoryChange(long value, long time) {
        memoryChange = memoryChangeRecords.isEmpty()
                ? value
                : ema(memoryChange, value, Scheduler.getEmaAlpha());
        memoryChangeRecords.put(time, value);
    }

    @JsonIgnore
    private void recordNetworkUpChange(long value, long time) {
        networkUpChange = networkUpChangeRecords.isEmpty()
                ? value
                : ema(networkUpChange, value, Scheduler.getEmaAlpha());
        networkUpChangeRecords.put(time, value);
    }

    @JsonIgnore
    private void recordNetworkDownChange(long value, long time) {
        networkDownChange = networkDownChangeRecords.isEmpty()
                ? value
                : ema(networkDownChange, value, Scheduler.getEmaAlpha());
        networkDownChangeRecords.put(time, value);
    }

    @JsonIgnore
    private void recordHeapChange(long value, long time) {
        heapChange = heapChangeRecords.isEmpty()
                ? value
                : ema(heapChange, value, Scheduler.getEmaAlpha());
        heapChangeRecords.put(time, value);
    }

    @JsonIgnore
    private void recordRealMemoryChange(long value, long time) {
        realMemoryChange = realMemoryChangeRecords.isEmpty()
                ? value
                : ema(realMemoryChange, value, Scheduler.getEmaAlpha());
        realMemoryChangeRecords.put(time, value);
    }

    @JsonIgnore
    private void recordGCCountChange(int value, long time) {
        gcCountChange = gcCountChangeRecords.isEmpty()
                ? value
                : ema(gcCountChange, value, Scheduler.getEmaAlpha());
        gcCountChangeRecords.put(time, value);
    }

    // ===============================
    // EMA Utility
    // ===============================

    @JsonIgnore
    private static double ema(double oldVal, double newVal, double alpha) {
        return alpha * newVal + (1 - alpha) * oldVal;
    }

    @JsonIgnore
    private static long ema(long oldVal, long newVal, double alpha) {
        return Math.round(alpha * newVal + (1 - alpha) * oldVal);
    }

    @JsonIgnore
    private static int ema(int oldVal, int newVal, double alpha) {
        return (int) Math.round(alpha * newVal + (1 - alpha) * oldVal);
    }

    // ===============================
    // toString（修論ログ向け）
    // ===============================

    @Override
    public String toString() {
        return "AgentClassInfo{" +
                "name='" + name + '\'' +
                ", cpuChange=" + cpuChange +
                ", gpuChange=" + gpuChange +
                ", memoryChange=" + memoryChange +
                ", networkUpChange=" + networkUpChange +
                ", networkDownChange=" + networkDownChange +
                ", heapChange=" + heapChange +
                ", realMemoryChange=" + realMemoryChange +
                ", gcCountChange=" + gcCountChange +
                ", migrateTime=" + migrateTime +
                '}';
    }
}