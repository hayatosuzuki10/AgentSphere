package ranporkTest.chorddhtDHTTest;

import primula.agent.AbstractAgent;
import primula.api.DHTChordAPI;

public class DHTTestPutAgent extends AbstractAgent{

	@Override
	public void requestStop() {

	}

	@Override
	public void run() {
		DHTChordAPI.put("test1", "OK");
	}

}
