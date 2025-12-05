package primula.api.core.assh.command.interim;

import java.io.File;
import java.io.IOException;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.assh.data.AgentClassData;

public class loA extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
        GhostClassLoader gcl = GhostClassLoader.unique;
        ChainContainer cc = gcl.getChainContainer();

        if((fileNames == null || fileNames.isEmpty()) && instance == null) {
        	try {
        		cc.resistNewClassLoader(new StringSelector("Agent"),new File("C:\\Users\\okubo\\Desktop\\workspace\\Primula_Eclipse\\agent"));
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
        else if(instance != null) {

        	Class<?> cls = null;
        	try {
        		cls = gcl.loadClass("LoadedAgent");
        	} catch (ClassNotFoundException e) {
        		e.printStackTrace();
        	}
        	System.err.println(cls.getClassLoader() + " ************ un#cls.getClassLoader()");////////////////////////

        	AbstractAgent agent = null;
        	try {
        		agent = (AbstractAgent) cls.newInstance();
        	} catch (InstantiationException e) {
        		e.printStackTrace();
        	} catch (IllegalAccessException e) {
        		e.printStackTrace();
        	}
        	AgentAPI.runAgent(agent);

        	ObjectIO oio = new ObjectIO();
        	try {
				byte[] binary = oio.getBinary(agent);
				AgentClassData.setAgentBinary("LoadedAgent", binary);
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}

/*
    		JFileChooser fileChooser = new JFileChooser(".");
    		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    		int result = fileChooser.showSaveDialog(null);

    		if(result != JFileChooser.APPROVE_OPTION) return null;

    		File objectFile = fileChooser.getSelectedFile();

    		try{
    			Thread.currentThread().sleep(50);
    			objectFile.createNewFile();
    			ObjectIO oio = new ObjectIO();
    			byte[] binary = oio.getBinary(agent);
    			FileOutputStream fos = new FileOutputStream(objectFile);
    			fos.write(binary);
    			fos.close();
    		}catch(FileNotFoundException e1){
    			// TODO 自動生成された catch ブロック
    			e1.printStackTrace();
    		}catch(IOException e1){
    			// TODO 自動生成された catch ブロック
    			e1.printStackTrace();
    		}catch(InterruptedException r1){
    			// TODO 自動生成された catch ブロック
    			r1.printStackTrace();
    		}
*/
        }
        else {
        	ObjectIO oio = new ObjectIO();
        	Object obj = null;
        	try {
				obj = oio.getObject(AgentClassData.getAgentBinary("LoadedAgent"));
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			AbstractAgent agent = (AbstractAgent) obj;
			AgentAPI.runAgent(agent);

/*
    		JFileChooser fileChooser = new JFileChooser(".");
    		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    		int result = fileChooser.showOpenDialog(null);

    		if(result != JFileChooser.APPROVE_OPTION) return null;

    		File objectFile = fileChooser.getSelectedFile();
    		ObjectIO oio = new ObjectIO();
    		Object obj = null;
    		try{
    			obj = oio.getObject(objectFile);
    		}catch(IOException e2){
    			e2.printStackTrace();
    		}catch(ClassNotFoundException e2){
    			e2.printStackTrace();
    		}
    		AbstractAgent agent = (AbstractAgent) obj;
    		AgentAPI.runAgent(agent);
   */
        }



//		UnknownAgent agent = new UnknownAgent();
		return null;
	}

}
