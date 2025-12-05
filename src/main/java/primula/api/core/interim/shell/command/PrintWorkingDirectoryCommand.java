/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.shell.command;

import java.io.File;
import java.util.List;

import primula.api.core.interim.shell.ShellEnvironment;

/**
 * とても意味のないpwdコマンド
 * @author yuichiro
 */
public class PrintWorkingDirectoryCommand extends AbstractCommand{
    
    @Override
    public String getCommandName() {
        return "pwd";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        File file = new File("");
        System.out.println(file.getAbsolutePath());
    }
}
