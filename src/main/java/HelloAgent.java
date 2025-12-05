/**
 * @author Mikamiyama Kaito
 */

//3秒毎に出力するエージェント。

import primula.agent.AbstractAgent;

public class HelloAgent extends AbstractAgent{
	public void run(){
		while(true){
			System.out.println("Hello, AgentSphere!");

			try{
				Thread.sleep(3000);
			}catch(InterruptedException e){
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
		//System.out.println("000000");
		//System.exit(0);
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
}