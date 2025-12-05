package primula.api.core.assh.command.interim;

/* AgentMonitorAgentを起動する */

import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.interim.monitor.AgentMonitorAgent;

public class am extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		AgentMonitorAgent agent = new AgentMonitorAgent();
        AgentAPI.runAgent(agent);
		return null;
	}
}
