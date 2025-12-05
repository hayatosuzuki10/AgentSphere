package primula.api.core.assh.command.interim;

// loadコマンド
// primula.api.core.agent.loader.multiloader.LocalFileClassLoaderでロードさせる。

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.assh.data.AgentClassData;

public class load extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {

	    GhostClassLoader gcl = GhostClassLoader.unique;
	    ChainContainer cc = gcl.getChainContainer();

    	try {
    		cc.resistNewClassLoader(new StringSelector("Agent"), new File("C:\\Users\\selab\\Desktop\\AgentWeb\\Primula_Eclipse\\bin"));
    	} catch (IOException e) {
    		e.printStackTrace();
    	}

    	if(instance != null) {
			if(AgentClassData.containsKey((String) instance.getClass().getSimpleName())) {
				data = new ArrayList();
				ObjectIO oio = new ObjectIO();
				Object obj = null;
				try {
					obj = oio.getObject(AgentClassData.getAgentBinary((String) instance.getClass().getSimpleName()));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				data.add(obj);
				System.out.println(data);
			}
    	}

		return data;
	}

}