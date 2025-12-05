package primula.api.core.assh.command.interim;

import java.io.File;
import java.io.IOException;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.assh.command.AbstractCommand;

public class osero extends AbstractCommand {

	public static AbstractAgent agent;

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
        GhostClassLoader gcl = GhostClassLoader.unique;
        ChainContainer cc = gcl.getChainContainer();

        try {
//			cc.resistNewClassLoader(new StringSelector("UnknownAgent"),new File("C:\\Users\\okubo\\Desktop\\workspace\\Primula_Eclipse\\bin\\primula\\api\\core\\interim\\testagent"));
			cc.resistNewClassLoader(new StringSelector("UnknownAgent"),new File("C:\\Users\\okubo\\Desktop\\workspace\\Primula_Eclipse\\agent"));
//			cc.resistNewClassLoader(new StringSelector("UnknownAgent"),new File("C:\\3"));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		Class<?> cls = null;
		try {
			cls = gcl.loadClass("OthelloAgent");
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		System.err.println(cls.getClassLoader() + " ************ un#cls.getClassLoader()");////////////////////////

//		AbstractAgent agent = null;
		try {
			agent = (AbstractAgent) cls.newInstance();
		} catch (InstantiationException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}


		//AbstractAgent agent = OthelloAgent.getInstance();


//		UnknownAgent agent = new UnknownAgent();
		AgentAPI.runAgent(agent);
		return null;
	}
}
