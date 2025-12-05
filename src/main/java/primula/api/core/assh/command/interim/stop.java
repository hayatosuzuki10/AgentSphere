package primula.api.core.assh.command.interim;

// stopコマンド
// 指定したエージェントの動きを一時停止させる。

import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.assh.command.AbstractCommand;

public class stop extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		for (int i = 0; i < fileNames.size(); i++) {
			System.out.println("stopします");
			Object object=fileNames.get(i);
			AgentAPI.requestStop((AbstractAgent) object);
			System.out.println("stopしました");
		}
		return null;
	}

}
