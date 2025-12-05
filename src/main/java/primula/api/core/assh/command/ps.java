package primula.api.core.assh.command;

import java.util.HashMap;
import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.agent.AgentInfo;

public class ps extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
        HashMap<String, List<AgentInfo>> agentInfos = AgentAPI.getAgentInfos();
        for (String string : agentInfos.keySet()) {
            System.out.println(string + ":");
            for (AgentInfo info : agentInfos.get(string)) {
                System.out.println("\t" + info.getAgentName() + ":" + info.getAgentId());
            }
        }
		return null;
	}
}
