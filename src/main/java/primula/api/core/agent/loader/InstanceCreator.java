/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.agent.loader;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import com.sun.jna.Native;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.loader.MalwareDetection.result;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.assh.ConsolePanel;
import primula.api.core.assh.data.AgentClassData;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import scheduler2022.RecommendedDest;
import scheduler2022.Scheduler;

/**
 *
 * @author kousuke
 */
public class InstanceCreator extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final String classfolder = "target/classes";
	private static final String preclassfolder = "target";
	private static final String postclassfolder = "classes";
	JFileChooser jfc;
	JFrame parent = null;
	File directoryPass;
	GhostClassLoader gcl;
	ChainContainer cc;
	ArrayList<StringSelector> sslist = new ArrayList<StringSelector>();
	String directoryPathName = new File("").getAbsolutePath();
	boolean newProject;//現状は特に意味をなしてない

	public InstanceCreator() {
		System.err.println("Generate agent from \"bin\" folder.");
		//MakeResultJson.console_AgentWeb("Generate agent from \"bin\" folder.");
		ConsolePanel.autoscroll();
		jfc = new JFileChooser(directoryPathName + "/" + classfolder);
		jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		this.newProject = false;
	}

	public InstanceCreator(String Project) {
		System.err.println("Generate agent from \"bin\" folder.");
		//MakeResultJson.console_AgentWeb("Generate agent from \"bin\" folder.");
		/*System.err.println("Generate agent from new project.");
		ConsolePanel.autoscroll();
		// jfc = new JFileChooser(directoryPathName.split(".Primula_Eclipse")[0]);
		//jfc = new JFileChooser("C:\\Users\\okubo\\Desktop\\workspace2");
		//		jfc = new JFileChooser(directoryPathName.split("workspace")[0] + "\\workspace");
		jfc = new JFileChooser(directoryPathName + "\\bin");
		jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);*/
		this.newProject = false;
	}

	public Object selectDirectory() {//mclのみ入力したとき

		Object obj = null;

		gcl = GhostClassLoader.unique;
		cc = gcl.getChainContainer();
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);

		/*
		 * JFileChooserを最前面に出せるように
		 * ①前処理
		 * Hasumi 2016
		 */
		this.parent = new JFrame(); //親フレームを作成
		this.parent.setLocationRelativeTo(null); //親フレームを中心に
		this.parent.setUndecorated(true); //親フレームを透明に
		this.parent.setAlwaysOnTop(true); //親フレームを最前面に
		this.parent.setVisible(true); //親フレームを表示

		if (jfc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			directoryPass = jfc.getSelectedFile();
			System.err.println("Choose " + jfc.getName(directoryPass) + ".");
			//MakeResultJson.console_AgentWeb("Choose " + jfc.getName(directoryPass) + ".");
			obj = this.newProject ? selectNewProject() : selectFromAgentFolder();

		}

		/*
		 * JFileChooserを前面に出せるように
		 * ②後片づけ
		 * Hasumi 2016
		 */
		this.parent.dispose(); //親フレームを削除

		//MalwareDetection s1 = (MalwareDetection) Native.loadLibrary("test4", MalwareDetection.class);
		//result.ByReference ret = new MalwareDetection.result.ByReference();
		int n = 1;
		String s, db;

		if (obj != null) {
			ObjectIO oio = new ObjectIO();
			try {
				byte[] binary = oio.getBinary(obj); // エージェントのクラスのバイナリを取得。
				AgentClassData.setAgentBinary(obj.getClass().getSimpleName(), binary); // エージェント名をキー、バイナリをバリュー。
			} catch (IOException e) {
				e.printStackTrace();
			}

			AbstractAgent aa = (AbstractAgent) obj;
			/*
			//2020/07/23 RanPork
			//FQCNのピリオドを/に変えるごり押し
			s = directoryPathName + "/"+classfolder+"/" + obj.getClass().getName().replace('.', '/') + ".class";
			//s=directoryPathName+"/bin/"+obj.getClass().getSimpleName()+".class";
			db = directoryPathName + "/database";

			System.err.println("Wait a minute! Scanning " + s);
			OS互換性のないコード使っているセキュリティ機能一時無効化
			正しくやりたいなら下のif-elseのコメント外してくださいな
			Ranpork 2020

			n = 0;
			if((n=s1.scanfile(s,db,ret)) < 0){
				System.out.println("error : "+ret.virname);
			}
			else if(n==1){
				System.out.println("found "+s+" : "+ret.virname);
			}
			else{
				System.out.println(s+" "+ret.virname);
			}

			System.err.println("scan finish");
			if (n == 1) {
				System.err.println("This Agent is danger. Do not migrate.");
				return null;
			}
			*/

			/*
			 * 2021卒　高田追記
			 */
			Scheduler sc = new Scheduler();

			if (sc.getCount() > 1 && !RecommendedDest.canExecute(aa)) {
				/*
				 * 他に生きてるのがいるのか
				 * いるけど移動先として使えない時は待機
				 * 使えるなら移動させる
				 */
				String dest = RecommendedDest.recomDest(aa.getAgentName());
				while (dest == IPAddress.myIPAddress) {
					System.err.println("エージェントスケジューラ:起動できるPCがありません。待機しています");

					if (RecommendedDest.canExecute(aa)) {
						System.err.println("エージェントスケジューラ:" + aa.getAgentName() + "起動します");
						AgentAPI.runAgent(aa);
						break;
					}

					dest = RecommendedDest.recomDest(aa.getAgentName());

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
				}
				//					System.out.println(dest);
				if (dest != IPAddress.myIPAddress) {
					System.err.println("エージェントスケジューラ:" + dest + "に移動させます");
					KeyValuePair<InetAddress, Integer> address = null;
					try {
						address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(dest), 55878);
					} catch (UnknownHostException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
					AgentAPI.migration(address, aa);
				}// else {これいる？
				//	System.err.println("エージェントスケジューラ:" + aa.getAgentName() + "通常起動２");
				//	AgentAPI.runAgent(aa);
				//}
			} else {
				System.err.println("エージェントスケジューラ:" + aa.getAgentName() + "通常起動します");
				AgentAPI.runAgent(aa);
			}
			sc.countMinus();

		}

		return obj;
	}

	//@author Nakadaira(2019/12/02)
	public Object selectDirectory(String file) {//引数でclass名の入力したとき
		Object obj = null;

		gcl = GhostClassLoader.unique;
		cc = gcl.getChainContainer();

		directoryPass = new File(file);
		System.err.println("Choose " + directoryPass + ".");
		//MakeResultJson.console_AgentWeb("Choose " + directoryPass + ".");
		obj = this.newProject ? selectNewProject() : selectFromAgentFolder();

		MalwareDetection s1 = (MalwareDetection) Native.loadLibrary("test4", MalwareDetection.class);
		result.ByReference ret = new MalwareDetection.result.ByReference();
		int n = 1;
		String s, db;

		if (obj != null) {
			ObjectIO oio = new ObjectIO();
			try {
				byte[] binary = oio.getBinary(obj); // エージェントのクラスのバイナリを取得。
				AgentClassData.setAgentBinary(obj.getClass().getSimpleName(), binary); // エージェント名をキー、バイナリをバリュー。
			} catch (IOException e) {
				e.printStackTrace();
			}
			AbstractAgent aa = (AbstractAgent) obj;
			

			//2020/07/23 RanPork
			//FQCNのピリオドを\\に変えるごり押し
			s = directoryPathName + "/" + classfolder + "/" + obj.getClass().getName().replace('.', '/') + ".class";
			//s=directoryPathName+"/bin/"+obj.getClass().getSimpleName()+".class";
			db = directoryPathName + "/database";

			if ((n = s1.scanfile(s, db, ret)) < 0) {
				System.out.println("error : " + ret.virname);
			} else if (n == 1) {
				System.out.println("found " + s + " : " + ret.virname);
			} else {
				System.out.println(s + " " + ret.virname);
			}
			System.out.println("scan finish");
			if (n == 1) {
				System.out.println("This Agent is danger. Do not migrate.");
			} else {
				AgentAPI.runAgent(aa);
			}
		}

		return obj;
	}

	/**
	 * @author RanPork
	 * インスタンスから絶対パスを作成するメソッド
	 *
	 * @param inst インスタンス
	 * @return 絶対パス
	 */

	private static String getFullPath(Object inst) {
		return new File(inst.getClass().getResource(inst.getClass().getCanonicalName()).getPath()).getAbsolutePath();
	}

	// ******************************************** 別プロジェクトに存在するエージェントを選択 *************************************************
	private Object selectNewProject() {
		System.err.println(directoryPass + " ********** execute this project.");

		Object obj = null;
		Class<?> cls = null;

		try {
			cc.resistNewClassLoader(new StringSelector(getAgentFolderPath(directoryPass.getCanonicalPath())),
					new File(getAgentFolderPath(directoryPass.getCanonicalPath())));
		} catch (IOException ex) {
			Logger.getLogger(InstanceCreator.class.getName()).log(Level.SEVERE, null, ex);
		}

		try {
			cls = gcl.loadClass(getAbsoluteAgentName(directoryPass.getCanonicalPath()).split(".class")[0]);
			obj = cls.newInstance();
			System.err.println(cls.getClassLoader() + " ********** use this classloader");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		}

		return obj;
	}

	// binフォルダまでの絶対パスを取得
	// 例.
	// 引数 :
	// C:\Users\okubo\Desktop\workspace\AgentSphereTest\bin\main\TestAgent.class
	// 戻り値 : C:\Users\okubo\Desktop\workspace\AgentSphereTest\bin
	private String getAgentFolderPath(String prePath) {

		String path = null;

		path = prePath.split(classfolder + "/")[0] + classfolder;

		return path;
	}

	// binフォルダ以下からAgentの名前までのパスを抽出
	// 例.
	// 引数 :
	// C:\Users\okubo\Desktop\workspace\AgentSphereTest\bin\main\TestAgent.class
	// 戻り値 : main.TestAgent.class
	private String getAbsoluteAgentName(String prePath) {

		boolean flag = false;
		String[] pathArray = null;
		String absoluteAgentName = "";

		pathArray = prePath.split("/");
		for (int i = 0; i < pathArray.length; i++) {
			if (i == pathArray.length - 1 && flag)
				absoluteAgentName += pathArray[i];
			else if (flag)
				absoluteAgentName += pathArray[i] + ".";
			else if (pathArray[i].equals(postclassfolder))
				flag = true;
		}

		return absoluteAgentName;
	}

	// ********************************************************************************************************************************************

	// ***************************** Primula_Eclipse内のデフォルトパッケージで作成したエージェントを選択 ****************************************
	private Object selectFromAgentFolder() {

		Object obj = null;
		Class<?> cls = null;

		try {
			System.err.println(directoryPathName);
			cc.resistNewClassLoader(new StringSelector(directoryPathName + "/" + classfolder),
					new File(directoryPathName + "/" + classfolder));
		} catch (IOException ex) {
			Logger.getLogger(InstanceCreator.class.getName()).log(Level.SEVERE, null, ex);
		}

		try {
			//System.err.println("!!!!!!"+getFQCN(directoryPass.getCanonicalPath()).split(".class")[0]);
			cls = gcl.loadClass(getFQCN(directoryPass.getCanonicalPath()).split(".class")[0]);
			obj = cls.newInstance();
			System.err.println(cls.getClassLoader() + " ********** use this classloader");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		}

		return obj;
	}

	// Agentの名前だけを抽出
	// 例.
	// 引数 :
	// C:\Users\okubo\Desktop\workspace\Primula_Eclipse\agent\TestAgent.class
	// 戻り値 : TestAgent.class
	private String reconstractPath(String prePath) {

		String[] pathArray = null;
		String agentSimpleName = null;

		pathArray = prePath.split("/");
		agentSimpleName = pathArray[pathArray.length - 1];

		return agentSimpleName;
	}

	/**
	 * 渡された絶対パスからFQCNを推測して返します
	 * <p>
	 * 現状ではbin以降のディレクトリ名を使って生成します
	 * ぶっちゃけgetAbsoluteAgentNameと同じ
	 * @param prePath クラスを表すパス
	 * @return 指定したクラスのFQCN
	 * @author Ranpork
	 */
	private String getFQCN(String prePath) {
		System.out.println(prePath);
		boolean flag = false;
		String[] pathArray = null;
		String absoluteAgentName = "";

		pathArray = prePath.split("[/\\\\]");
		for (int i = 0; i < pathArray.length; i++) {
			if (i == pathArray.length - 1 && flag)
				absoluteAgentName += pathArray[i];
			else if (flag)
				absoluteAgentName += pathArray[i] + ".";
			else if (pathArray[i].equals(postclassfolder))
				flag = true;
		}

		return absoluteAgentName;
	}
	// ********************************************************************************************************************************************

}
