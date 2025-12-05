package ranporkTest.messageMODTest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import primula.agent.AbstractAgent;
import primula.agent.util.DHTutil;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
/**
 * メッセージAPI改良テスト
 * 再送機能および、TTLによるメッセージの廃棄が実装できているかをテストします
 * @author Niikura
 *
 */
public class SendTestAgent extends AbstractAgent{


	@Override
	public void run() {
		UUID id=UUID.randomUUID();//虚無に送る
		try {
			DHTutil.setAgentIP(id.toString(), InetAddress.getByName(IPAddress.myIPAddress));
			StandardEnvelope env1 = new StandardEnvelope(new AgentAddress(id.toString()), new StandardContentContainer(0));
			MessageAPI.send(new KeyValuePair<InetAddress, Integer>(InetAddress.getByName(IPAddress.myIPAddress), 55878), env1);
		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

}
