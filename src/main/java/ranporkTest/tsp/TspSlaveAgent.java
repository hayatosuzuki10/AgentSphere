package ranporkTest.tsp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import ranporkTest.tsp.util.TSPmessage;
import scheduler2022.RecommendedDest;

public class TspSlaveAgent extends AbstractAgent implements IMessageListener {

	private int[][] tspmap;
	private int min_cost = Integer.MAX_VALUE;
	private int[] min_path;
	private Lock minLock;
	private BitSet visited;
	private int rank;
	private int size;
	private String parentStrictName;

	public TspSlaveAgent() {
		throw new UnsupportedOperationException("mclからの実行はできません");
	}

	public TspSlaveAgent(int[][] map, BitSet visited, int rank, int size, String parentstrictname) {
		super();
		this.tspmap = map;
		this.visited = visited;
		visited.set(rank + 1);
		this.rank = rank;
		this.size = size;
		this.min_path = new int[map.length];
		this.parentStrictName = parentstrictname;
		minLock = new ReentrantLock();
	}

	@Override
	public void run() {
		if (size / 3 < rank) {
			InetAddress addr;
			try {
				addr = InetAddress.getByName(IPAddress.IPAddress);
				System.out.println(this.getClass().getName() + ":rank" + rank + " go to " + IPAddress.IPAddress);
			} catch (UnknownHostException e) {
				throw new RuntimeException("なんかダメだった", e);
			}
			migrate(addr);
		}
		MessageAPI.registerMessageListener(this);
		//System.err.println(this.getClass().getName()+":rank is ->"+rank);
		int[] path = new int[tspmap.length];
		path[0] = 0;//出発地点は0
		path[1] = this.rank + 1;//次の移動先はこのエージェントの担当範囲
		dfs(tspmap, visited, path, tspmap.length, arrgetter(tspmap, 0, rank + 1), 2);
		System.out.println(this.getClass().getName() + ":rank" + rank + "done.");
		int[] resultpath = new int[tspmap.length];
		int resultcost = 0;
		minLock.lock();
		try {
			System.arraycopy(min_path, 0, resultpath, 0, min_path.length);
			resultcost = min_cost;
		} finally {
			minLock.unlock();
		}
		sendPath(resultpath, resultcost, true);

		MessageAPI.removeMessageListener(this);
	}

	/**
	 * エージェントはこのような配列アクセス関数を作ってあげないとコンパイルでこける
	 * @param arr
	 * @param x
	 * @param y
	 * @return
	 */
	private int arrgetter(int arr[][], int x, int y) {
		return arr[x][y];
	}

	private @continuable void dfs(int[][] map, BitSet visited, int[] path_in, int mapSize, int cost_in, int depth) {
		if (depth == mapSize) {//=>葉=>ルート構築完了
			//int cost = cost_in + map[path_in[depth - 1]][path_in[0]];
			int cost = cost_in + arrgetter(map, path_in[depth - 1], path_in[0]);

			boolean update = false;
			try {
				minLock.lock();
				if (cost < min_cost) {
					StringBuilder str = new StringBuilder();
					str.append("rank->" + rank + ":");
					min_cost = cost;
					System.arraycopy(path_in, 0, min_path, 0, path_in.length);
					for (int i : min_path) {
						str.append(i + "->");
					}
					str.append(" total=" + cost);
					//System.out.println(str.toString());
					update = true;
				}
			} finally {
				minLock.unlock();
			}
			if (update) {
				sendPath(path_in, cost, false);
			}
		}

		if (depth == mapSize / 2) {
			String dest = RecommendedDest.RecomDest();//行き来が多すぎてsocketが例外をはく
			if (!IPAddress.myIPAddress.equals(dest)) {
				MessageAPI.removeMessageListener(this);
				//System.err.println(this.getClass().getName() + ":rank" + rank + " migrate to " + dest);
				InetAddress addr;
				try {
					addr = InetAddress.getByName(dest);
				} catch (UnknownHostException e) {
					throw new RuntimeException("なんかダメだった", e);
				}
				migrate(addr);
				//System.err.println(this.getClass().getName() + ":rank" + rank + " migrated");
				MessageAPI.registerMessageListener(this);
			}
		}

		for (int i = 0; i < mapSize; i++) {
			if (!visited.get(i)) {//未到達
				path_in[depth] = i;
				int cost = cost_in;
				if (depth > 0) {
					//cost += map[path_in[depth - 1]][i];
					cost += arrgetter(map, path_in[depth - 1], i);
				}

				try {
					minLock.lock();
					if (cost > min_cost) {
						//System.out.println("continue!");
						continue;
					}
				} finally {
					minLock.unlock();
				}
				visited.set(i);
				dfs(map, visited, path_in, mapSize, cost, depth + 1);
				visited.clear(i);
			}
		}
	}

	private void sendPath(int[] path, int cost, boolean finish) {
		TSPmessage message = new TSPmessage();
		message.path = path;
		message.cost = cost;
		message.finished = finish;
		StandardEnvelope env = new StandardEnvelope(new AgentAddress(parentStrictName),
				new StandardContentContainer(message));
		MessageAPI.send(new KeyValuePair<InetAddress, Integer>(getmyIP(), 55878), env);
	}

	@Override
	public void requestStop() {
	}

	@Override
	public String getStrictName() {
		return this.getAgentID();
	}

	@Override
	public String getSimpleName() {
		return this.getAgentName();
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
		try {
			minLock.lock();
			TSPmessage message = (TSPmessage) ((StandardContentContainer) envelope.getContent()).getContent();
			if (message.cost < min_cost) {
				min_cost = message.cost;
				min_path = message.path;
			}
		} finally {
			minLock.unlock();
		}
	}

}
