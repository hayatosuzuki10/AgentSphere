package primula.api.core.assh.command.interim;

import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.assh.command.AbstractCommand;

public class un extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
        GhostClassLoader gcl = GhostClassLoader.unique;

        Class<?> cls = null;
		try {
			cls = gcl.loadClass("UnknownAgent3");
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		AbstractAgent agent = null;
		try {
			agent = (AbstractAgent) cls.newInstance();
		} catch (InstantiationException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		AgentAPI.runAgent(agent);
		return null;
	}
}
