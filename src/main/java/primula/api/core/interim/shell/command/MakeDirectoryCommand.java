/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.shell.command;

import java.io.File;
import java.util.List;

import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author sumiya
 */
public class MakeDirectoryCommand extends AbstractCommand {
    String tmpDirectory;
    @Override
    public String getCommandName() {
        return "mkdir";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        DirectoryOperator Doperator=new DirectoryOperator();
        File mkdir = new File(Doperator.getPath(environment.getDirectory(), args));
        if(mkdir.mkdir())
            System.out.println("Success Create Directory");
        else
            System.out.println("Failure Create Cirectory");
    }


}
