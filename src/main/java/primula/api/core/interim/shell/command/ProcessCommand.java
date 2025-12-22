/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell.command;

import java.util.HashMap;
import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.agent.AgentInstanceInfo;
import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author yamamoto
 */
public class ProcessCommand extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "ps";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        HashMap<String, List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();
        for (String string : agentInfos.keySet()) {
            System.out.println(string + ":");
            for (AgentInstanceInfo info : agentInfos.get(string)) {
                System.out.println("\t" + info.getAgentName() + ":" + info.getAgentId());
            }
        }
    }
}
