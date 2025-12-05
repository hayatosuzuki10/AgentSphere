/**
 *
 */
package primula.api.core.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import primula.agent.AbstractAgent;
import primula.api.SystemAPI;
import primula.api.core.scheduler.cpu.CPUPerformaceMeasure;
import scheduler2022.Scheduler;
import scheduler2022.util.DHTutil;

/**
 * @author yamamoto
 *
 */
class RunningAgentPool {

	private ConcurrentHashMap<String, List<AgentThread>> agentPool = new ConcurrentHashMap<String, List<AgentThread>>();
	private ArrayList<RunningAgentPoolListener> agentPoolListeners = new ArrayList<RunningAgentPoolListener>();

	/**
	 * プールにエージェントを追加します
	 *
	 * @param agentThread
	 *            実行するエージェントのスレッド
	 */
	public synchronized void addAndRunAgent(final AgentThread agentThread, String group) {
		//        System.out.println("追加:"+group);
		if (agentPool.containsKey(group)) {
			agentPool.get(group).add(agentThread);
		} else {
			agentPool.put(group, Collections.synchronizedList(new ArrayList<AgentThread>()));
			agentPool.get(group).add(agentThread);
		}
		agentThread.RegisterAgentThreadListener(new AgentThreadListener() {

			@Override
			public void AgentThreadEnd() {
				for (Entry<String, List<AgentThread>> entry : agentPool.entrySet()) {
					if (entry.getValue().contains(agentThread)) {
						entry.getValue().remove(agentThread);
						break;
					}
				}
				notifyAgentFinished(agentThread.getAgent().getAgentID(), agentThread.getAgent().getAgentName());
			}
		});

		CPUPerformaceMeasure.add(agentThread); // エージェントの平均CPU使用率を求めるための追加 2014/01/20

    	agentThread.start();
        new Thread(() -> Scheduler.analyze.analyze(agentThread.getAgent().getAgentID()), "Analyze-agent-123").start();
       
		
	}

	public synchronized void addRunningAgentPoolListener(RunningAgentPoolListener listener) {
		agentPoolListeners.add(listener);
	}

	public synchronized void removeRunningAgentPoolListener(RunningAgentPoolListener listener) {
		agentPoolListeners.remove(listener);
	}

	private void notifyAgentFinished(String agentID, String agentName) {
		for (RunningAgentPoolListener listener : agentPoolListeners) {
			listener.agentFinished(agentID, agentName);
		}
	}

	public synchronized HashMap<String, List<AgentInfo>> getAgentInfos() {
		HashMap<String, List<AgentInfo>> list = new HashMap<String, List<AgentInfo>>();
		for (String string : agentPool.keySet()) {
			list.put(string, new ArrayList<AgentInfo>());
			List<AgentThread> agentlist = agentPool.get(string);
			synchronized (agentlist) {
				for (AgentThread agentThread : agentlist) {
					try {
						AgentInfo agentInfo = new AgentInfo(agentThread.getAgent());
						String agentID = agentThread.getAgent().getAgentID();
						agentInfo.setAgentName(agentThread.getAgent().getAgentName());
						agentInfo.setAgentId(agentID);
						agentInfo.setTime(agentThread.getAgent().getTime());
						agentInfo.setAgent(agentThread.getAgent());
						list.get(string).add(agentInfo);
						if(DHTutil.getAgentInfo(agentID)==null) {
							DHTutil.setAgentInfo(agentID, agentInfo);
						}
					} catch (Exception e) {
						SystemAPI.getLogger()
								.debug("取得途中にagentが消されたかも？後で直す" + System.getProperty("line.separator") + e);
					}
				}
			}
		}
		return list;
	}
	
	public AbstractAgent getAgentByID(String agentID) {
		for(String string : agentPool.keySet()) {
			List<AgentThread> agentlist = agentPool.get(string);
			synchronized (agentlist) {
				for (AgentThread agentThread : agentlist) {
					AbstractAgent agent = agentThread.getAgent();
					String id = agent.getAgentID();
					if(agentID.equals(id)) {
						return agent;
					}
				}
			}
		}
		return null;
	}
}
