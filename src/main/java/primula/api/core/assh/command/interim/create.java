package primula.api.core.assh.command.interim;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import primula.api.SystemAPI;
import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.network.dthmodule2.address.Address;
import primula.api.core.scheduler.MachineInfo;
import primula.api.core.scheduler.PerformanceMeasure;
import primula.api.core.scheduler.ScheduleThread;
import primula.api.core.scheduler.vmemory.VMemoryViewer;
import primula.util.IPAddress;

public class create extends AbstractCommand {
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
		
		VMemoryViewer viewer = new VMemoryViewer("viewer"); // debug 2014/11/18
		
		ScheduleThread scheduleThread = new ScheduleThread(myMachineInfo,
				performance, true, viewer);
		scheduleThread.start();




		//PropertiesLoader.loadPropertyFile();

		//System.err.println("Property where to find propeties file::"+ PropertiesLoader.PROPERTY_WHERE_TO_FIND_PROPERTY_FILE);

		//MercuryImpl node = new MercuryImpl();
		//MercuryUtil.setImpl(node);


		return null;
	}

}
