/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell.command;

import java.util.List;

import primula.api.SystemAPI;
import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author sumiya
 */
public class ExitCommand extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "exit";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        environment.setStop();
        SystemAPI.shutdown();
    }
}
