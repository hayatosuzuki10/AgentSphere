package primula.api.core.assh.command;

// 指定したクラスまたはインスタンスの持つメソッドの情報を表示するshowMethodメソッドを呼び出す

import java.util.List;

import primula.api.core.assh.operator.MethodOperator;

public class sm extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		MethodOperator.showMethod(fileNames.get(0));
		return data;
	}
}
