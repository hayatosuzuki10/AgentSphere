


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.scheduler.ScheduleThread;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class UnknownAgent3 extends AbstractAgent {
	private int count = 1000;
	boolean migrate = true;

	@Override
	public void requestStop() {
		System.err.println("Agentをstopします。"  + this);
		// TODO 自動生成されたメソッド・スタブ
		synchronized (this) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}

	@Override
	public void runAgent() {
		while(count < 1020) {
			if(count == 1009) migrate = true;
			if(count == 1014) migrate = true;
			if(count == 1005 && migrate) {
				System.out.println("更新したよ" );
				migrate = false;
				KeyValuePair<InetAddress, Integer> address = null;
//				address = new KeyValuePair<InetAddress, Integer>(ScheduleThread.adviseWhereAgentShouldMigrate(), 55878);
				try {
					address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.IPAddress), 55878);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				AgentAPI.migration(address, this);
				return;
			}
			if(count == 1010 && migrate) {
				System.out.println("更新したよ" );
				migrate = false;
				KeyValuePair<InetAddress, Integer> address = null;
//				address = new KeyValuePair<InetAddress, Integer>(ScheduleThread.adviseWhereAgentShouldMigrate(), 55878);
				try {
					address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.IPAddress), 55878);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				AgentAPI.migration(address, this);
				return;
			}
			if(count == 1015 && migrate) {
				System.out.println("更新したよ" );
				migrate = false;
				KeyValuePair<InetAddress, Integer> address = null;
				address = new KeyValuePair<InetAddress, Integer>(ScheduleThread.adviseWhereAgentShouldMigrate(), 55878);
				try {
					address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.IPAddress), 55878);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				AgentAPI.migration(address, this);
				return;
			}
			System.out.println(count + " " + migrate);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			count++;
		}
	}

}
