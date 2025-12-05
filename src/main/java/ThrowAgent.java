import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.IPAddress;
import primula.util.KeyValuePair;


public class ThrowAgent extends AbstractAgent{
	private int count = 1000;
	boolean migrate = true;

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

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
