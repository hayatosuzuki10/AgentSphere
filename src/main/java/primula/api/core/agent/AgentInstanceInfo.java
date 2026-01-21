package primula.api.core.agent;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo.Agent;
import scheduler2022.InformationCenter;
import scheduler2022.Scheduler;
import scheduler2022.util.DHTutil;

public class AgentInstanceInfo implements Serializable{
	
		private String id;
		private String name;
		private String ipAddress = IPAddress.myIPAddress;
		private long threadId;
		private long startTime;
	    private int cpuChange = 5000;
	    private int gpuChange = 5000;
	    private long networkUpChange = 5000;
	    private long networkDownChange = 5000;

	    // ★ 追加：メモリ負荷の内訳
	    private long heapChange = 0;         // ヒープ差分(bytes)
	    private int gcCountChange = 0;      // GC 回数差分
	    private long diskReadChange = 0;
	    private long diskWriteChange = 0;
	    
	    private double priority = 0.5;
	    private double progress;
	    private double migrateTime;
	    private double previousMigrateTime;
		private List<history> history = new ArrayList<history> ();
		
		private Map<Long, Integer> cpuChangeRecords = new HashMap<>();
		private Map<Long, Integer> gpuChangeRecords = new HashMap<>();
		private Map<Long, Long> networkUpChangeRecords = new HashMap<>();
		private Map<Long, Long> networkDownChangeRecords = new HashMap<>();
		private Map<Long, Long> heapChangeRecords = new HashMap<>();
	    private Map<Long, Integer> gcCountChangeRecords = new HashMap<>();
		private Map<Long, Long> diskReadChangeRecords = new HashMap<>();
		private Map<Long, Long> diskWriteChangeRecords = new HashMap<>();
	    
		
	    @JsonIgnore
	    public void recordCPUChange(int cpuChange, long time) {
	        if (cpuChangeRecords.isEmpty()) {
	            this.setCpuChange(cpuChange);
	        } else {
	            this.setCpuChange(ema(this.getCpuChange(), cpuChange, Scheduler.getEmaAlpha()));
	        }
	        cpuChangeRecords.put(time, cpuChange);
	        AgentClassInfo info = InformationCenter.getAgentClassInfo(this.name);
	        if (info == null) return;   // ★busy時はスキップ
	        info.setCpuChange(cpuChange);
	        DHTutil.setAgentInfo(this.name, info);
	        
	    }

	    @JsonIgnore
	    public void recordGPUChange(int gpuChange, long time) {
	        if (gpuChangeRecords.isEmpty()) {
	            this.gpuChange = gpuChange;
	        } else {
	            this.gpuChange = ema(this.gpuChange, gpuChange, Scheduler.getEmaAlpha());
	        }
	        gpuChangeRecords.put(time, gpuChange);
	        AgentClassInfo info = InformationCenter.getAgentClassInfo(this.name);

	        if (info == null) return;   // ★busy時はスキップ
	        info.setGpuChange(gpuChange);
	        DHTutil.setAgentInfo(this.name, info);
	    }

	

	    @JsonIgnore
	    public void recordNetworkUpChange(long networkUpChange, long time) {
	        if (networkUpChangeRecords.isEmpty()) {
	            this.networkUpChange = networkUpChange;
	        } else {
	            this.networkUpChange = ema(this.networkUpChange, networkUpChange, Scheduler.getEmaAlpha());
	        }
	        networkUpChangeRecords.put(time, networkUpChange);
	        AgentClassInfo info = InformationCenter.getAgentClassInfo(this.name);

	        if (info == null) return;   // ★busy時はスキップ
	        info.setNetworkUpChange(networkUpChange);
	        DHTutil.setAgentInfo(this.name, info);
	    }

	    @JsonIgnore
	    public void recordNetworkDownChange(long networkDownChange, long time) {
	        
	        if (networkDownChangeRecords.isEmpty()) {
	            this.networkDownChange = networkDownChange;
	        } else {
	            this.networkDownChange = ema(this.networkDownChange, networkDownChange, Scheduler.getEmaAlpha());
	        }
	        
	        networkDownChangeRecords.put(time, networkDownChange);
	        AgentClassInfo info = InformationCenter.getAgentClassInfo(this.name);

	        if (info == null) return;   // ★busy時はスキップ
	        info.setNetworkDownChange(networkDownChange);
	        DHTutil.setAgentInfo(this.name, info);
	    }
	    
