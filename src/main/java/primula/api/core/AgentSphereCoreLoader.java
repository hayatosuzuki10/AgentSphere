package primula.api.core;

import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import oshi.util.GlobalConfig;
import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.SystemAPI;
import primula.api.core.agent.AgentManager;
import primula.api.core.assh.ConsolePanel;
import primula.api.core.resource.SystemResource;
import primula.api.core.scheduler.cpu.CPUPerformaceMeasure;
import primula.util.IPAddress;
import scheduler2022.Scheduler;

/**
 * @author yamamoto
 */
public class AgentSphereCoreLoader {
	public static AgentManager manager;

	public AgentSphereCoreLoader() {
	}

	/**
	 * @author yamamoto
	 */
	public synchronized void start() {

		ConsolePanel cp = new ConsolePanel(); // コンソール画面の設定
		cp.setVisible(true); // コンソール画面の表示

		CPUPerformaceMeasure.measureFirstCPUperformance(); //システム起動時のマシン測定(出力結果:起動時間　測り始め　測り終わり)
		System.err.println(CPUPerformaceMeasure.getOpenningIncrement()); //マシンの性能値の表示
		GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LOADAVERAGE, true); // Windows環境で load average（ロードアベレージ）を有効にする」設定を、プログラム実行中にオンにしている　GlobalConfig:OSHIの設定保存用クラス
		System.setProperty("java.rmi.server.hostname", IPAddress.myIPAddress); // Java RMI（Remote Method Invocation） を使うときに、通信に使うホスト名（またはIPアドレス）を手動で指定する
		initializeAgentSphere();//Agentの読み込み等を行う。(主にtestServerSideAgent等といったAgentSphereの機能Agentの起動)

		try {
			wait();
		} catch (InterruptedException ex) { // 割り込みが入った場合エラーとして扱う
			SystemAPI.getLogger().fatal("致命的なエラーが発生しました\r\n" + ex);
			System.exit(1);
		}

		finalizeAgentSphere(); // 終了処理

	}

	public synchronized void shutdown() {
		notifyAll();
	}

	/**
	 * @author yamamoto
	 */
	private void initializeAgentSphere() {
		//特殊な初期化
		SystemResource.initialize(this); // AgentSphereCoreLoderを他からもアクセスできるように保存

		SystemAPI.getLogger().trace("モジュールの初期化を開始します。"); // ログに残す

		// モジュール初期化　モジュールの共通の動きをSingletonS2ContainerFactoryで設定
		SingletonS2ContainerFactory.setConfigPath("./setting/AgentSphere.dicon"); // diconファイルの中のモジュールを生成
		SingletonS2ContainerFactory.init(); // モジュールを初期化（インスタンス化）
		S2Container coreModuleContainer = SingletonS2ContainerFactory.getContainer(); // 登録したモジュール（from diconファイル）を取得
		for (int i = 0; i < coreModuleContainer.getComponentDefSize(); i++) { // モジュールごとにfor文
			Object o = coreModuleContainer.getComponentDef(i).getComponent(); 
			((ICoreModule) o).initializeCoreModele(); // モジュールを初期化
			primula.api.core.assh.data.ModuleList.addModule(o); // 他でも呼び出せるようにリストに保存
		}

		SystemAPI.getLogger().trace("Startup Agentの読み込みを開始します。"); // ログに残す

		// 最初に起動するエージェントの読み込み
		S2Container factory = S2ContainerFactory.create("./setting/StartupAgent.dicon");//diconの中身が読み込まれ、Agentとして起動される

		for (int i = 0; i < factory.getComponentDefSize(); i++) {
			Object agent = factory.getComponentDef(i).getComponent();
			AgentAPI.runAgent((AbstractAgent) agent);//ここでAgentの起動を行う
			SystemAPI.getLogger().trace(agent.getClass() + "が読み込まれました。");
			//MakeResultJson.output_AgentWeb(agent.getClass() + "が読み込まれました。");
			//System.err.println(agent.getClass() + "が読み込まれました。");
		}
	 
	
	       
		Scheduler sch = new Scheduler();
		Thread th = new Thread(sch);
		th.setName("AgentSchedule2022");
		th.start();
		System.out.println("スケジューラ起動！！");

		/*
		GhostClassLoader gcl = GhostClassLoader.unique;
		ChainContainer cc = gcl.getChainContainer();
		    	try {
		    		cc.resistNewClassLoader(new StringSelector("Agent"),new File("C:\\Users\\okubo\\Desktop\\workspace\\Primula_Eclipse\\agent"));
		    	} catch (IOException ex) {
		    		Logger.getLogger(InstanceCreator.class.getName()).log(Level.SEVERE, null, ex);
		    	}
		Class<?> cls = null;
				try {
					cls = gcl.loadClass("UnknownAgent3");
				} catch (ClassNotFoundException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}

				AbstractAgent agent = null;
				try {
					agent = (AbstractAgent) cls.newInstance();
				} catch (InstantiationException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
				AgentAPI.runAgent(agent);
		*/
	}

	/**
	 * @author yamamoto
	 */
	private void finalizeAgentSphere() {
		SystemAPI.getLogger().trace("モジュールの終了処理を開始します。"); // ログに残す
		S2Container coreModuleContainer = SingletonS2ContainerFactory.getContainer(); 
		for (int i = 0; i < coreModuleContainer.getComponentDefSize(); i++) { // モジュールごとにfor文
			Object o = coreModuleContainer.getComponentDef(i).getComponent();
			((ICoreModule) o).finalizeCoreModule(); //モジュールを終了
		}
	}
}
