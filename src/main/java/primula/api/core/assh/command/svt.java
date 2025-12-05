package primula.api.core.assh.command;

// ShowVariableTableコマンド
// 使用しているシェル変数とその中身を出力する

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import primula.api.core.assh.VariableTable;

public class svt extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		Map<String, List<Object>> map = VariableTable.getVariableMap();
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry)it.next();
			String key = (String) entry.getKey();
			List<Object> value = (List<Object>) entry.getValue();
			System.out.println(key + " : " + value);
		}
		return null;
	}
}
