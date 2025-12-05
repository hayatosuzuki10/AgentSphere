package primula.api.core.network.dthmodule2.data;

import java.net.InetAddress;

import primula.Main;
import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.network.dthmodule2.data.hubimpl.IntegerHub;
import primula.api.core.network.dthmodule2.data.hubimpl.IntegerRange;
import primula.util.KeyValuePair;

public class InsertAgent extends AbstractAgent{
	IntegerHub hub;
	KeyValuePair<InetAddress, Integer> next = null;
	KeyValuePair<Integer,Integer> key= null;//保存するデータ用変数。保存するデータ型はいろいろ変わると思うけど今回はIntegerHubだけに対応。


	public InsertAgent(int value){//保存するデータをもらう
		this.key=new KeyValuePair<Integer, Integer>(value,value);
	}
	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void runAgent() {
		// TODO 自動生成されたメソッド・スタブ
		hub=(IntegerHub)Main.container.getHub("INTEGER");//暫定的にIntegerHubにしてるだけ。そのうち自分で選択できるように。
		if(hub.getRange().isInRange(new IntegerRange("INTEGER",key.getKey(),key.getKey(),key.getKey()))!=-1){//おかしい
			hub.setEntry(key);
			Main.container.updateHub(hub);
			System.out.println("Success!");
		}
		else{
			System.out.println("else");
			next=new KeyValuePair<InetAddress, Integer>(hub.getDirectSuccessor(), 55878);
			AgentAPI.migration(next, this);
		}
	}

}
