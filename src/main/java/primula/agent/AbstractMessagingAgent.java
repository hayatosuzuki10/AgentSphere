package primula.agent;

import primula.api.MessageAPI;
import primula.api.core.network.message.IMessageListener;
/**
 * メッセージング機能を実装したAbstractAgent<br>
 * 各AgentSphereで活動を開始する際にリスナーを登録する
 * @author selab
 *
 */
public abstract class AbstractMessagingAgent extends AbstractAgent implements IMessageListener{

	@Override
	public void runAgent() {
		MessageAPI.registerMessageListener(this);//runAgent呼び出し=エージェント生成もしくは移動直後
		super.runAgent();
		//runAgent終了=エージェント正常終了もしくは別スレッドに移動処理させ終わった後の実質抜け殻
		//そう思っていた時期が私にもありました
		// TODO メッセージングだけを行うようなエージェントの場合removeListenerしてしまうとまずいのでどうにかしたい
		MessageAPI.removeMessageListener(this);
	}

	@Override
	public String getStrictName() {
		return getAgentID();
	}

	@Override
	public String getSimpleName() {
		return getAgentName();
	}

}
