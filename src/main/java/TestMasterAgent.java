/*
 * @author Mikamiyama
 */

//AgentSphereに複数のエージェントを残すためのテスト用エージェント。sleepAgentを使用。

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class TestMasterAgent extends AbstractAgent{
	sleepAgent slave1=new sleepAgent();
	sleepAgent slave2=new sleepAgent();
	sleepAgent slave3=new sleepAgent();
	KeyValuePair<InetAddress,Integer> ToSlave1=null;
	KeyValuePair<InetAddress,Integer> ToSlave2=null;
	KeyValuePair<InetAddress,Integer> ToSlave3=null;

	public void run(){
		try{
			ToSlave1=new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.Slave1),55878);
			ToSlave2=new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.Slave2),55878);
			ToSlave3=new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.Slave3),55878);
		}catch(UnknownHostException e){
			e.printStackTrace();
		}

		 AgentAPI.migration(ToSlave1,slave1);
         AgentAPI.migration(ToSlave2,slave2);
         AgentAPI.migration(ToSlave3,slave3);
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}