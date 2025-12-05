/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell.command;

import java.util.List;

import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author yamamoto
 */
public class ExecuteAgentCommand extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "execagent";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        //TODO:テスト
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
