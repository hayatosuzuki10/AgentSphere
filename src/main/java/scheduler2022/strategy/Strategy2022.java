package scheduler2022.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import primula.agent.AbstractAgent;
import primula.api.core.assh.ConsolePanel;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;
import scheduler2022.util.DHTutil;

public class Strategy2022 implements SchedulerStrategy {


	private static Map<String, Integer> AgentnumsMap;
	private static int allAgents;
	private static double LoadAverage;
	private String NextDirection;
	
	@Override
	public void initialize() {

		Set<String> currentIPs = DHTutil.getAllSuvivalIPaddresses();
		Map<String, Double> nextmigratehint = new ConcurrentHashMap<>();
		Map<String, Integer> nextAgentnumsMap = new ConcurrentHashMap<>();
		int nextallAgents = 0;

		double sumla = 0;
		
		for (String ip : currentIPs) {
			DynamicPCInfo dpi = DHTutil.getPcInfo(ip);
			if (dpi != null && dpi.LoadAverage > 0.0) { //計測してない場合もあるのでその時はやらない
				nextAgentnumsMap.put(ip, dpi.AgentsNum);
				nextallAgents += dpi.AgentsNum;
				nextmigratehint.put(ip, dpi.LoadAverage);
				sumla += dpi.LoadAverage;
				
			}
			if (IPAddress.myIPAddress.equals(ip)) {
			    LoadAverage = dpi.LoadAverage;
			}
			//System.err.println(key+" LoadAverage:"+cpi.load_average);
		}

		
		
		//ConsolePanel.autoscroll();

		//ロードアベレージ{"1.1.1.1":2 "1.1.1.2":3 "1.1.1.3":5 } sum:10 ->a+b+c

		double reciproSumLa = 0;//この後正規化するための比率合計
		for (String key : nextmigratehint.keySet()) {
			double rla = sumla / nextmigratehint.get(key);//"R"atio"L"oad"A"varegeだと思う
			nextmigratehint.put(key, rla);
			reciproSumLa += rla;
			//System.err.println(key+":"+newMap.get(key));
		}
		//割り振る比率{"1.1.1.1":5 "1.1.1.2":3.3333... "1.1.1.3":2 } sum:10.333333333 -> x(reciproSumLa)

		for (String key : nextmigratehint.keySet()) {
			nextmigratehint.put(key, nextmigratehint.get(key) / reciproSumLa);//それぞれ総計で割る
		}
		//正規化 {"1.1.1.1":5/x "1.1.1.2":3.3333/x "1.1.1.3":2/x } sum:1

		//		for (var a : newMap.entrySet()) {
		//			System.err.println(a.getKey() + ":" + a.getValue());
		//		}

		//この段階で代入することで多分タイミングずれとか防止
		//要はcopyOnWriteと同じ理屈になるはず
		Scheduler.migratehint = nextmigratehint;
		AgentnumsMap = nextAgentnumsMap;
		allAgents = nextallAgents;
	}

	@Override
	public void excuteMainLogic() {
		Queue<String> nextmigrateTickets = new ConcurrentLinkedQueue<String>();
		//自分自身のでーたがないとき
		//ロードアベレージを含むデータなのでタイミングによってはない
		if (!Scheduler.migratehint.containsKey(IPAddress.myIPAddress)) {
			return;
		}
		int myAgentsNum = AgentnumsMap.get(IPAddress.myIPAddress);
		double myLoadAverageRatio = Scheduler.migratehint.get(IPAddress.myIPAddress);
		int idealMyAgentsNum = (int) Math.round(allAgents * myLoadAverageRatio);
		int mydiff = myAgentsNum - idealMyAgentsNum;
		//{現状の数}-{理想の数}<0 => 足りていない数<0
		//つまり足りていないorちょうどいい場合はできることないので終わり
		if (mydiff <= 0) {
			Scheduler.migrateTickets = nextmigrateTickets;
			return;
		}
		//		System.err.println(this.getClass().getName() + ":" + "     Allagents->" + allAgents);
		//あまってるので配る
		List<KeyValuePair<String, Integer>> diffList = new ArrayList<KeyValuePair<String, Integer>>();
		for (Entry<String, Double> a : Scheduler.migratehint.entrySet()) {
			int idealAgentsNum = (int) Math.round(allAgents * a.getValue());
			int AgentsNum = AgentnumsMap.get(a.getKey());
			int diff = idealAgentsNum - AgentsNum;
			//System.err.println(this.getClass().getName() + ":" + "Key->" + a.getKey());
			//System.err.println(this.getClass().getName() + ":" + "value->" + a.getValue());
			//System.err.println(this.getClass().getName() + ":" + "idealAgentsNum->" + idealAgentsNum);
			//System.err.println(this.getClass().getName() + ":" + "AgentsNum->" + AgentsNum);
			if (diff <= 0) {//足りてるのは無視 上のほうのmydiffとは正負逆なので注意
				continue;
			}
			KeyValuePair<String, Integer> keyvalue = new KeyValuePair<String, Integer>(a.getKey(), diff);
			diffList.add(keyvalue);
		}

		//エージェントを受け入れる余裕がある順にソート
		Collections.sort(diffList, (a, b) -> {
			return b.getValue().compareTo(a.getValue());
		});

		StringBuilder str = new StringBuilder();
		str.append("=====difflist=======\n");
		str.append("mydiff : " + mydiff + "\n");
		str.append("myAgentsNum : " + myAgentsNum + "\n");
		str.append("myLoadAverage : " + LoadAverage + "\n");
		str.append("myLoadAverage ratio : " + myLoadAverageRatio + "\n");
		for (var as : diffList) {
			str.append(as.getKey() + " : " + as.getValue() + "\n");
			for (int i = 0; i < as.getValue() && mydiff > 0; i++, mydiff--) {//足りない分だけQueueに行先を詰める
				nextmigrateTickets.add(as.getKey());
			}
		}
		if (!nextmigrateTickets.isEmpty())
			System.err.println(str.toString());
		ConsolePanel.autoscroll();
		Scheduler.migrateTickets = nextmigrateTickets;
		NextDirection = nextmigrateTickets.poll();
		
		
	}

	@Override
	public void cleanUp() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public boolean shouldMove(AbstractAgent agent) {
		return true;
	}

	@Override
	public String getDestination(AbstractAgent agent) {
		
		return null;
	}

}
