/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.assh.command;

// すべてのコマンドはこのAbstractCommandクラスを継承する。
// runCommand()メソッドの引数は
// 		fileNames : ディレクトリやファイル (引数を指定しない場合、fileNamesはnullではなく空である）
// 		instance : エージェントのインスタンス
// 		opt : オプション
// である。
//
// メンバ変数dataはrunCommand()メソッドの結果を返したい場合に用いる。

import java.util.List;

import primula.api.core.assh.ShellEnvironment;

public abstract class AbstractCommand {
	protected ShellEnvironment shellEnv = ShellEnvironment.getInstance();
	protected List<Object> data = null;

    public abstract List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt);
}
