package primula.api.core.assh.command;

import java.util.List;

import primula.api.core.backup.AgentMove;

/*
 *  move agentID IPAddress
 *  @author satou
 */

public class move extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		if(fileNames.isEmpty()) AgentMove.move();
		else AgentMove.move(fileNames.get(0), "133.220.114.244");
//		System.err.println(fileNames + " ***" + instance);
//		if(fileNames.size() != 2) {
//			System.err.println("manual : move agentID IPAddress");
//			return null;
//		}
//		AgentMove.move(fileNames.get(0), fileNames.get(1));
		return null;
	}

}
