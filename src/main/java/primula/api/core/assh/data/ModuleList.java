package primula.api.core.assh.data;

import java.util.HashMap;
import java.util.Map;

public class ModuleList {
	private static Map<String, Object> moduleList = new HashMap();

	public ModuleList() {
	}

	public static void addModule(Object module) {
		moduleList.put(module.getClass().getSimpleName(), module);
	}

	public static Object getModule(String moduleName) {
		return moduleList.get(moduleName);
	}
}
