package primula.api.core.assh.command;

// ShowFieldValueコマンド
// 指定したクラスまたはインスタンスの持つ変数の中身を表示するshowFieldValueメソッドを呼び出す

import java.util.List;

import primula.api.core.assh.operator.FieldOperator;

public class sfv extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		FieldOperator.showFieldValue(instance);
		return data;
	}
}
