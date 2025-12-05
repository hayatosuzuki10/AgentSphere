import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;

// 使用しなくなったテストコード

public class ModuleTestAgent extends AbstractAgent implements IMessageListener{
	private int[] array;

	@Override
	public void runAgent() {
        try {
			MessageAPI.registerMessageListener(this);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        
        
		int[] num = {5,10,3};
//		AgentAPI.useFunction("SortAgent", 3, num);
		synchronized(this) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for(int n : array) {
			System.out.print(n + " ");
		}
		System.out.println();
        MessageAPI.removeMessageListener(this);
	}


	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public String getStrictName() {
		return getAgentID();
	}

	@Override
	public String getSimpleName() {
		// TODO 自動生成されたメソッド・スタブ
		return getAgentName();
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
		/*
		try {
			array = (int[]) AgentAPI.getResult("SortAgent", (WrittenSenderEnvelope) envelope);
		} catch (NoMatchModuleAgentException e) {
			System.out.println("miss!");
		}
        synchronized(this) {
        	notify();
        }
        */
	}

}
