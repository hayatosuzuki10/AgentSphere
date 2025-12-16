package scheduler2022.util;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import primula.api.DHTChordAPI;
import primula.api.core.agent.AgentInfo;
import scheduler2022.Scheduler;
import sphereConnection.stub.SphereSpec;

/**
 * スケジューラ機能に必要な情報をDHTに登録するための便利クラス<br>
 * DHT全体でkeyの衝突を避けるために引数で渡されたkeyとは別に識別符号を付与してDHTにputしたりgetしたりする
 *
 */
public class DHTutil {
	private static final String keyCode = "2022scheduler:";
	private static final String SpecCode = "Spec:";
	private static final String PICode = "pi:";
	private static final String PIStampCode = "pis:";
	private static final String StaticPICode = "stapi:";
	private static final String StaticPIStampCode = "stapis:";
	private static final String AcceptableCode = "acceptable:";
	private static final String AgentCode = "agent";
	private static long timeStampExpire = Scheduler.getTimeStampExpire();
	private static int skipCount = 0;

	public static void setSpec(String key, SphereSpec spec) {
		DHTChordAPI.put(keyCode + SpecCode + key, spec);
	}

	public static SphereSpec getSpec(String key) {
		return (SphereSpec) DHTChordAPI.get(keyCode + SpecCode + key);
	}

	public static boolean containsSpec(String key) {
		return DHTChordAPI.contains(keyCode + SpecCode + key);
	}
	

	public static void setAcceptable(String key, boolean canAccept) {
		DHTChordAPI.put(keyCode + AcceptableCode + key, canAccept);
	}
	
	public static void setAgentInfo(String key, AgentInfo agentInfo) {
		DHTChordAPI.put(keyCode + AgentCode + key, agentInfo);
	}
	
	public static boolean containsAgent(String key) {
		return DHTChordAPI.contains(keyCode + AgentCode + key);
	}

	public static Instant getPcInfoTimeStamp(String key) {
		return (Instant) DHTChordAPI.get(keyCode + PIStampCode + key);
	}

	public static boolean canAccept(String key) {
	    Boolean val = (Boolean) DHTChordAPI.get(keyCode + AcceptableCode + key);
	    return val != null && val; // null 安全
	}
	
	public static AgentInfo getAgentInfo(String key) {
		return (AgentInfo) DHTChordAPI.get(keyCode + AgentCode + key);
	}
	
	
	public static void removeAcceptable(String key) {
		DHTChordAPI.remove(keyCode + AcceptableCode + key);
	}
	public static void removeAgentInfo(String key) {
		DHTChordAPI.remove(keyCode + AgentCode + key);
	}
	
	synchronized public static Set<String> getAllAgentIDs() {
		List<String> allList = DHTChordAPI.listAll();
		Set<String> allAgentInfos = new HashSet<>();
		for(String value : allList) {
			if(value.contains("AgentInfo")) {
				
				int start = value.indexOf("id='") + 4; // "id='" の直後
				int end = value.indexOf("'", start);   // 次のシングルクォート
				String id = value.substring(start, end);

				allAgentInfos.add(id);
			}
		}
		return allAgentInfos;

	}
	

}
