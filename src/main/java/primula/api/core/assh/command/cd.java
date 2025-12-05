package primula.api.core.assh.command;

import java.util.List;

public class cd extends AbstractCommand {

	private String tmpDirectory;

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		DirectoryOperator Doperator = new DirectoryOperator();
        tmpDirectory = Doperator.getPath(shellEnv.getDirectory(), fileNames.get(0));

        if (Doperator.exist(tmpDirectory)) {
        	shellEnv.setDirectory(tmpDirectory);
        } else {
        	System.out.println("Directory Not Found");
        }
        return data;
    }
}
