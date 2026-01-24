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
	    public void recordCPUChange(int cpuChange, long time, int agentChange) {
	        if (cpuChangeRecords.isEmpty()) {
	            this.setCpuChange(cpuChange);
	        } else {
	        	if(agentChange == 1) {
	        		this.setCpuChange(ema(this.getCpuChange(), cpuChange, 0.8));
	        	} else {
	        		this.setCpuChange(ema(this.getCpuChange(), cpuChange, 0.2));
	        	}
	            
	        }
	        cpuChangeRecords.put(time, cpuChange);
	        
	    }

	    @JsonIgnore
	    public void recordGPUChange(int gpuChange, long time, int agentChange) {
	        if (gpuChangeRecords.isEmpty()) {
	            this.gpuChange = gpuChange;
	        } else {

	        	if(agentChange == 1) {
		            this.gpuChange = ema(this.gpuChange, gpuChange, 0.8);
	        	} else {
		            this.gpuChange = ema(this.gpuChange, gpuChange, 0.2);
	        	}
	        }
	        gpuChangeRecords.put(time, gpuChange);
	    }

	

	    @JsonIgnore
	    public void recordNetworkUpChange(long networkUpChange, long time, int agentChange) {
	        if (networkUpChangeRecords.isEmpty()) {
	            this.networkUpChange = networkUpChange;
	        } else {
	        	if(agentChange == 1) {
		            this.networkUpChange = ema(this.networkUpChange, networkUpChange, 0.8);
	        	} else {
		            this.networkUpChange = ema(this.networkUpChange, networkUpChange, 0.2);
	        	}
	        }
	        networkUpChangeRecords.put(time, networkUpChange);
	    }

	    @JsonIgnore
	    public void recordNetworkDownChange(long networkDownChange, long time, int agentChange) {
	        
	        if (networkDownChangeRecords.isEmpty()) {
	            this.networkDownChange = networkDownChange;
	        } else {
	        	if(agentChange == 1) {
		            this.networkDownChange = ema(this.networkDownChange, networkDownChange, 0.8);
	        	} else {
		            this.networkDownChange = ema(this.networkDownChange, networkDownChange, 0.2);
	        	}
	        }
	        
	        networkDownChangeRecords.put(time, networkDownChange);
	    }
	    
	    @JsonIgnore
	    public void recordHeapChange(long heapChange, long time, int agentChange) {
	        if (heapChangeRecords.isEmpty()) {
	            this.heapChange = heapChange;
	        } else {
	        	if(agentChange == 1) {
		            this.heapChange = ema(this.heapChange, heapChange, 0.8);
	        	} else {
		            this.heapChange = ema(this.heapChange, heapChange, 0.2);
	        	}
	        }
	        heapChangeRecords.put(time, heapChange);
	    }

	    

	    // ★ 追加：GC 回数差分
	    @JsonIgnore
	    public void recordGCCountChange(int gcCountChange, long time, int agentChange) {
	        if (gcCountChangeRecords.isEmpty()) {
	            this.gcCountChange = gcCountChange;
	        } else {
	        	if(agentChange == 1) {
		            this.gcCountChange = ema(this.gcCountChange, gcCountChange, 0.8);
	        	} else {
		            this.gcCountChange = ema(this.gcCountChange, gcCountChange, 0.2);
	        	}
	        }
	        gcCountChangeRecords.put(time, gcCountChange);
	    }
	    
	    @JsonIgnore
	    public void recordDiskIOChange(long read, long write, long time, int agentChange) {
	        // ---- Read ----
	        if (diskReadChangeRecords.isEmpty()) {
	            this.diskReadChange = read;
	        } else {
	        	if(agentChange == 1) {
		            this.diskReadChange = ema(this.diskReadChange, read, 0.8);
	        	} else {
		            this.diskReadChange = ema(this.diskReadChange, read, 0.2);
	        	}
	        }
	        diskReadChangeRecords.put(time, read);

	        // ---- Write ----
	        if (diskWriteChangeRecords.isEmpty()) {
	            this.diskWriteChange = write;
	        } else {
	        	if(agentChange == 1) {
		            this.diskWriteChange = ema(this.diskWriteChange, write, 0.8);
	        	} else {
		            this.diskWriteChange = ema(this.diskWriteChange, write, 0.2);
	        	}
	        }
	        diskWriteChangeRecords.put(time, write);

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
