package scheduler2022;

import java.io.Serializable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 各エージェントスフィアの性能を表すクラス
 * 収集は collect(...) で一括取得
 * @author selab
 */
public class DynamicPCInfo implements Serializable {
	public boolean isForecast = false;
	public long timeStanp = System.currentTimeMillis();

	/** OSロードアベレージ（System Load Average） */
	public double LoadAverage = 0;

	/** ホストCPU（OSHI/MXBean両取り） */
	public CPU CPU;

	/** 互換用（ホスト空きメモリ） */
	public long FreeMemory;

	/** メモリ（ホスト＆JVM） */
	public Memory Memory;

	/** GC統計 */
	public GC GCStats;

	/** ロード済みクラス数 */
	public int LoadedClass;

	/** NICごとの累積と瞬間速度 */
	public long allNetworkUp = 0L;
	public long allNetworkDown = 0L;
	public Map<String, NetworkCard> NetworkCards;

	/** Node間の疎通速度（任意） */
	public Map<String,NetworkSpeed> NetworkSpeeds;
	
	public long socketReadBytes;
	public long socketWriteBytes;

	/** GPUデバイスの状態 */
	public GPU mainGPU;
	public Map<String,GPU> GPUs;
	
	public DiskIO diskIO;

	/** 稼働中エージェント一覧 */
	public Map<String,Agent> Agents;

	public int AgentsNum;

	public DynamicPCInfo() {}

	public DynamicPCInfo(
			double loadAverage,
			CPU cpu,
			long freeMemory,
			Memory memory,
			GC gc,
			int loadedClass,
			Map<String,GPU> gpus,
			Map<String, NetworkCard> networkCards,
			Map<String,NetworkSpeed> networkSpeeds,
			long socketReadBytes,
			long socketWriteBytes,
			DiskIO diskIO,
			Map<String,Agent> agents,
			int agentsNum
	) {
		this.LoadAverage = loadAverage;
		this.CPU = cpu;
		this.FreeMemory = freeMemory;
		this.Memory = memory;
		this.GCStats = gc;
		this.LoadedClass = loadedClass;
		this.NetworkCards = networkCards;
		this.NetworkSpeeds = networkSpeeds;
		this.socketReadBytes = socketReadBytes;
		this.socketWriteBytes = socketWriteBytes;
		this.GPUs = gpus;
		this.diskIO = diskIO;
		this.Agents = agents;
		this.AgentsNum = agentsNum;
		this.timeStanp = System.currentTimeMillis();
	}

	

