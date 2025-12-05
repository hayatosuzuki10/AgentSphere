package primula.api.core.assh.command.interim;

import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.assh.command.Graph2;

public class graphtest extends AbstractCommand{

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance,
			List<String> opt) {
		// TODO Auto-generated method stub
		Graph2 g = new Graph2();
		AgentAPI.runAgent(g);
		
		return null;
	}
	
	

}
