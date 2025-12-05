/**
 * @author Mikamiyama
 */


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class test2 extends AbstractAgent{
	test2_tmp1 slave1=new test2_tmp1();
	test2_tmp1 slave2=new test2_tmp1();
	test2_tmp2 slave3=new test2_tmp2();

	KeyValuePair<InetAddress,Integer> Toslave1=null;
	KeyValuePair<InetAddress,Integer> Toslave2=null;
	KeyValuePair<InetAddress,Integer> Toslave3=null;

	public void run(){

		try{
			Toslave1=new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.Slave1),55878);
			Toslave2=new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.Slave2),55878);
			Toslave3=new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.Slave3),55878);
		}catch(UnknownHostException e){
			e.printStackTrace();
		}

		 AgentAPI.migration(Toslave1, slave1);
         AgentAPI.migration(Toslave2, slave2);
         AgentAPI.migration(Toslave3, slave3);
	}
	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}

