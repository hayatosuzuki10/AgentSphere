package primula.api.core.assh.command.interim;

import java.util.ArrayList;
import java.util.List;

import primula.api.core.assh.command.AbstractCommand;

public class echo extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {

        ArrayList<String> arg = new ArrayList<String>();
        for (String s : fileNames) {
            System.out.println(s);
        }
        return data;
    }
}
