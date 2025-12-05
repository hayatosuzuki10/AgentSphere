package primula.api.core.agent.function;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.api.core.network.message.WrittenSenderEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

/**
 * @author okubo
 * 2013/9/25
 */
public class ModuleAgentManager {
	
    private static ModuleAgentManager ModuleAgentManager;
	
    public static synchronized ModuleAgentManager getInstance() {
        if (ModuleAgentManager == null) {
        	ModuleAgentManager = new ModuleAgentManager();
        }
        return ModuleAgentManager;
    }

	public void useFunction(String moduleAgentName, List<Object> objects) {
		KeyValuePair<InetAddress, Integer> address = null;
		try {
			address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.IPAddress),55878);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		StandardContentContainer contents = new StandardContentContainer((Serializable)objects);
		StandardEnvelope envelope = new StandardEnvelope(new AgentAddress(moduleAgentName), contents);

		MessageAPI.send(envelope);
//		System.out.println(IPAddress.IPAddress);
//		MessageAPI.send(address, envelope);
	}
	
	public void returnResult(String client, String moduleAgentName, Object result) {
		KeyValuePair<InetAddress, Integer> address = null;
		try {
			address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.IPAddress),55878);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		StandardContentContainer contents = new StandardContentContainer((Serializable)result);
		WrittenSenderEnvelope envelope = new WrittenSenderEnvelope(new AgentAddress(client), contents, moduleAgentName);

//		MessageAPI.send(envelope);
		MessageAPI.send(address, envelope);
	}

	public boolean versionUP(ModuleAgent newVersion, ModuleAgent oldVersion) {
		newVersion.setClient(oldVersion.getClient());
		newVersion.setDatas((ArrayList)oldVersion.getDatas());
		oldVersion.requestStop();
		return oldVersion.getRequestDoModule();
	}
	
	
	// 暫定(マーキュリーが完成したら消す)④-----------------------------------------------ここから------
	public void returnResult(String client, String returnAddress, String moduleAgentName, Object result) {
		KeyValuePair<InetAddress, Integer> address = null;
		try {
			address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(returnAddress),55878);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		StandardContentContainer contents = new StandardContentContainer((Serializable)result);
		WrittenSenderEnvelope envelope = new WrittenSenderEnvelope(new AgentAddress(client), contents, moduleAgentName);

//		MessageAPI.send(envelope);
		MessageAPI.send(address, envelope);
	}
	// ------------------------------------------------------------------------------------ここまで消す--
}
