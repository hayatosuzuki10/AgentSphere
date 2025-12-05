package primula.api.core.assh.command;

import java.util.List;

import primula.api.core.scheduler.MachineInfo;
import primula.api.core.scheduler.ScheduleThread;

public class list extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		List<MachineInfo> machineList = ScheduleThread.getMachineList();
		for(MachineInfo eachInfo : machineList) {
			System.out.println("IPアドレス：" + eachInfo.getIp());
		}
		return null;
	}

}
