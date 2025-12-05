package primula.api.core.assh.data;

import java.util.HashMap;
import java.util.Map;

public class AgentClassData {
	private static Map<String, byte[]> agentBinary = new HashMap();
	private static Map<String, Map<String, Object>> localAreaVariable = new HashMap();

	public static void setAgentBinary(String agentName, byte[] binary) {
		agentBinary.put(agentName, binary);
	}

	public static boolean containsKey(String agentName) {
		return agentBinary.containsKey(agentName);
	}

	public static byte[] getAgentBinary(String agentName) {
		return agentBinary.get(agentName);
	}
}
