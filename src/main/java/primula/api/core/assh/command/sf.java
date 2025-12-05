package primula.api.core.assh.command;

// 指定したクラスまたはインスタンスの持つフィールドの情報を表示するshowFieldメソッドを呼び出す

import java.util.List;

import primula.api.core.assh.operator.FieldOperator;

public class sf extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		FieldOperator.showField(instance);
		return data;
	}
}
