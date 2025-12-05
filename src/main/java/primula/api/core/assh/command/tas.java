package primula.api.core.assh.command;

import java.util.List;

import primula.api.core.assh.MainPanel;

public class tas extends AbstractCommand{

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		// TODO 自動生成されたメソッド・スタブ
		if(MainPanel.scroll_flg) {
			System.out.println("TerminalオートスクロールOFFにしました");
			MainPanel.scroll_flg=false;
		}else {
			System.out.println("TerminalオートスクロールをONにしました");
			MainPanel.scroll_flg=true;
		}
		return data;
	}

}
