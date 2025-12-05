import primula.agent.AbstractAgent;

/**
 * @author Mikamiyama Kaito
 */

//エージェント名を取得するテスト。

public class TestAgent extends AbstractAgent{
	public void run(){
		System.out.println("AgentName:"+this.getAgentName());
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}