package primula.agent.util;

import java.net.InetAddress;
import java.util.regex.Pattern;

import primula.api.DHTChordAPI;

/**
 * Agentが移動した情報をDHTに登録するための便利クラス<br>
 * DHT全体でkeyの衝突を避けるために引数で渡されたkeyとは別に識別符号を付与してDHTにputしたりgetしたりする
 *
 */
public class DHTutil {
	private static final String keyCode = "2022Agent:";
	private static final String IPCode = "IP:";

	public static void setAgentIP(String strictName, InetAddress addr) {
		if(!checkUUID(strictName))throw new IllegalArgumentException("StrictNameがUUIDではありません");
		DHTChordAPI.put(keyCode + IPCode + strictName, addr);
	}

	public static InetAddress getAgentIP(String strictName) {
		if(!checkUUID(strictName))throw new IllegalArgumentException("StrictNameがUUIDではありません");
		return (InetAddress) DHTChordAPI.get(keyCode + IPCode + strictName);
	}

	public static boolean containsAgentIP(String strictName) {
		if(!checkUUID(strictName))throw new IllegalArgumentException("StrictNameがUUIDではありません");
		return DHTChordAPI.contains(keyCode + IPCode + strictName);
	}

	public static void removeAgentIP(String strictName) {
		if(!checkUUID(strictName))throw new IllegalArgumentException("StrictNameがUUIDではありません");
		DHTChordAPI.remove(keyCode+IPCode+strictName);
	}
	
	private static boolean checkUUID(String str) {
		String regex = "[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}";
		Pattern p = Pattern.compile(regex);
		return p.matcher(str).find();
	}


}