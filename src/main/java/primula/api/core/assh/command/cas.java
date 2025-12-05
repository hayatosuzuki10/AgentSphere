package primula.api.core.assh.command;

import java.util.List;

import primula.api.core.assh.ConsolePanel;

public class cas extends AbstractCommand{

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		// TODO 自動生成されたメソッド・スタブ
		if(ConsolePanel.scroll_flg) {
			System.out.println("ConsoleオートスクロールOFFにしました");
			ConsolePanel.scroll_flg=false;
		}else {
			System.out.println("ConsoleオートスクロールをONにしました");
			ConsolePanel.scroll_flg=true;
		}
		return data;
	}

}
