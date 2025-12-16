import java.util.HashMap;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.AgentInfo;
import scheduler2022.Scheduler;
import scheduler2022.util.DHTutil;

public class atest extends AbstractAgent{
	public void run() {
		HashMap<String, List<AgentInfo>> agentInfos = AgentAPI.getAgentInfos();
		for(String key : agentInfos.keySet()) {
			for(AgentInfo ai : agentInfos.get(key)) {
				System.out.println(ai.getAgentName());
			}
		}
		if(DHTutil.containsSpec(this.getAgentName())){
			System.out.println(DHTutil.getSpec(this.getAgentName()).memoryused);
			System.out.println(DHTutil.getSpec(this.getAgentName()).spec);
			System.out.println(DHTutil.getSpec(this.getAgentName()).time);
		}
		for(String key : Scheduler.getAliveIPs()) {
			System.out.println(key);
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

}
