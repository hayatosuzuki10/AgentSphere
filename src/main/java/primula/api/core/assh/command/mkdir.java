package primula.api.core.assh.command;

import java.io.File;
import java.util.List;

public class mkdir extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		DirectoryOperator Doperator=new DirectoryOperator();
        while(!fileNames.isEmpty()) {
        	File mkdir = new File(Doperator.getPath(shellEnv.getDirectory(), fileNames.remove(0)));
        	if(mkdir.mkdir())
        		System.out.println("Success Create Directory");
        	else
        		System.out.println("Failure Create Cirectory");
        }
        return data;
    }
}
