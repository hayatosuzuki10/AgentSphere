package primula.api.core.assh.command.interim;

import java.util.List;

import primula.Main;
import primula.api.core.assh.command.AbstractCommand;

public class hublist extends AbstractCommand{

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance,
			List<String> opt) {
		System.out.println(Main.container.toString());
		return null;
	}

}
