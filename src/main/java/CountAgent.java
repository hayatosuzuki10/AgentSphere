import java.util.List;

import primula.api.core.agent.function.ModuleAgent;


public class CountAgent extends ModuleAgent {

	private int max = Integer.MAX_VALUE;

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	protected void doModule() {
		for(int i=0; i<100; i++){
			for(int j=0; j<max; j++){

			}
			System.out.println(i + " : " + client);
		}

		System.out.println("finish");
	}

	@Override
	protected void receivedData(List<Object> data) {
		// TODO 自動生成されたメソッド・スタブ
		
	}

}
