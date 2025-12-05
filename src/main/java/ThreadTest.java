import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;


public class ThreadTest extends AbstractAgent {
	boolean BackupFlag = false;
	int BACKUPNUM = 0;
	
	int count = 0;
	int num;
	String agentName;
	long time;
	long start;
	long end;

	public ThreadTest(int n, String agentStrictName) {
		this.count = 0;
		this.num = n;
		agentName = agentStrictName;
	}

	@Override
	public void runAgent() {
		if(BackupFlag == false) {
			start = System.currentTimeMillis();
			end = 30000;
		} else {
			start = System.currentTimeMillis();
			end = end - time;
			time = 0;
		}
		while(BackupFlag==true || time < end) {
			count++;
			time = System.currentTimeMillis() - start;
			if(count % 10000000 == 0) {
				BackupFlag = true;
				if(AgentAPI.backup (this, ++BACKUPNUM, true) ) return;
				BackupFlag = false;
			}
		}

	    if(AgentAPI.backup ( this , ++BACKUPNUM ,false) ) ;
		
//		System.out.println("性能" + num + " : " + count + " ***********");
		int[] box = new int[2];
		box[0] = num;
		box[1] = count;
		KeyValuePair<InetAddress, Integer> address;
		try {
			address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName("133.220.114.240"),55878);
			MessageAPI.send(address, new StandardEnvelope(new AgentAddress(agentName), new StandardContentContainer(box)));
		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

}
