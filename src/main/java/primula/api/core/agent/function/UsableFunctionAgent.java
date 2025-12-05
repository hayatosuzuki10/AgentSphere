package primula.api.core.agent.function;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.WrittenSenderEnvelope;

public abstract class UsableFunctionAgent extends AbstractAgent implements IMessageListener{
	private static final long serialVersionUID = 1L;
	private WrittenSenderEnvelope envelope;
//	private boolean flag = true; // migrateが完成したら解禁


	public void run() {
//		System.out.println(this.getAgentName());////////////////////////////////
        try {
			MessageAPI.registerMessageListener(this);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        runMyTask();
        
        MessageAPI.removeMessageListener(this);
	}
	
	protected abstract void runMyTask();
	
	protected void useFunction(Object... objects) {
		AgentAPI.useFunction(this.getAgentName(), objects);

		synchronized(this) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
// migrateが完成したら解禁
//		while(flag) {
//			synchronized(this) {
//				try {
//					wait(10000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			KeyValuePair<InetAddress, Integer> address = null;
//			try {
//				address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.IPAddress),55878);
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//			}
//			flag = false;
//			AgentAPI.migration(address, this);
//		}
	}

	@Override
	public void requestStop() {
	}

	@Override
	public String getStrictName() {
		return getAgentID();
	}

	@Override
	public String getSimpleName() {
		return getAgentName();
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
		this.envelope = (WrittenSenderEnvelope) envelope;
		receivedResult(this.envelope.getSenderName());
        synchronized(this) {
        	notify();
        }
	}
	
	protected abstract void receivedResult(String moduleAgentName);
	
	protected Object openContent() {
		StandardContentContainer content = (StandardContentContainer) this.envelope.getContent();
		return content.getContent();
	}

}
