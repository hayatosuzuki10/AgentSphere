import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;

/**
 * @author Mikamiyama Kaito
 */

public class RecieveTestAgent extends AbstractAgent implements IMessageListener{
	public void run(){
		System.out.println("ReceiveTestAgent Start!\n");

		try{     //受け取る側だけこの記述が必要
			MessageAPI.registerMessageListener(this);
		} catch (Exception e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
		try{
			Thread.sleep(5000);
		}catch(InterruptedException e2){
			// TODO 自動生成された catch ブロック
			e2.printStackTrace();
		}
	}
	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}

	@Override
	public String getStrictName(){
		// TODO 自動生成されたメソッド・スタブ
		return this.getAgentID();
	}

	@Override
	public String getSimpleName(){
		// TODO 自動生成されたメソッド・スタブ
		return this.getAgentName();
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope){
		// TODO 自動生成されたメソッド・スタブ

		System.out.println("Recieved Message!\n");
	}
}
