package scheduler2022.strategy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import primula.agent.AbstractAgent;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;

public class LoadAverageStrategy implements SchedulerStrategy{
	private double delta = 1;
	private double average = 0;
	private boolean CanReserve = true;
	private String NextDirection = IPAddress.myIPAddress;

	@Override
	public void initialize() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void excuteMainLogic() {
		Queue<String> highLoadAverageIPs = new LinkedList<>();
		Queue<String> lowLoadAverageIPs = new LinkedList<>();
		
		Map<String, Double> loadAverages = makeLoadAverageMap();
		double averageLoadAverage = getAverage(loadAverages);
		average = averageLoadAverage;
		CanReserve = true;

		for (String ip : loadAverages.keySet()) {
			double loadAverage = loadAverages.get(ip);
    
			if (loadAverage > averageLoadAverage) {
				highLoadAverageIPs.offer(ip);
			} else if (loadAverage < averageLoadAverage) {
				lowLoadAverageIPs.offer(ip);
			}
		}
		
		String highLoadAverageIP, lowLoadAverageIP;
//		while ((highLoadAverageIP = highLoadAverageIPs.poll()) != null) {
//			if(!lowLoadAverageIPs.isEmpty()) {
//				lowLoadAverageIP = lowLoadAverageIPs.poll();
//			} else {
//				break;
//			}
//			DynamicPCInfo dpi = DHTutil.getPcInfo(highLoadAverageIP);
//			List<DynamicPCInfo.Agent> agents = dpi.Agents;
//			if(agents.isEmpty()) break;
//			String agentID = agents.get(0).ID;
//            System.out.println(highLoadAverageIP + " " + lowLoadAverageIP +" "+ agentID);
//
//			if(highLoadAverageIP == IPAddress.myIPAddress) {
//                AbstractAgent agent = AgentAPI.getAgentByID(agentID);
//                if(agent != null) {
//                	agent.moveByExternal(lowLoadAverageIP);
//                }
//			} else {
//				MigrateInstruction instr = new MigrateInstruction(agentID, lowLoadAverageIP, true);
//				SchedulerMessenger.sendMigrateRequest(highLoadAverageIP, 8888, instr);
//			}
//			demo.routes.add(agentID +" of "+ highLoadAverageIP +" migrate to "+ lowLoadAverageIP);
//		}
		highLoadAverageIP = highLoadAverageIPs.poll();
		lowLoadAverageIP = lowLoadAverageIPs.poll();
		NextDirection = lowLoadAverageIP;
		
		
	}

	private double getAverage(Map<String, Double> loadAverages) {
		double sumLoadAverage = 0;
		double IPSize = loadAverages.size();
		for(String ip : loadAverages.keySet()) {
			double loadAverage = loadAverages.get(ip);
			sumLoadAverage += loadAverage;
		}

		double averageLoadAverage = sumLoadAverage / IPSize;
		return averageLoadAverage;
	}

	private Map<String, Double> makeLoadAverageMap() {
		Map<String, Double> loadAverageMap = new HashMap<>();
		Set<String> currentIPs = Scheduler.getAliveIPs();
		currentIPs.add(IPAddress.myIPAddress);
		for(String ip : currentIPs) {
			DynamicPCInfo dpi = Scheduler.getDpis().get(ip);
			if (dpi != null) {
			    loadAverageMap.put(ip, dpi.LoadAverage);
			}
		}
		return loadAverageMap;
	}

	@Override
	public void cleanUp() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public synchronized boolean shouldMove(AbstractAgent agent) {
		DynamicPCInfo myInfo = Scheduler.getDpis().get(IPAddress.myIPAddress);
		if (myInfo == null) {
			System.err.println("[WARN] shouldMove(): DynamicPCInfo または LoadAverage が null です");
			return false;
		}
		double myLoadAverage = myInfo.LoadAverage;
		if (CanReserve && average + delta < myLoadAverage ) {
			CanReserve = false;
			System.out.println("Reserve" + myLoadAverage +" ; "+ average);
			return true;
		}

		//System.out.println("Full");
		return false;
		

	}

	@Override
	public String getDestination(AbstractAgent agent) {
		return NextDirection;
	}
	
	public void setDelta(int newDelta) {
		delta = newDelta;
	}
	
}
