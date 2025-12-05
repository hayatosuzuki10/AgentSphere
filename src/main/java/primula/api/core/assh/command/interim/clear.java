package primula.api.core.assh.command.interim;

import java.util.List;

import primula.api.core.assh.command.AbstractCommand;

public class clear extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		for(int i=0; i<30; i++){
			System.out.println();
		}
		return null;
	}
}
