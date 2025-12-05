package primula.api.core.assh.command;

import java.io.File;
import java.util.List;

import primula.api.core.assh.data.lsData;

public class ls extends AbstractCommand {

	private lsData lsd = new lsData();

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
        DirectoryOperator Doperator=new DirectoryOperator();
        File cdirectory = new File(Doperator.getPath(shellEnv.getDirectory(), (String)null));
        File filelist[] = cdirectory.listFiles();
        boolean dollar = true;
        //MakeResultJson.console_AgentWeb("lsコマンドを実行");
        System.err.println("lsコマンドを実行");
        for (int i = 0 ; i < filelist.length ; i++){
        	if (filelist[i].isFile()){
        		System.out.println("[F]" + filelist[i].getName());
        		//MakeResultJson.output_AgentWeb("[F]" + filelist[i].getName());
        		if(dollar) lsd.setData("[F]" + filelist[i].getName());
        	}else if (filelist[i].isDirectory()){
        		System.out.println("[D]" + filelist[i].getName());
        		//MakeResultJson.output_AgentWeb("[D]" + filelist[i].getName());
        		if(dollar) lsd.setData("[D]" + filelist[i].getName());
        	}else{
        		System.out.println("[?]" + filelist[i].getName());
        		//MakeResultJson.output_AgentWeb("[?]" + filelist[i].getName());
        		if(dollar) lsd.setData("[?]" + filelist[i].getName());
        	}
        }
        if(dollar) data = lsd.getData();
        return data;
	}
}
