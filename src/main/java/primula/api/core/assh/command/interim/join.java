package primula.api.core.assh.command.interim;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import primula.api.AgentAPI;
import primula.api.SystemAPI;
import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.network.GetNodeAgent;
import primula.api.core.network.dthmodule2.address.Address;
import primula.api.core.scheduler.MachineInfo;
import primula.api.core.scheduler.PerformanceMeasure;
import primula.api.core.scheduler.ScheduleThread;
import primula.api.core.scheduler.vmemory.VMemoryViewer;
import primula.util.IPAddress;

public class join extends AbstractCommand {
	static int port = 5122;
	static String myIPAddress = IPAddress.myIPAddress;

	public List<Object> runCommand(List<String> fileNames, Object instance,
			List<String> opt) {

		MachineInfo myMachineInfo = null;
		try {
			myMachineInfo = new MachineInfo(SystemAPI.getAgentSphereId(),
					new Address(myIPAddress,port,InetAddress.getLocalHost()));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		PerformanceMeasure performance = new PerformanceMeasure();
		myMachineInfo.setPerformance(performance.measurePerfo());// 初回性能測定
		System.out.println("性能値：" + myMachineInfo.getPerformance()); // debug
		VMemoryViewer viewer = new VMemoryViewer("viewer");
		ScheduleThread scheduleThread = new ScheduleThread(myMachineInfo,
				performance, false, viewer);
		scheduleThread.start();
		GetNodeAgent getNodeAgent = new GetNodeAgent(myMachineInfo);
		AgentAPI.runAgent(getNodeAgent);

		// mercury
		//PropertiesLoader.loadPropertyFile();

		System.err.println("E_N_D");
		return null;
	}
}
