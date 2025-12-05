/**
 * @author Mikamiyama
 */

//マイグレーションの時間を計測するためのエージェント。

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class timerMaster extends AbstractAgent{
	timerAgent slave=new timerAgent();
	KeyValuePair<InetAddress,Integer> ToSlave=null;

	public void run(){
		try{
			ToSlave=new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.Slave1),55878);
		}catch(UnknownHostException e){
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		long time=System.currentTimeMillis();

		System.out.println("start="+time);

		AgentAPI.migration(ToSlave,slave);
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}