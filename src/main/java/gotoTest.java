import primula.agent.AbstractAgent;
import primula.api.core.assh.MainPanel;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;


public class gotoTest extends AbstractAgent implements IMessageListener{

	private boolean running=true;
	int i=0;

	public void run()
	{
		System.out.println("hoge"+i++);
		//this.migrate();
		while(running) {
			System.out.println("hoge"+i++);
			MainPanel.autoscroll();
//			Continuation.suspend();
			//this.migrate();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
//		System.exit(0);
		//Continuation.cancel();
		running=false;
//		try {
//			Thread.sleep(60000);
//		} catch (InterruptedException e) {
//			// TODO 自動生成された catch ブロック
//			e.printStackTrace();
//		}
//		Continuation.suspend();
//		this.run();
//		Runtime run = Runtime.getRuntime();
//		run.runFinalizersOnExit(true);
//		Thread.currentThread().halt();
//		Thread.currentThread().resume();
//		executorService.shutdown();
	}

}
