package scheduler2022.util;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.api.DHTChordAPI;
import primula.api.core.agent.AgentClassInfo;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;
import sphereConnection.EasySphereNetworkManeger;
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

	public static void setSpec(String key, SphereSpec spec) {
		DHTChordAPI.put(keyCode + SpecCode + key, spec);
	}

	public static SphereSpec getSpec(String key) {
		return (SphereSpec) DHTChordAPI.get(keyCode + SpecCode + key);
	}

	public static boolean containsSpec(String key) {
		return DHTChordAPI.contains(keyCode + SpecCode + key);
	}

	public static void setPcInfo(String key, DynamicPCInfo cpi) {
	    long now = System.currentTimeMillis();
	    DynamicPCInfo prevDPI = DHTutil.getPcInfo(key);

	    // ① 今回が予測なら、常にセットしてよい
	    if (cpi.isForecast) {
	        DHTChordAPI.put(keyCode + PICode + key, cpi);
	        DHTChordAPI.put(keyCode + PIStampCode + key, Instant.now());
	        return;
	    }

	    // ② 今回が実値 → 前回が有効な予測なら、上書き禁止
	    boolean prevIsValidForecast =
	            prevDPI != null &&
	            prevDPI.isForecast &&
	            (prevDPI.timeStanp + timeStampExpire > now);

	    if (prevIsValidForecast) {;
	        return;
	    }

	    // ③ それ以外はセットしてよい
	    DHTChordAPI.put(keyCode + PICode + key, cpi);
	    DHTChordAPI.put(keyCode + PIStampCode + key, Instant.now());
	    
	}
	
	public static void setStaticPCInfo(String key, StaticPCInfo spi) {
		DHTChordAPI.put(keyCode + StaticPICode + key, spi);
		DHTChordAPI.put(keyCode + StaticPIStampCode + key, Instant.now());
	}

	public static void setAcceptable(String key, boolean canAccept) {
		DHTChordAPI.put(keyCode + AcceptableCode + key, canAccept);
	}
	
	public static void setAgentInfo(String key, AgentClassInfo agentInfo) {
		DHTChordAPI.put(keyCode + AgentCode + key, agentInfo);
	}
	
	public static boolean containsAgent(String key) {
		return DHTChordAPI.contains(keyCode + AgentCode + key);
	}

	public static DynamicPCInfo getPcInfo(String key) {
		return (DynamicPCInfo) DHTChordAPI.get(keyCode + PICode + key);
	}
	
	public static StaticPCInfo getStaticPCInfo(String key) {
		return (StaticPCInfo) DHTChordAPI.get(keyCode + StaticPICode + key);
	}

	public static Instant getPcInfoTimeStamp(String key) {
		return (Instant) DHTChordAPI.get(keyCode + PIStampCode + key);
	}

	public static boolean canAccept(String key) {
	    Boolean val = (Boolean) DHTChordAPI.get(keyCode + AcceptableCode + key);
	    return val != null && val; // null 安全
	}
	
	public static void setCondition(String key ,boolean canAccept) {

	    DHTChordAPI.put(keyCode + AcceptableCode + key, canAccept);
	}
	
	public static AgentClassInfo getAgentInfo(String key) {
		return (AgentClassInfo) DHTChordAPI.get(keyCode + AgentCode + key);
	}
	
	public static void removePcInfo(String key) {
		DHTChordAPI.remove(keyCode + PICode + key);
		DHTChordAPI.remove(keyCode + PIStampCode + key);
	}
	
	public static void removeStaticPCInfo(String key) {
		DHTChordAPI.remove(keyCode + StaticPICode + key);
		DHTChordAPI.remove(keyCode + StaticPIStampCode + key);
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
	

	synchronized public static Set<String> getAllSuvivalIPaddresses() {
		// TODO 自動生成されたメソッド・スタブ
		Set<String> allIPaddresses = new HashSet<>();
		Instant comp = Instant.now().minusSeconds(60);
		for (String key : ((EasySphereNetworkManeger) SingletonS2ContainerFactory.getContainer()
				.getComponent("EasySphereNetworkManeger")).getIPTable()) {
			Instant temp = getPcInfoTimeStamp(key);
			if (temp!=null&&temp.isAfter(comp)) {
				allIPaddresses.add(key);
			}
		}
		return allIPaddresses;
	}
}
