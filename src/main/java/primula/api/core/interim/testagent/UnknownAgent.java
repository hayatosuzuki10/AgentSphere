package primula.api.core.interim.testagent;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class UnknownAgent extends AbstractAgent {
	private int count = 1;
	boolean migrate = true;

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void runAgent() {
		while(count < 10) {
			if(count == 5 && migrate) {
				System.out.println("5nau");
				migrate = false;
				KeyValuePair<InetAddress, Integer> address = null;
				try {
					address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.IPAddress), 55878);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				AgentAPI.migration(address, this);
			}
			System.out.println(count);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			count++;
		}
	}

}
