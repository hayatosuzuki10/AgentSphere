package primula.api.core.assh.command.interim;

import java.util.List;

import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.scheduler.MachineInfo;
import primula.api.core.scheduler.ScheduleThread;

public class showMachineList extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance,
			List<String> opt) {
		
		System.out.print(ScheduleThread.getMyMachineInfo().getIp()+" ");
		for(MachineInfo i:ScheduleThread.getMachineList()){
			System.out.print(i.getIp()+" ");
		}
		System.out.println();
		
		return null;
	}
	
	

}
