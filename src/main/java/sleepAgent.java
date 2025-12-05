/**
 * @author Mikamiyama Kaito
 */

import primula.agent.AbstractAgent;

public class sleepAgent extends AbstractAgent{
	public void run(){
		System.out.println("Hello, world!");
		try {
			Thread.sleep(20000);     //20秒間スリープ
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}