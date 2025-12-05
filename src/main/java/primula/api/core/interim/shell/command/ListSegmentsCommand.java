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
public class ListSegmentsCommand extends AbstractCommand {

    @Override
    public String getCommandName() {
        return "ls";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        DirectoryOperator Doperator=new DirectoryOperator();
        File cdirectory = new File(Doperator.getPath(environment.getDirectory(), args));
    File filelist[] = cdirectory.listFiles();

    for (int i = 0 ; i < filelist.length ; i++){
      if (filelist[i].isFile()){
        System.out.println("[F]" + filelist[i].getName());
      }else if (filelist[i].isDirectory()){
        System.out.println("[D]" + filelist[i].getName());
      }else{
        System.out.println("[?]" + filelist[i].getName());
      }
    }
    }
}
