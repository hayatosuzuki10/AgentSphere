/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell.command;

import java.util.List;

import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author sumiya
 */
public abstract class AbstractCommand {

    public abstract String getCommandName();

    public abstract void runCommand(ShellEnvironment environment, List<String> args);
}
