package primula.api.core.assh;

import java.util.HashMap;
import java.util.Map;

public class ClassTable {
	private static Map<String, Class> classTable = new HashMap();

	public ClassTable() {
		try {
			classTable.put("Byte", Byte.class);
			classTable.put("Short", Short.class);
			classTable.put("Integer", Integer.class);
			classTable.put("Long", Long.class);
			classTable.put("Float", Float.class);
			classTable.put("Double", Double.class);
			classTable.put("Character", Character.class);
			classTable.put("String", String.class);
			classTable.put("Math", Math.class);
			classTable.put("System", System.class);
			classTable.put("Thread", Thread.class);
			classTable.put("Calendar", Class.forName("java.util.Calendar"));
			classTable.put("Inet4Address", Class.forName("java.net.Inet4Address"));
			classTable.put("IPAddress", Class.forName("primula.util.IPAddress"));
			//classTable.put("ModuleList", Class.forName("primula.api.core.assh.data.ModuleList"));
			classTable.put("AgentAPI", Class.forName("primula.api.AgentAPI"));
			classTable.put("MessageAPI", Class.forName("primula.api.MessageAPI"));
			classTable.put("NetworkAPI", Class.forName("primula.api.NetworkAPI"));
			classTable.put("SystemAPI", Class.forName("primula.api.SystemAPI"));
			classTable.put("GhostClassLoader", Class.forName("primula.api.core.agent.loader.multiloader.GhostClassLoader"));
			classTable.put("AgentClassData", Class.forName("primula.api.core.assh.data.AgentClassData"));
			classTable.put("SuccessorList", Class.forName("primula.api.core.network.dhtmodule.routing.impl.SuccessorList"));
			classTable.put("SocketProxy", Class.forName("primula.api.core.network.dhtmodule.nodefunction.impl.SocketProxy"));

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void add(String className) {
		String simpleClassName = null;
		for(String s : className.split("\\.")){
			simpleClassName = s;
		}
		if(!classTable.containsKey(simpleClassName)) {
			try {
				classTable.put(simpleClassName, Class.forName(className));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public static Class getClass(String simpleClassName) {
		return classTable.get(simpleClassName);
	}

	public static boolean isClass(String simpleClassName) {
		return classTable.containsKey(simpleClassName);
	}
}
