package primula.api.core.agent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    
    private int measureCount  = 0;
    private long measureTime = System.currentTimeMillis();

    private final double accurateLearning = 0.8;
    private final double unAccurateLearning = 0.2;
    private final int unAccurateCount = 5;
    private final long measureTimeExpired = 1000 * 60 * 3;
    
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

    // ===============================
    // Getters / Setters（全て record*** 経由）
    // ===============================

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
    
    public boolean isAccurate() {
    	return measureCount < unAccurateCount;
    }
    
    public boolean isExpired() {
    	return System.currentTimeMillis() - measureTime > measureTimeExpired;
    }

    public int getCpuChange() { return cpuChange; }

    public void setCpuChange(int val, boolean isAccurate) { 
    	recordCPUChange(val, now(), isAccurate);
    	if(isAccurate) {
    		measureCount = 0;
    		measureTime = System.currentTimeMillis();
    	} else {
    		measureCount ++;
    	}
    	}

    public int getGpuChange() { return gpuChange; }

    public void setGpuChange(int val, boolean isAccurate) { recordGPUChange(val, now(), isAccurate); }

    public long getNetworkUpChange() { return networkUpChange; }

    public void setNetworkUpChange(long val, boolean isAccurate) { recordNetworkUpChange(val, now(), isAccurate); }

    public long getNetworkDownChange() { return networkDownChange; }

    public void setNetworkDownChange(long val, boolean isAccurate) { recordNetworkDownChange(val, now(), isAccurate); }

    public long getHeapChange() { return heapChange; }

    public void setHeapChange(long val, boolean isAccurate) { recordHeapChange(val, now(), isAccurate); }

    public int getGCCountChange() { return gcCountChange; }

    public void setGCCountChange(int val, boolean isAccurate) { recordGCCountChange(val, now(), isAccurate); }

    public long getDiskReadChange() { return diskReadChange; }

    public void setDiskReadChange(long val, boolean isAccurate) { recordDiskReadChange(val, now(), isAccurate); }

    public long getDiskWriteChange() { return diskWriteChange; }

    public void setDiskWriteChange(long val, boolean isAccurate) { recordDiskWriteChange(val, now(), isAccurate); }

    public double getMigrateTime() { return migrateTime; }

    public void setMigrateTime(long val, boolean isAccurate) { recordMigrateTime(val, now(), isAccurate); }

    private static long now() { return System.currentTimeMillis(); }

    // ===============================
    // Record 系（EMA + Records）
    // ===============================

    @JsonIgnore private void recordCPUChange(int v, long t, boolean isAccurate) {
    	if(isAccurate) {
            cpuChange = cpuChangeRecords.isEmpty() ? v : ema(cpuChange, v, accurateLearning);
    	}else {
            cpuChange = cpuChangeRecords.isEmpty() ? v : ema(cpuChange, v, unAccurateLearning);
    	}
        cpuChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordGPUChange(int v, long t, boolean isAccurate) {
    	if(isAccurate) {
    		gpuChange = gpuChangeRecords.isEmpty() ? v : ema(gpuChange, v, accurateLearning);
    	}else {
    		gpuChange = gpuChangeRecords.isEmpty() ? v : ema(gpuChange, v, unAccurateLearning);
    	}
        gpuChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordNetworkUpChange(long v, long t, boolean isAccurate) {
    	if(isAccurate) {
    		networkUpChange = networkUpChangeRecords.isEmpty() ? v : ema(networkUpChange, v, accurateLearning);
    	}else {
    		networkUpChange = networkUpChangeRecords.isEmpty() ? v : ema(networkUpChange, v, unAccurateLearning);
    	}
        networkUpChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordNetworkDownChange(long v, long t, boolean isAccurate) {
    	if(isAccurate) {
    		networkDownChange = networkDownChangeRecords.isEmpty() ? v : ema(networkDownChange, v, accurateLearning);
    	}else {
    		networkDownChange = networkDownChangeRecords.isEmpty() ? v : ema(networkDownChange, v, unAccurateLearning);
    	}
        networkDownChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordHeapChange(long v, long t, boolean isAccurate) {
    	if(isAccurate) {
    		heapChange = heapChangeRecords.isEmpty() ? v : ema(heapChange, v, accurateLearning);
    	}else {
    		heapChange = heapChangeRecords.isEmpty() ? v : ema(heapChange, v, unAccurateLearning);
    	}
        
        heapChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordGCCountChange(int v, long t, boolean isAccurate) {
    	if(isAccurate) {
    		gcCountChange = gcCountChangeRecords.isEmpty() ? v : ema(gcCountChange, v, accurateLearning);
    	}else {
    		gcCountChange = gcCountChangeRecords.isEmpty() ? v : ema(gcCountChange, v, unAccurateLearning);
    	}
        
        gcCountChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordDiskReadChange(long v, long t, boolean isAccurate) {
    	if(isAccurate) {
    		diskReadChange = diskReadChangeRecords.isEmpty() ? v : ema(diskReadChange, v, accurateLearning);
    	}else {
    		diskReadChange = diskReadChangeRecords.isEmpty() ? v : ema(diskReadChange, v, unAccurateLearning);
    	}
        
        diskReadChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordDiskWriteChange(long v, long t, boolean isAccurate) {
    	if(isAccurate) {
    		diskWriteChange = diskWriteChangeRecords.isEmpty() ? v : ema(diskWriteChange, v, accurateLearning);
    	}else {
    		diskWriteChange = diskWriteChangeRecords.isEmpty() ? v : ema(diskWriteChange, v, unAccurateLearning);
    	}
        
        diskWriteChangeRecords.put(t, v);
    }

    @JsonIgnore private void recordMigrateTime(long v, long t, boolean isAccurate) {
    	if(isAccurate) {
    		migrateTime = migrateTimeRecords.isEmpty() ? v : ema(migrateTime, v, accurateLearning);
    	}else {
    		migrateTime = migrateTimeRecords.isEmpty() ? v : ema(migrateTime, v, unAccurateLearning);
    	}
        
        migrateTimeRecords.put(t, v);
    }

    // ===============================
    // EMA Utils
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