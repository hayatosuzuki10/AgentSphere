/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.

api.core.interim.shell.command;



import java.util.List;

import primula.api.core.interim.shell.ShellEnvironment;

/**
 *
 * @author sumiya
 */
public class ChangeDirectoryCommand extends AbstractCommand {
    String tmpDirectory;
    @Override
    public String getCommandName() {
        return "cd";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        DirectoryOperator Doperator=new DirectoryOperator();
        tmpDirectory=Doperator.getPath(environment.getDirectory(), args);
        /*
         * セットする
         */
        if (Doperator.exist(tmpDirectory)) {
            environment.setDirectory(tmpDirectory);
        } else {
            System.out.println("Directory Not Found");
        }
    }

}
