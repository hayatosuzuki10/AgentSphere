/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell.command;

import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.interim.shell.ShellEnvironment;
import primula.api.core.interim.test.MessageTestManagerAgent;

/**
 *
 * @author yamamoto
 */
public class MessageTestCommand extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "msgtest";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        MessageTestManagerAgent agent = new MessageTestManagerAgent();
        AgentAPI.runAgent(agent);
    }
}
