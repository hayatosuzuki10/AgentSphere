/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell.command;

import java.util.List;
import primula.api.AgentAPI;
import primula.api.core.interim.application.DesktopAgent;
import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author kurosaki
 */
public class ApplicationTestCommand extends AbstractCommand{

    @Override
    public String getCommandName() {
        return "ap";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        DesktopAgent agent = new DesktopAgent();
        AgentAPI.runAgent(agent);
    }
    
}
