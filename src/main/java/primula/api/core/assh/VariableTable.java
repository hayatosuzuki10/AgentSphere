package primula.api.core.assh;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableTable {
	private static Map<String, List<Object>> variableTable = new HashMap();

	public static void setVariable(String variable, List<Object> data) {
		variableTable.put(variable, data);
	}

	public static boolean containsKey(String variable) {
		return variableTable.containsKey(variable);
	}

	public static Map<String, List<Object>> getVariableMap() {
		return variableTable;
	}

	public static List<Object> getData(String variable) {
		return variableTable.get(variable);
	}

	/* クラスの名前のみ */
	public static Object getOneData(String variable, int index) {
		if(index == -1) return variableTable.get(variable).get(0);
		else return variableTable.get(variable).get(index);
	}
}