	@JsonIgnore
	public DynamicPCInfo deepCopy() {
	    // ---- CPU ----
	    CPU cpuCopy = null;
	    if (this.CPU != null) {
	        cpuCopy = new CPU();
	        cpuCopy.ClockSpeed            = this.CPU.ClockSpeed;
	        cpuCopy.LoadPercentByMXBean   = this.CPU.LoadPercentByMXBean;
	        // cpuCopy.LoadPercentByOSHI  = this.CPU.LoadPercentByOSHI; // 使っていないなら不要
	        cpuCopy.ProcessCpuLoad        = this.CPU.ProcessCpuLoad;
	        cpuCopy.AvailableProcessors   = this.CPU.AvailableProcessors;
	        // JFR スナップショット由来
	        cpuCopy.jvmSystem             = this.CPU.jvmSystem;
	        cpuCopy.jvmUser               = this.CPU.jvmUser;
	        cpuCopy.total                 = this.CPU.total;
	        cpuCopy.prev                  = this.CPU.prev;
	    }

	    // ---- Memory ----
	    Memory memCopy = null;
	    if (this.Memory != null) {
	        memCopy = new Memory();
	        memCopy.HostTotalBytes       = this.Memory.HostTotalBytes;
	        memCopy.HostAvailableBytes   = this.Memory.HostAvailableBytes;
	        memCopy.SwapTotalBytes       = this.Memory.SwapTotalBytes;
	        memCopy.SwapUsedBytes        = this.Memory.SwapUsedBytes;
	        memCopy.JvmHeapUsed          = this.Memory.JvmHeapUsed;
	        memCopy.JvmHeapCommitted     = this.Memory.JvmHeapCommitted;
	        memCopy.JvmHeapMax           = this.Memory.JvmHeapMax;
	        memCopy.JvmNonHeapUsed       = this.Memory.JvmNonHeapUsed;
	        memCopy.JvmNonHeapCommitted  = this.Memory.JvmNonHeapCommitted;
	    }

	    // ---- GPUs ----
	    Map<String, GPU> gpusCopy = new HashMap<>();
	    if (this.GPUs != null) {
	        for (Map.Entry<String, GPU> e : this.GPUs.entrySet()) {
	            GPU s = e.getValue();
	            if (s == null) continue;
	            GPU d = new GPU();
	            d.Name        = s.Name;
	            d.LoadPercent = s.LoadPercent;
	            d.UsedMemory  = s.UsedMemory;
	            d.TotalMemory = s.TotalMemory;
	            d.TemperatureC= s.TemperatureC;
	            gpusCopy.put(e.getKey(), d);
	        }
	    }

	    // ---- NetworkCards ----
	    Map<String, NetworkCard> ncCopy = new HashMap<>();
	    if (this.NetworkCards != null) {
	        for (Map.Entry<String, NetworkCard> e : this.NetworkCards.entrySet()) {
	            NetworkCard s = e.getValue();
	            if (s == null) continue;
	            NetworkCard d = new NetworkCard();
	            d.SentByte      = s.SentByte;
	            d.ReceivedByte  = s.ReceivedByte;
	            d.UploadSpeed   = s.UploadSpeed;
	            d.DownloadSpeed = s.DownloadSpeed;
	            ncCopy.put(e.getKey(), d);
	        }
	    }

	    // ---- NetworkSpeeds ----
	    Map<String, NetworkSpeed> nsCopy = new HashMap<>();
	    if (this.NetworkSpeeds != null) {
	        for (Map.Entry<String, NetworkSpeed> e : this.NetworkSpeeds.entrySet()) {
	            NetworkSpeed s = e.getValue();
	            if (s == null) continue;
	            NetworkSpeed d = new NetworkSpeed();
	            d.Sender                  = s.Sender;
	            d.Receiver                = s.Receiver;
	            d.UploadSpeedByOriginal   = s.UploadSpeedByOriginal;
	            d.DownloadSpeedByOriginal = s.DownloadSpeedByOriginal;
	            d.UploadSpeedByIperf3     = s.UploadSpeedByIperf3;
	            d.DownloadSpeedByIperf3   = s.DownloadSpeedByIperf3;
	            nsCopy.put(e.getKey(), d);
	        }
	    }
	    
	    DiskIO diskIOCopy = null;
	    if (this.diskIO != null) {
	        diskIOCopy = new DiskIO();
	        diskIOCopy.ReadBytes  = this.diskIO.ReadBytes;
	        diskIOCopy.WriteBytes = this.diskIO.WriteBytes;
	        diskIOCopy.ReadSpeed  = this.diskIO.ReadSpeed;
	        diskIOCopy.WriteSpeed = this.diskIO.WriteSpeed;
	    }

	    // ---- Agents ----
	    Map<String, Agent> agentsCopy = new HashMap<>();
	    if (this.Agents != null) {
	        for (Map.Entry<String, Agent> e : this.Agents.entrySet()) {
	            Agent s = e.getValue();
	            if (s == null) continue;
	            Agent d = new Agent();
	            d.ID        = s.ID;
	            d.Name      = s.Name;
	            d.StartTime = s.StartTime;
	            agentsCopy.put(e.getKey(), d);
	        }
	    }

	    // ---- GC ----
	    GC gcCopy = null;
	    if (this.GCStats != null) {
	        gcCopy = new GC();
	        gcCopy.gcCountByJFR  = this.GCStats.gcCountByJFR;   // ★ JFR 追加分
	        gcCopy.gcPauseMillis = this.GCStats.gcPauseMillis;  // ★ JFR 追加分
	        gcCopy.Collectors = new HashMap<>();
	        if (this.GCStats.Collectors != null) {
	            for (Map.Entry<String, GC.GCStat> e : this.GCStats.Collectors.entrySet()) {
	                GC.GCStat s = e.getValue();
	                if (s == null) continue;
	                GC.GCStat d = new GC.GCStat();
	                d.Name            = s.Name;
	                d.AllCount        = s.AllCount;
	                d.CollectionCount = s.CollectionCount;
	                d.CollectionTimeMs= s.CollectionTimeMs;
	                gcCopy.Collectors.put(e.getKey(), d);
	            }
	        }
	    }

	    // ---- 本体生成（★ socketReadBytes / socketWriteBytes を忘れずに）----
	    DynamicPCInfo out = new DynamicPCInfo(
	        this.LoadAverage,
	        cpuCopy,
	        this.FreeMemory,
	        memCopy,
	        gcCopy,
	        this.LoadedClass,
	        gpusCopy,
	        ncCopy,
	        nsCopy,
	        this.socketReadBytes,     // ★ 追加
	        this.socketWriteBytes,    // ★ 追加
	        diskIOCopy,
	        agentsCopy,
	        this.AgentsNum
	    );

	    out.isForecast = this.isForecast;
	    out.timeStanp  = this.timeStanp;     // 既存フィールド名に合わせてそのまま
	    return out;
	}




