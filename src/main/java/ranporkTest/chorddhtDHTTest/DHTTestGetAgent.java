package ranporkTest.chorddhtDHTTest;

import primula.agent.AbstractAgent;
import primula.api.DHTChordAPI;

public class DHTTestGetAgent extends AbstractAgent {

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void run() {
		System.out.println(DHTChordAPI.get("test1").toString());
	}

}