	    @JsonIgnore
	    public void recordHeapChange(long heapChange, long time) {
	        if (heapChangeRecords.isEmpty()) {
	            this.heapChange = heapChange;
	        } else {
	            this.heapChange = ema(this.heapChange, heapChange, Scheduler.getEmaAlpha());
	        }
	        heapChangeRecords.put(time, heapChange);
	        AgentClassInfo info = InformationCenter.getAgentClassInfo(this.name);

	        if (info == null) return;   // ★busy時はスキップ
	        info.setHeapChange(heapChange);
	        DHTutil.setAgentInfo(this.name, info);
	    }

	    

	    // ★ 追加：GC 回数差分
	    @JsonIgnore
	    public void recordGCCountChange(int gcCountChange, long time) {
	        if (gcCountChangeRecords.isEmpty()) {
	            this.gcCountChange = gcCountChange;
	        } else {
	            this.gcCountChange = ema(this.gcCountChange, gcCountChange, Scheduler.getEmaAlpha());
	        }
	        gcCountChangeRecords.put(time, gcCountChange);
	        AgentClassInfo info = InformationCenter.getAgentClassInfo(this.name);

	        if (info == null) return;   // ★busy時はスキップ
	        info.setGCCountChange(gcCountChange);
	        DHTutil.setAgentInfo(this.name, info);
	    }
	    
	    @JsonIgnore
	    public void recordDiskIOChange(long read, long write, long time) {
	        // ---- Read ----
	        if (diskReadChangeRecords.isEmpty()) {
	            this.diskReadChange = read;
	        } else {
	            this.diskReadChange = ema(this.diskReadChange, read, Scheduler.getEmaAlpha());
	        }
	        diskReadChangeRecords.put(time, read);

	        // ---- Write ----
	        if (diskWriteChangeRecords.isEmpty()) {
	            this.diskWriteChange = write;
	        } else {
	            this.diskWriteChange = ema(this.diskWriteChange, write, Scheduler.getEmaAlpha());
	        }
	        diskWriteChangeRecords.put(time, write);

	        // ---- DHT更新 ----
	        AgentClassInfo info = InformationCenter.getAgentClassInfo(this.name);
	        if (info != null) {
	            info.setDiskReadChange(read);
	            info.setDiskWriteChange(write);
	            DHTutil.setAgentInfo(this.name, info);
	        }
	    }
		
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
		
		
		@JsonIgnore
		private AbstractAgent agent;
		
		public AgentInstanceInfo(AbstractAgent agent) {
			this.agent =agent;
		}
		
	    public AgentInstanceInfo(
	            String id,
	            String name,
	            String ipAddress,
	            AbstractAgent agent,
	            long threadId,
	            long startTime,
	            int cpuChange,
	            int gpuChange,
	            long memoryChange,
	            long networkChange,
	            double priority,
	            double progress,
	            double migrateTime,
	            double previousMigrateTime
	    ) {
	        this.id = id;
	        this.name = name;
	        this.setIpAddress(ipAddress);
	        this.agent = agent;
	        this.setThreadId(threadId);
	        this.setStartTime(startTime);
	        this.setCpuChange(cpuChange);
	        this.gpuChange = gpuChange;
	        this.networkUpChange = networkChange;
	        this.setPriority(priority);
	        this.progress = progress;
	        this.setMigrateTime(migrateTime);
	        this.setPreviousMigrateTime(previousMigrateTime);
	    }
		
