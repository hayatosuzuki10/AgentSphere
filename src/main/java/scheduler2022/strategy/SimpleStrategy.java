package scheduler2022.strategy;

import primula.agent.AbstractAgent;
import primula.util.IPAddress;

public class SimpleStrategy implements SchedulerStrategy{

	@Override
	public void initialize() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void excuteMainLogic() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void cleanUp() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public boolean shouldMove(AbstractAgent agent) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	@Override
	public String getDestination(AbstractAgent agent) {
		// TODO 自動生成されたメソッド・スタブ
		return  IPAddress.myIPAddress;
	}

}
