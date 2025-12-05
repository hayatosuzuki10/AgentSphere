package primula.api.core.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import primula.Main;
import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.network.dthmodule2.data.hubimpl.IntegerHub;
import primula.api.core.network.dthmodule2.data.hubimpl.IntegerRange;
import primula.util.KeyValuePair;

public class LinkAgent extends AbstractAgent {
	private int plus;
	private int bound;
	private List<KeyValuePair<InetAddress, Boolean>> list = new ArrayList<KeyValuePair<InetAddress, Boolean>>();
	private IntegerHub hub;
	private InetAddress pre;
	private boolean first;
	private boolean finish;

	public LinkAgent(int p, int b, List<KeyValuePair<InetAddress, Boolean>> l,
			IntegerHub h) {
		this.plus = p;
		this.bound = b;
		this.list = l;
		this.hub = h;
		this.pre = this.getmyIP();
		this.first = true;
		this.finish = false;
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	/*
	 * (非 Javadoc)
	 *
	 * @see primula.agent.AbstractAgent#runAgent()
	 *
	 * このエージェントを巡回させることでMercuryの情報共有ネットワークを構築させる。
	 *
	 * 初回： 自分の担当範囲を設定→Successorを設定→hubをコンテナに格納→次の島へ
	 *
	 * 2回目以降： 自分の担当範囲を設定→Predecessorを設定→Successorを設定→hubをコンテナに格納→次の島へ
	 *
	 * 最初の島へ戻ってきた： Predecessorを設定→最初にコンテナに格納したhubのPredecessorを設定→終了
	 */
	@Override
	public void runAgent() {
		KeyValuePair<InetAddress, Integer> next = null;

		if (!finish) {
			// 自分の担当範囲を設定
			IntegerRange range = new IntegerRange(this.hub.getType(), bound,
					bound + plus, this.hub.getRange().getMaxValue());
			bound += (plus + 1);
			this.hub.setRange(range);
		}

		if (!first) {
			// Predecessorを設定
			this.hub.setDirectPredecessor(this.pre);
		}

		if (!finish) {
			// Successorを設定
			if (!check(this.list)) {
				for (KeyValuePair<InetAddress, Boolean> pair : this.list) {
					if (!pair.getValue()) {
						this.hub.setDirectSuccessor(pair.getKey());
						pair.setValue(true);
						next = new KeyValuePair<InetAddress, Integer>(pair
								.getKey(), 55878);
						break;
					}
				}
			} else {
				this.hub.setDirectSuccessor(this.getmyIP());
			}
		}

		// hubをコンテナに格納
		if (!finish) {
			Main.container.addHub(this.hub);
		} else {
			Main.container.getHub(this.hub.getType()).setDirectPredecessor(
					this.pre);
		}

		try {
			pre = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if (first) {
			first = false;
		}
		if (finish) {
		} else if (next == null) {
			next = new KeyValuePair<InetAddress, Integer>(this.getmyIP(), 55878);
			this.finish = true;
			AgentAPI.migration(next, this);
		} else {
			AgentAPI.migration(next, this);
		}
	}

	/*
	 * リストを見ていけるところがあればfalseを返し、 すべて回ったか、リストがnull（つまり自分だけ）の場合は trueを返す。
	 */
	private boolean check(List<KeyValuePair<InetAddress, Boolean>> l) {
		if (l == null) {
			return true;
		} else {
			for (KeyValuePair<InetAddress, Boolean> pair : l) {
				if (!pair.getValue()) {
					return false;
				}
			}
			return true;
		}
	}

}
