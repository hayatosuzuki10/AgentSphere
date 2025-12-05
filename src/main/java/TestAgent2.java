/*テストエージェントなので要りません!!*/
/**
 *
 * @author satoh
 */


import primula.agent.AbstractAgent;
import primula.api.AgentAPI;


public class TestAgent2 extends AbstractAgent{
	int backupNumber=0;
	int i=1;

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
	}

	@Override
	public synchronized void runAgent(){
		switch(backupNumber){
		case 0:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
		case 1:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
		case 2:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
		case 3:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
		case 4:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
		case 5:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
		case 6:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
		case 7:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
		case 8:
			function(i++);
			AgentAPI.backup(this,++backupNumber,true);
			break;
		}
		function(i++);
		System.out.println("おわり");
		AgentAPI.backup(this,++backupNumber,false);
	}

	private void function(int num){
		System.out.println(num+"番目の処理だよ");

		try {
			wait(1500);
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
}
