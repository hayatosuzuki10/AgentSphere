package primula.api.core.assh.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;

public class callbackup extends AbstractCommand{
	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		System.out.println(fileNames.get(0));//これがファイル名
		ObjectIO oio = new ObjectIO();
		File file = new File(fileNames.get(0));
		FileInputStream fin=null;
		Object obj=null;
		try {
			fin = new FileInputStream(file);
			byte[] binary = new byte[20480];
			fin.read(binary);
			obj = oio.getObject(binary);
			AgentAPI.runAgent((AbstractAgent)obj);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}


}
