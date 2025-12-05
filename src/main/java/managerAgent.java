import primula.agent.AbstractAgent;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;

/*
 * 目標：管理エージェントの作成
 * 役割：一定間隔で、AgentSphere間を移動し、データの更新等を行う。
 * 現状：AgentSphere間を移動するテストプログラム
 *
 */

public class managerAgent extends AbstractAgent implements IMessageListener {

	public void run() {
		while (true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			this.migrate();
		}
	}

	@Override
	public String getStrictName() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public String getSimpleName() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}
}