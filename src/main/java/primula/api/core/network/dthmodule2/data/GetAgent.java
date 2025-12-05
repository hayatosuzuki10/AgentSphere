package primula.api.core.network.dthmodule2.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import primula.Main;
import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.network.dthmodule2.data.hubimpl.IntegerHub;
import primula.util.KeyValuePair;

public class GetAgent extends AbstractAgent {
	IntegerHub hub;
	DataRange range;
	LinkedList<Serializable> listOfEntriesValue;
	KeyValuePair<InetAddress, Integer> next = null;
	Boolean finish;

	public GetAgent(DataRange range) {
		this.range = range;
		listOfEntriesValue = new LinkedList<Serializable>();
		this.finish = false;
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void runAgent() {
		// TODO 自動生成されたメソッド・スタブ
		if (this.finish == true) {
			System.out.println("listOfEntriesValue:" + listOfEntriesValue);
		} else {
			hub = (IntegerHub) Main.container.getHub("INTEGER");// 暫定的にIntegerHubにしてるだけ。そのうち自分で選択できるように。
			if (hub.getRange().isInRange(range) == 0) {
				System.out.println("getAgent");
				listOfEntriesValue = (LinkedList<Serializable>) hub
						.getEntry(range);
				System.out.println("listOfEntriesValue:" + listOfEntriesValue);
				try {
					if (this.getmyIP() != InetAddress.getLocalHost()) {
						this.finish = true;
						next = new KeyValuePair<InetAddress, Integer>(this
								.getmyIP(), 55878);
						AgentAPI.migration(next, this);
					}
				} catch (UnknownHostException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			} else {
				System.out.println("else2");
				next = new KeyValuePair<InetAddress, Integer>(hub
						.getDirectSuccessor(), 55878);
				AgentAPI.migration(next, this);
			}
		}
	}

}
