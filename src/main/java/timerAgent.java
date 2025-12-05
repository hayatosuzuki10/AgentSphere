/**
 * @author Mikamiyama
 */

//マイグレーションの時間を計測するエージェント。

import primula.agent.AbstractAgent;

public class timerAgent extends AbstractAgent{
	public void run(){
		long time=System.currentTimeMillis();

		System.out.println("time="+time);
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}