	    @Override
	    public String toString() {
	        return "AgentInstanceInfo{" +
	                "id='" + id + '\'' +
	                ", name='" + name + '\'' +
	                ", ipAddress='" + getIpAddress() + '\'' +
	                ", startTime=" + getStartTime() +
	                ", cpuChange=" + getCpuChange() +
	                ", gpuChange=" + gpuChange +
	                ", networkChange=" + networkUpChange +
	                ", heapChange=" + heapChange +
	                ", gcCountChange=" + gcCountChange +
	                ", priority=" + getPriority() +
	                ", progress=" + progress +
	                ", migrateTime=" + getMigrateTime() +
	                ", previousMigrateTime=" + getPreviousMigrateTime() +
	                '}';
	    }
		

		public static class history implements Serializable{
			String ip;
			long time;
			history(String ip,long time){
				this.ip = ip;
				this.time = time;
			}

		}
		
		@JsonIgnore
		public Map<String, Agent> getAgents(){
			Map<String, Agent> agents = new HashMap<>();
			HashMap<String,List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();
			S2Container factory = S2ContainerFactory.create("./setting/StartupAgent.dicon");//diconの中身が読み込まれ、Agentとして起動される
			List<String> agentNames = new ArrayList<>();
			for (int i = 0; i < factory.getComponentDefSize(); i++) {
				Object agent = factory.getComponentDef(i).getComponent();
				agentNames.add(agent.getClass().getName());
				
			}

			for(String string:agentInfos.keySet()){
	            for(AgentInstanceInfo info:agentInfos.get(string)){
	            	if(agentNames.contains(info.getAgentName()))
	            		continue;
	            	Agent agent = new Agent();
	            	agent.ID = info.getAgentId();
	            	agent.Name = info.getAgentName();
	            	agent.StartTime = info.getTime();
	            	agents.put(agent.ID, agent);
	            }
	        }
			return agents;
		}
		
		public String getAgentName() {
	        return this.name;
	    }

	    /**
	     * @param agentName the agentName to set
	     */
	    public void setAgentName(String agentName) {
	        this.name = agentName;
	    }

	    /**
	     * @return the agentId
	     */
	    public String getAgentId() {
	        return id;
	    }

	    /**
	     * @param agentId the agentId to set
	     */
	    public void setAgentId(String agentId) {
	        this.id = agentId;
	    }

	    /**
	     * @author Mikamiyama
	     * @param runtime
	     */
	    public void setTime(long runtime){
	    	this.setStartTime(runtime);
	    }

	    public long getTime(){
	    	return this.getStartTime();
	    }
	    
	    @JsonIgnore
	    public AbstractAgent getAgent() {
	    	return this.agent;
	    }

	    @JsonIgnore
	    public void setAgent(AbstractAgent agent) {
	    	this.agent = agent;
	    }

//	    goto
	    public void RegistarHistory(String ip) {
			long time = System.currentTimeMillis() - this.getStartTime();
			for(int i=0;i<this.history.size();i++) {
				time -= this.history.get(i).time;
			}
			this.history.add(new history(ip,time));
		}
		public long getHistoryTime(int num) {
//			his.push(new);
			return this.history.get(num).time;
		}
		public String getHistoryIP(int num) {
			return this.history.get(num).ip;
		}
		public int getHistoryLength() {

			return this.history.size();
		}

		public String getIpAddress() {
			return ipAddress;
		}

		public void setIpAddress(String ipAddress) {
			this.ipAddress = ipAddress;
			this.RegistarHistory(ipAddress);
		}

		public long getStartTime() {
			return startTime;
		}

		public void setStartTime(long startTime) {
			this.startTime = startTime;
		}

		public long getThreadId() {
			return threadId;
		}

		public void setThreadId(long threadId) {
			this.threadId = threadId;
		}

		public double getPreviousMigrateTime() {
			return previousMigrateTime;
		}

		public void setPreviousMigrateTime(double previousMigrateTime) {
			this.previousMigrateTime = previousMigrateTime;
		}

		public double getMigrateTime() {
			return migrateTime;
		}

		public void setMigrateTime(double migrateTime) {
			this.migrateTime = migrateTime;
		}

		public int getCpuChange() {
			return cpuChange;
		}

		public void setCpuChange(int cpuChange) {
			this.cpuChange = cpuChange;
		}

		public double getPriority() {
			return priority;
		}

		public void setPriority(double priority) {
			this.priority = priority;
		}
		    
		
		
}