	@Override
	public String toString() {
		StringBuilder str=new StringBuilder();
		str.append(this.isForecast).append(",")
			.append(this.timeStanp).append(",")
			.append(this.LoadAverage).append(",");
		if (this.CPU != null) {
			str.append(this.CPU.ClockSpeed).append(",")
			   .append(this.CPU.LoadPercentByMXBean).append(",");
		//	   .append(this.CPU.LoadPercentByOSHI).append(",");
		}
		str.append(this.FreeMemory).append(",")
		   .append(this.AgentsNum).append(",");
		
		       // GC（収集器ごとの回数・時間を出す例）
	        if (this.GCStats != null && this.GCStats.Collectors != null) {
		            for (Map.Entry<String, GC.GCStat> e : this.GCStats.Collectors.entrySet()) {
		                GC.GCStat s = e.getValue();
		                str.append("GC[").append(s.Name).append("],")
		                   .append(s.CollectionCount).append(",")
		                   .append(s.CollectionTimeMs).append(",");
		            }
		        }
		if (this.GPUs != null) {
			for(Map.Entry<String, GPU> entry : this.GPUs.entrySet()) {
				str.append(entry.getValue().Name).append(",")
				   .append(entry.getValue().LoadPercent).append(",")
				   .append(entry.getValue().UsedMemory).append(",")
				   .append(entry.getValue().TotalMemory).append(",")
				   .append(entry.getValue().TemperatureC).append(",");
			}
		}
		if (this.NetworkCards != null) {
			for(Map.Entry<String, NetworkCard> entry : this.NetworkCards.entrySet()) {
				str.append(entry.getKey()).append(",")
				   .append(entry.getValue().SentByte).append(",")
				   .append(entry.getValue().ReceivedByte).append(",")
				   .append(entry.getValue().UploadSpeed).append(",")
				   .append(entry.getValue().DownloadSpeed).append(",");
			}
		}
		if (this.NetworkSpeeds != null) {
			for(Map.Entry<String, NetworkSpeed> entry : this.NetworkSpeeds.entrySet()) {
				str.append(entry.getValue().Sender).append(",")
				   .append(entry.getValue().Receiver).append(",")
				   .append(entry.getValue().UploadSpeedByOriginal).append(",")
				   .append(entry.getValue().DownloadSpeedByOriginal).append(",")
				   .append(entry.getValue().UploadSpeedByIperf3).append(",")
				   .append(entry.getValue().DownloadSpeedByIperf3).append(",");
			}
		}
		if (this.diskIO != null) {
		    str.append(this.diskIO.ReadBytes).append(",")
		       .append(this.diskIO.WriteBytes).append(",")
		       .append(this.diskIO.ReadSpeed).append(",")
		       .append(this.diskIO.WriteSpeed).append(",");
		}
		
		if (this.Agents != null) {
			for(Map.Entry<String, Agent> entry : this.Agents.entrySet()) {
				str.append(entry.getValue().ID).append(",")
				   .append(entry.getValue().Name).append(",")
				   .append(entry.getValue().StartTime).append(",");
			}
		}
		return str.toString();
	}

