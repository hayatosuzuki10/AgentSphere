package primula.api.core.assh.command;

import java.util.List;

import primula.api.SystemAPI;
import primula.api.core.scheduler.ScheduleThread;

public class exit extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		ScheduleThread.requestStop();
		SystemAPI.shutdown();
		System.out.println("exit");
		System.exit(0);
		return null;
	}

}
