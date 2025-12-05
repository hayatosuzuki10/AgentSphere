package primula.api.core.assh.command;

import java.io.File;
import java.util.List;
//ファイルおよびディレクトリの削除コマンド
public class delete extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		DirectoryOperator Doperator=new DirectoryOperator();
        while(!fileNames.isEmpty()) {
        	File delDirectory = new File(Doperator.getPath(shellEnv.getDirectory(), fileNames.remove(0)));
        	if(deleteFile(delDirectory))
        		System.out.println("Success Delete Directory");
        	else
        		System.out.println("Failure Delete Cirectory");
        }
        return data;
    }

    private boolean deleteFile(File dirOrFile) {
        if (dirOrFile.isDirectory()) {
        	String[] children = dirOrFile.list();
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
