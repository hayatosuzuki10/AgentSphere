package primula.api.core.scheduler;

import java.net.InetAddress;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.KeyValuePair;

public class CommunicateAgent extends AbstractAgent {
	private MachineInfo myMachineInfo;                        // 自分のマシンの情報
    private KeyValuePair<InetAddress, Integer> accessAddress;
    boolean migFlag = true;

	public CommunicateAgent(MachineInfo info, InetAddress destination) {
		this.myMachineInfo = info;
		this.accessAddress = new KeyValuePair<InetAddress, Integer>(destination, 55878);
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void runAgent() {
		if(migFlag) {
			migFlag = false;
			AgentAPI.migration(accessAddress, this);
		} else {
			ScheduleThread.resetMachineInfo(myMachineInfo);
		}

	}

}
