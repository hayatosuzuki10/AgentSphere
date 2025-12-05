/**
 * @author Mikamiyama
 */

//ファイルからエージェントを読み込んで起動させるためのテストエージェント。

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.loader.InstanceCreator;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.assh.data.AgentClassData;

public class RunTestAgent extends AbstractAgent{
	public void run(){
		ObjectIO oio=new ObjectIO();
		Object obj=null;

		obj=selectFromAgentFolder();

		try{
			byte[] binary = oio.getBinary(obj); // エージェントのクラスのバイナリを取得。
			AgentClassData.setAgentBinary(obj.getClass().getSimpleName(), binary); // エージェント名をキー、バイナリをバリュー。
		}catch(IOException e){
			e.printStackTrace();
		}

		AbstractAgent agent=(AbstractAgent)obj;
		AgentAPI.runAgent(agent);
	}

	private Object selectFromAgentFolder(){
		Object obj=null;
		Class<?> cls=null;
		ChainContainer cc;
		GhostClassLoader gcl;
		String directoryPathName=new File("").getAbsolutePath();
		File file=new File("C:\\Users\\selab\\Desktop\\Agent Sphere\\Primula_Eclipse\\log\\run.txt");     //読み込むファイルのパス
		String s=new String();
		BufferedReader br;

		try{
			br=new BufferedReader(new FileReader(file));

			s=br.readLine();     //読み込み

			br.close();
		}catch(FileNotFoundException e){
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}catch(IOException e){
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		gcl = GhostClassLoader.unique;
		cc = gcl.getChainContainer();

		try{
			cc.resistNewClassLoader(new StringSelector(directoryPathName+"\\bin"),new File(directoryPathName+"\\bin"));
		}catch(IOException e){
			Logger.getLogger(InstanceCreator.class.getName()).log(Level.SEVERE,null,e);
		}
		try{
			cls=gcl.loadClass(s.split(".class")[0]);     //開くクラスファイルの指定
			obj=cls.newInstance();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(InstantiationException e){
			e.printStackTrace();
		}catch(IllegalAccessException e){
			e.printStackTrace();
		}

		return obj;
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}