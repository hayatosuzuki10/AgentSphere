package ranporkTest.tsp;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.assh.MainPanel;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;
import ranporkTest.tsp.util.TSPmessage;
import ranporkTest.tsp.util.TspUtil;
import ranporkTest.util.CountLoadAverageAgent;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;
import scheduler2022.util.IInfoUpdateListener;
import sphereConnection.EasySphereNetworkManeger;

public class TspMasterAgent extends AbstractAgent implements IMessageListener, IInfoUpdateListener {

	private int[][] tspmap;
	private int min_cost = Integer.MAX_VALUE;
	private int[] min_path;
	private Lock minLock;
	private String[] agentIDs;
	private CountDownLatch ready;
	private int finishedAgent;
	private Instant start;

	private DynamicPCInfo maxLoadAverage;
	private DynamicPCInfo maxagentnum;

	@Override
	public void run() {
		finishedAgent = 0;
		ready = new CountDownLatch(1);
		Set<String> ips = ((EasySphereNetworkManeger) SingletonS2ContainerFactory.getContainer()
				.getComponent("EasySphereNetworkManeger")).getIPTable();
		for (var elem : ips) {
			KeyValuePair<InetAddress, Integer> ToSlave = null;
			try {
				ToSlave = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(elem), 55878);
			} catch (UnknownHostException e1) {
				// TODO 自動生成された catch ブロック
				e1.printStackTrace();
			}
			if (ToSlave != null) {
				AgentAPI.migration(ToSlave, new CountLoadAverageAgent());
			}
		}

		MessageAPI.registerMessageListener(this);
		Scheduler.registerInfoUpdateListener(this);
		minLock = new ReentrantLock();
		start = Instant.now();
		try {
			//tspmap = TspUtil.readMap("./others/tsp.txt", true);
			tspmap = TspUtil.readMap(TspUtil.SOURCEDIR + "/data/tsp17.txt", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		agentIDs = new String[tspmap.length - 1];
		for (int i = 0; i < tspmap.length - 1; i++) {
			BitSet visited = new BitSet();
			visited.set(0);
			TspSlaveAgent agent = new TspSlaveAgent(tspmap, visited, i, tspmap.length, this.getStrictName());
			agentIDs[i] = agent.getStrictName();
			AgentAPI.runAgent(agent);
		}
		ready.countDown();
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public String getStrictName() {
		// TODO Auto-generated method stub
		return this.getAgentID();
	}

	@Override
	public String getSimpleName() {
		// TODO Auto-generated method stub
		return this.getAgentName();
	}

	@Override
	public void pcInfoUpdate(DynamicPCInfo info) {
		if (maxLoadAverage.LoadAverage < info.LoadAverage) {
			maxLoadAverage = info;
		}
		if (maxagentnum.AgentsNum <= info.AgentsNum) {
			maxagentnum = info;
		}
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
		try {
			ready.await();
			try {
				minLock.lock();
				TSPmessage message = (TSPmessage) ((StandardContentContainer) envelope.getContent()).getContent();
				if (message.cost < min_cost) {
					min_cost = message.cost;
					min_path = message.path;
					StringBuilder str = new StringBuilder();

					str.append(this.getClass().getName() + ":update")
							.append("\n")
							.append(this.getClass().getName() + ":minCost> " + min_cost)
							.append("\n")
							.append(this.getClass().getName() + ":min_path ");
					for (int i : min_path) {
						str.append(i + "->");
					}
					//System.err.println(str.toString());
					for (String id : agentIDs) {
						StandardEnvelope env = new StandardEnvelope(new AgentAddress(id),
								new StandardContentContainer(message));
						MessageAPI.send(new KeyValuePair<InetAddress, Integer>(getmyIP(), 55878), env);
					}
				}
				if (message.finished) {
					finishedAgent++;
					if (finishedAgent >= agentIDs.length) {
						Set<String> ips = ((EasySphereNetworkManeger) SingletonS2ContainerFactory
								.getContainer()
								.getComponent("EasySphereNetworkManeger")).getIPTable();
						for (var elem : ips) {
							KeyValuePair<InetAddress, Integer> ToSlave = null;
							try {
								ToSlave = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(elem), 55878);
							} catch (UnknownHostException e1) {
								// TODO 自動生成された catch ブロック
								e1.printStackTrace();
							}
							if (ToSlave != null) {
								AgentAPI.migration(ToSlave, new CountLoadAverageAgent());
							}
						}
						Scheduler.removeInfoUpdateListener(this);
						StringBuilder str = new StringBuilder();
						Instant end = Instant.now();
						Duration elapse = Duration.between(start, end);
						str.append(this.getClass().getName() + ":task finish\n")
								.append(this.getClass().getName() + ":total time " + (elapse.toNanos() / 1000000.0)
										+ "[ms]\n")
								.append(this.getClass().getName() + ":minCost->" + min_cost + "\n")
								.append(this.getClass().getName() + ":min_path\n");
						for (int i : min_path) {
							str.append(i + "->");
						}
						str.append("\n");
						str.append(this.getClass().getName() + ":-maxLoadAverage-" + "\n")
								.append(this.getClass().getName() + ":LoadAverage " + maxLoadAverage.LoadAverage
										+ "\n")
								.append(this.getClass().getName() + ":Agentsnum " + maxLoadAverage.AgentsNum + "\n")
								.append(this.getClass().getName() + ":-maxAgentnum-" + "\n")
								.append(this.getClass().getName() + ":LoadAverage " + maxagentnum.LoadAverage + "\n")
								.append(this.getClass().getName() + ":Agentsnum " + maxagentnum.AgentsNum);
						System.out.println(str.toString());

						MainPanel.autoscroll();
						MessageAPI.removeMessageListener(this);
					}
				}
			} finally {
				minLock.unlock();
			}

		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

}
