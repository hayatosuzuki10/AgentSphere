/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.shell.command;

import java.util.ArrayList;
import java.util.List;

import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author sumiya
 */
public class EchoCommand extends AbstractCommand{

    @Override
    public String getCommandName() {
        return "echo";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        ArrayList<String> arg = new ArrayList<String>();
        for (String s : args) {
            System.out.println(s);
        }
    }

}
