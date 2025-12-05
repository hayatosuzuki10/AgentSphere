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
public class DeleteDirectoryCommand extends AbstractCommand {
String tmpDirectory;
    @Override
    public String getCommandName() {
        return "delete";
    }

    @Override
    public void runCommand(ShellEnvironment environment, List<String> args) {
        DirectoryOperator Doperator=new DirectoryOperator();
        File delDirectory = new File(Doperator.getPath(environment.getDirectory(), args));
        if(deleteFile(delDirectory))
            System.out.println("Success Delete Directory");
         else
             System.out.println("Failure Delete Cirectory");
        
    }
    private boolean deleteFile(File dirOrFile) {
        if (dirOrFile.isDirectory()) {//ディレクトリの場合
            String[] children = dirOrFile.list();//ディレクトリにあるすべてのファイルを処理する
            for (int i=0; i<children.length; i++) {
                boolean success = deleteFile(new File(dirOrFile, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // 削除
        return dirOrFile.delete();
    }
}
