import primula.agent.AbstractAgent;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;


public class gotoTest2 extends AbstractAgent implements IMessageListener{


	public void run()
	{
		System.out.println("hoge");
//		this.migrate();
//		while(true) {
//
//		}
//		System.out.println("received");
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
		System.out.println("000000");
		System.exit(0);
	}

}
