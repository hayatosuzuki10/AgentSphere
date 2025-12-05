package primula.api.core.assh.command.interim;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.network.LinkAgent;
import primula.api.core.network.dthmodule2.data.hubimpl.IntegerHub;
import primula.api.core.network.dthmodule2.data.hubimpl.IntegerRange;
import primula.api.core.scheduler.MachineInfo;
import primula.api.core.scheduler.ScheduleThread;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

/*
 * 現在このコマンドはcreateしたノードのみ使える。子ノードがやるの禁止
 */
public class newhub extends AbstractCommand {
	int port = 5122;
	String myIPAddress = IPAddress.myIPAddress;

	public List<Object> runCommand(List<String> fileNames, Object instance,
			List<String> opt) {
		int plus = 0;
		int bound = 0;
		List<KeyValuePair<InetAddress, Boolean>> list = new ArrayList<KeyValuePair<InetAddress, Boolean>>();
		List<MachineInfo> machineList = ScheduleThread.getMachineList();

		IntegerRange intRange = new IntegerRange("INTEGER", 0, 10, 10);
		IntegerHub hub = new IntegerHub("INTEGER", 3);
		hub.setRange(intRange);

		if (machineList != null) {
			for (MachineInfo info : machineList) {
				KeyValuePair<InetAddress, Boolean> adder = new KeyValuePair<InetAddress, Boolean>();
				adder.setKey(info.getIp().getAddress());
				adder.setValue(false);
				list.add(adder);
			}
			plus = hub.getRange().getMaxValue() / (machineList.size() + 1);
		}
		else{
			plus = hub.getRange().getMaxValue();
		}


		LinkAgent agent = new LinkAgent(plus, bound, list, hub);
		AgentAPI.runAgent(agent);

		return null;
	}
}
