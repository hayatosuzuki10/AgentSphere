package ranporkTest.messageMODTest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractMessagingAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class ResendeTestAgent extends AbstractMessagingAgent{

	@Override
	public void run() {
		try {
			StandardEnvelope env1 = new StandardEnvelope(new AgentAddress(this.getStrictName()), new StandardContentContainer(0));
			MessageAPI.send(new KeyValuePair<InetAddress, Integer>(InetAddress.getByName(IPAddress.myIPAddress), 55878), env1);
		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
		// TODO 自動生成されたメソッド・スタブ
		System.out.println(this.getClass().getName()+":received");
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

}
