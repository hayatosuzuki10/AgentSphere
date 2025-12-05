package primula.api.core.assh.command;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import primula.api.core.agent.loader.InstanceCreator;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.util.KeyValuePair;

//nakadaira 変更(2019)

/*
 * mcl
 * エージェントの新規生成をするプログラム
 * または
 * AgentWebからの操作でほかのPCで新規エージェントの生成
 *
 * eclipseを通して、ほかのPCでエージェントを起動する際には、mexeコマンドを使用する
 */

public class mcl extends AbstractCommand {

	private static KeyValuePair<InetAddress, Integer> ToSlave;

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {

		System.out.println(fileNames);
//		if (!destination.ShellIP.isEmpty()) {//ほかのPCでのエージェント生成
//			//AgentWebからの操作の時に動作
//			for (int i = 0; i < fileNames.size(); i++) {
//				Object obj = selectFromAgentFolder(fileNames.get(i));
//				AbstractAgent a = (AbstractAgent) obj;
//				try {
//					ToSlave = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(destination.ShellIP),
//							55878);
//				} catch (UnknownHostException e) {
//					// TODO 自動生成された catch ブロック
//					e.printStackTrace();
//				}
//				AgentAPI.migration(ToSlave, a);
//
//
//				//a.requestStop(); //このAgentSphereの該当のエージェントを停止させる
//			}
//			if(instance!=null) {
//				Object obj = selectFromAgentFolder((String)instance);
//				AbstractAgent a = (AbstractAgent) obj;
//				AgentAPI.migration(ToSlave, a);
//			}
//			destination.setShellIP("");
//		} else {//自分のPCで新規エージェントを生成する場合
			if (instance == null && fileNames.isEmpty()) {
				InstanceCreator ic = new InstanceCreator();
				data = new ArrayList<Object>();
				data.add(ic.selectDirectory());//ウィンドウが表示されて、フォルダ等が表示される
			} else if (instance == null && !(fileNames.isEmpty())) {//引数でclass名の入力したとき
				//data = new ArrayList<Object>();
				//data.add(ic.selectDirectory());
				data = new ArrayList<Object>();
				for (int i = 0; i < fileNames.size(); i++) {
					InstanceCreator ic = new InstanceCreator(fileNames.get(0));
					data.add(ic.selectDirectory(fileNames.get(i)));
				}

			} else {//？

				data = new ArrayList<Object>();
				//for (int i = 0; i < fileNames.size(); i++) {
				InstanceCreator ic = new InstanceCreator((String) instance);
				data.add(ic.selectDirectory((String) instance));
				//}
				if (!fileNames.isEmpty()) {
					for (int i = 0; i < fileNames.size(); i++) {
						ic = new InstanceCreator(fileNames.get(i));
						data.add(ic.selectDirectory(fileNames.get(i)));
					}
				}
			}
		//}
		//}
		return data;
	}

	private Object selectFromAgentFolder(String agentName) {
		Object obj = null;
		Class<?> cls = null;
		ChainContainer cc;
		GhostClassLoader gcl;
		String path = ".\\bin";

		gcl = GhostClassLoader.unique;
		cc = gcl.getChainContainer();

		try {
			cc.resistNewClassLoader(new StringSelector(path), new File(path));
		} catch (IOException e) {
			Logger.getLogger(InstanceCreator.class.getName()).log(Level.TRACE, null, e);
		}

		try {
			cls = gcl.loadClass(agentName.split(".class")[0]); //開くファイルの指定
			obj = cls.newInstance();
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return obj;
	}

}


//以下一応うまくいったやつ消しても良し
//if (AgentClassData.containsKey((String) instance)) {
//ObjectIO oio = new ObjectIO();
//Object obj = null;
//try {
//obj = oio.getObject(AgentClassData.getAgentBinary((String) instance));
//} catch (IOException e) {
//e.printStackTrace();
//} catch (ClassNotFoundException e) {
//e.printStackTrace();
//}
//Object obj=null;

//以下、一応うまくいった例
//					Class<?> cls=null;
//					ChainContainer cc;
//					GhostClassLoader gcl;
//					String path=".\\bin";
//
//
//
//					gcl=GhostClassLoader.unique;
//					cc=gcl.getChainContainer();
//
//
//					try {
//						cc.resistNewClassLoader(new StringSelector(path),new File(path));
//					} catch(IOException e){
//						Logger.getLogger(InstanceCreator.class.getName()).log(Level.TRACE,null,e);
//					}
//
//					try{
//						cls=gcl.loadClass((String) instance);     //開くファイルの指定
//						obj=cls.newInstance();
//					}catch(ClassNotFoundException e){
//						// TODO 自動生成された catch ブロック
//						e.printStackTrace();
//					}catch(InstantiationException e){
//						// TODO 自動生成された catch ブロック
//						e.printStackTrace();
//					}catch(IllegalAccessException e){
//						// TODO 自動生成された catch ブロック
//						e.printStackTrace();
//					}
//				AbstractAgent agent = (AbstractAgent) obj;
//				AgentAPI.runAgent(agent);
