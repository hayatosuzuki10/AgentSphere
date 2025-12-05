/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell.command;

import java.net.InetAddress;
import java.util.List;

import primula.api.NetworkAPI;
import primula.api.core.interim.shell.ShellEnvironment;
import primula.util.KeyValuePair;

/**
 *
 * @author yamamoto
 */
public class ShowAgentSphereCommand extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "node";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        for (KeyValuePair<String, KeyValuePair<InetAddress, Integer>> pair : NetworkAPI.getAddresses()) {
            System.out.println(pair.getKey() + "/" + pair.getValue().getKey() + ":" + pair.getValue().getValue());
        }
    }
}
