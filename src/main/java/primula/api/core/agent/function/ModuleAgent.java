package primula.api.core.agent.function;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

/*
 * マーキュリーが出来たら消す箇所（クラス）
 * ①AgentAPI
 * ②ModuleAgent
 * ③ModuleAgent
 * ④ModuleAgentManager
 * 以上
 */

public abstract class ModuleAgent extends AbstractAgent implements IMessageListener, Cloneable{
	private static final long serialVersionUID = 1L;
	private boolean requestStop = false;
	protected boolean requestDoModule = false;
	private long waitTime = 10000;
	protected String client;
	private List<Object> datas;

	boolean migrateFlag = true;
	boolean isClone = false;

	protected String clientAddress;// 暫定(マーキュリーが完成したら消す)

	public @continuable void run() {

		registerMessageListener();

		while(requestStop == false) {
			if(requestDoModule) {
				if(isClone == false)System.out.println("オリジナル  : client-->" + client + " , @ " + clientAddress);
				else System.out.println("クローン : client-->" + client + " , @ " + clientAddress);
				doModule();
				requestDoModule = false;
				if(isClone) requestStop = true;
			}

			synchronized(this) {
				try {
					wait(waitTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// マシン間を移動させたくない場合はmigrateFlagに「!」をつける
			if(!migrateFlag) {
				removeMessageListener();
				Continuation.suspend();
				migrateFlag = false;
				System.out.println("TEST1 ****************");
				if(MessageAPI.checkMessageListener(this)) {
					System.out.println("TEST2 ****************");
					requestDoModule = ModuleAgentManager.getInstance().versionUP(this, (ModuleAgent) MessageAPI.getListener(this));
					if(requestDoModule) {
						receivedData(datas);
					}
				}
			}
	        registerMessageListener();
//			AgentAPI.migration(new KeyValuePair(ScheduleAPI.getHighPerformancedMachine(), 55878), this);
		}
    	removeMessageListener();
	}

	protected abstract void doModule();

	protected void finishTask(Object result) {
//		if(requestStop == false) ModuleAgentManager.getInstance().returnResult(client, this.getAgentName(), result);
		if(requestStop == false) // 暫定(マーキュリーが完成したら消す)③
			ModuleAgentManager.getInstance().returnResult(client, clientAddress, this.getAgentName(), result);
	}

	private void registerMessageListener() {
		if(isClone == false) {
			try {
				MessageAPI.registerMessageListener(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void removeMessageListener() {
		if(isClone == false) MessageAPI.removeMessageListener(this);
	}

	@Override
	public void requestStop() {
    	requestStop = true;
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
    	if(requestDoModule) {
    		ModuleAgent cloneAgent = (ModuleAgent) this.clone();
    		cloneAgent.requestDoModule = false;
    		cloneAgent.isClone = true;
    		cloneAgent.receivedMessage(envelope);
//    		AgentAPI.runAgent(cloneAgent);
    		System.out.println("クローン生成");
    		try {
				AgentAPI.migration(new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.IPAddress),55878), cloneAgent);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
    	} else {
    		StandardContentContainer content = (StandardContentContainer)envelope.getContent();
    		datas = (ArrayList<Object>) content.getContent();
    		client = (String) datas.remove(0);
    		clientAddress = (String) datas.remove(0); // 暫定(マーキュリーが完成したら消す)②
    		receivedData(datas);
    		requestDoModule = true;
    		synchronized(this) {
    			notify();
    		}
    	}
    }

    protected abstract void receivedData(List<Object> data);


    public Object clone() {
    	ModuleAgent cloneAgent = null;
    	try {
			cloneAgent = (ModuleAgent) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return cloneAgent;
    }

    public void setClient(String client) {
    	this.client = client;
    }

    public String getClient() {
    	return client;
    }

    public void setDatas(ArrayList<Object> datas) {
    	this.datas = datas;
    }

    public List<Object> getDatas() {
    	return datas;
    }

    public boolean getRequestDoModule() {
    	return requestDoModule;
    }
}
