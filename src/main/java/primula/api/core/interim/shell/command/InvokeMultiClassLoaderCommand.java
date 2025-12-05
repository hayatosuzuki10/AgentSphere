/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell.command;

import java.util.List;

import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author kousuke
 */
public class InvokeMultiClassLoaderCommand extends AbstractCommand{

    @Override
    public String getCommandName() {
       return "mcl";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        //InstanceCreator ic = new InstanceCreator(environment);
        //ic.selectDirectory();
    }

}