	/* ======================== 各サブクラス ======================== */

	public static class CPU implements Serializable {
		public long ClockSpeed;               // Hz
		public double LoadPercentByMXBean;    // 0.0〜1.0
		//public double LoadPercentByOSHI;      // 0.0〜1.0
		public double ProcessCpuLoad;         // JVMプロセス 0.0〜1.0
		public int AvailableProcessors;
		public long prev;
		public double jvmSystem;
		public double jvmUser;
		public double total;

	}

	public static class Memory implements Serializable {
		// Host
		public long HostTotalBytes;
		public long HostAvailableBytes;
		public long SwapTotalBytes;
		public long SwapUsedBytes;
		public long RSS;
		// JVM
		public long JvmHeapUsed;
		public long JvmHeapCommitted;
		public long JvmHeapMax;
		public long JvmNonHeapUsed;
		public long JvmNonHeapCommitted;

	
	}

	public static class GPU implements Serializable {
		public String Name;
		public int LoadPercent;
		public int UsedMemory;
		public int TotalMemory;
		public int TemperatureC;

		

	}

	public static class NetworkCard implements Serializable {
		public Long SentByte;
		public Long ReceivedByte;
		public Long UploadSpeed;    // bytes/interval
		public Long DownloadSpeed;  // bytes/interval

		
	}

	public static class NetworkSpeed implements Serializable {
		public String Sender;
		public String Receiver;
		public Double UploadSpeedByOriginal;
		public Double DownloadSpeedByOriginal;
		public Double UploadSpeedByIperf3;
		public Double DownloadSpeedByIperf3;

		
	}

	public static class Agent implements Serializable {
		public String ID;
		public String Name;
		public long StartTime;
		public long allocNew;
		public long allocOutside;

		
	}

	/* ===== GC ===== */

	public static class GC implements Serializable {
	    public Map<String, GCStat> Collectors = new HashMap<>();
	    public long gcCountByJFR;
	    public long gcPauseMillis;

	    public static class GCStat implements Serializable {
	        public String Name;
	        public long AllCount;
	        public long CollectionCount;
	        public long CollectionTimeMs;
	    }

	    @JsonIgnore
	    public static GC collect(DynamicPCInfo previous, long gcCount, long gcPauseMillis) {
	        GC g = new GC();
	        g.gcCountByJFR  = gcCount;
	        g.gcPauseMillis = gcPauseMillis;
	        g.Collectors    = new HashMap<>();

	        // 前回値（差分用）
	        Map<String, GCStat> prevMap = null;
	        if (previous != null && previous.GCStats != null && previous.GCStats.Collectors != null) {
	            prevMap = previous.GCStats.Collectors;
	        }

	        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
	            GCStat s = new GCStat();
	            s.Name    = bean.getName();
	            long now  = bean.getCollectionCount();   // 実装によっては -1 のこともある
	            s.AllCount = now;

	            long prevAll = 0L;
	            if (prevMap != null) {
	                GCStat prev = prevMap.get(s.Name);
	                if (prev != null) {
	                    prevAll = prev.AllCount;
	                }
	            }

	            if (now < 0L) {
	                // JVM によっては -1 が返るので、その場合は「差分0扱い」
	                s.CollectionCount = 0L;
	            } else {
	                s.CollectionCount = Math.max(0L, now - prevAll);
	            }

	            s.CollectionTimeMs = bean.getCollectionTime();
	            g.Collectors.put(s.Name, s);
	        }

	        return g;
	    }
	}
	public static class DiskIO implements Serializable {
	    public long ReadBytes;
	    public long WriteBytes;
	    public long ReadSpeed;
	    public long WriteSpeed;
	}

}