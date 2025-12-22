package scheduler2022;

import java.util.HashMap;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.AgentInstanceInfo;
import scheduler2022.util.DHTutil;

public class MostHeavyAgentSpecify {
	public MostHeavyAgentSpecify() {
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public AbstractAgent get() {
		HashMap<String,List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();
		long maxmemory = 0;
		AbstractAgent MostHeavyAgent = null;

		for(String key : agentInfos.keySet())  {
			for(AgentInstanceInfo ai : agentInfos.get(key)) {
				if(DHTutil.containsSpec(ai.getAgentName())) {
					long temp = DHTutil.getSpec(ai.getAgentName()).memoryused;
					if(temp > maxmemory) {
						System.out.println(ai.getAgentName());
						maxmemory = temp;
						MostHeavyAgent = ai.getAgent();
					}
				}
			}
		}

		return MostHeavyAgent;
	}
}
