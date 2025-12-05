package primula.agent;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

import primula.api.AgentAPI;
import primula.api.core.agent.AgentInfo;
import primula.api.core.resource.SystemResource;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import scheduler2022.ClockSpeed;
import scheduler2022.DynamicPCInfo;
import scheduler2022.MemoryMeasure;
import scheduler2022.Scheduler;
import scheduler2022.util.DHTutil;
import sphereConnection.stub.SphereSpec;

public abstract class AbstractAgent extends SystemResource
		implements Serializable, Runnable, Callable<ContinuationData> {
	class history {
		String ip;
		long time;

		history(String ip, long time) {
			this.ip = ip;
			this.time = time;
		}

	}
	private AgentInfo info;
	
	private long threadId = -1; // ★ 実行中スレッドID保持用

    public long getThreadId() {
        return threadId;
    }

	/* Agent内部データ */
	private String agentID;
	private InetAddress myIP;
	private long time = System.currentTimeMillis(); //起動した時間
	private List<history> his = new ArrayList<history>();

	/* JavaFlow用データ */
	public Continuation Javaflow = null;
	private boolean ContinuationFlag = false;
	/**
	 * 行先
	 */
	private InetAddress address = null;

	/* マルチスレッド動作用フラグ */
	protected boolean multiFlag = false;
	private boolean checkFlag = false;
	private boolean endFlag = false;
	boolean migrateFlag = false;
	boolean callMethod_startFlag = false;

	/* バックアップ用変数 */
	protected int BACKUPNUM = 0;
	private boolean backupFlag = false;

	/*　スケジューラ用　2021年学部卒高田追加
	 *
	 */
	transient long ed;
	transient long clock;
	transient long usingmemory;
	transient long processingtime;
	public boolean forcedmove = true;
    public boolean shouldMove = false;
    public String nextDestination;
    
    private double LearningRate = 0.5;
    public String charactaristicsTemplete = "Balanced";
    public DynamicPCInfo DPIBeforeChange;
    public DynamicPCInfo DPIAfterChange;
    public DynamicPCInfo DPIChange;
    
    
    public double priority = 0.5;
    
    public double progress = -1;
    public long migrateTime = 0L;
    public long previousMigrateTime = 0L;
    


	public long getElapsedTime() {
		long temp = System.currentTimeMillis();
		return temp - time;
	}

	public void forcedMove(String ad) {
		try {
			address = InetAddress.getByName(ad);
		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		forcedmove = true;
	}

	/**
	 * エージェントのコンストラクタ
	 * オーバーロードする場合は必ずsuper()で加えること
	 */
	public AbstractAgent() {
		super();
		setAgentID(genarateUniqeID());
		setMyIP();
		
	}
	

	/**
	 * @author Mikamiyama
	 * @return
	 */
	public long getTime() {
		return this.time;
	}

	/**
	 * エージェントIDとして割り当てるためにユニークIDを生成する
	 * @return
	 */
	private String genarateUniqeID() {
		return UUID.randomUUID().toString();
	}

	/**
	 * このエージェントのエージェントIDを返す
	 * @return
	 */
	public String getAgentID() {
		return agentID;
	}

	/**
	 * このエージェントのクラス名（固有名）を返す
	 * @return
	 */
	public String getAgentName() {
		return this.getClass().getName();
	}

	/**
	 * このエージェントの生成されたAgentSphereのアドレスを返す
	 * @return
	 */
	public InetAddress getmyIP() {
		return this.myIP;
	}

	/**
	 * JavaFlowを用いない場合はこちらをオーバーロードして処理を書き込む
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public @continuable void runAgent() {

		/* 初回起動時
		 * Continuationがnullの場合はrun()メソッドを始めから実行する
		 */
		this.threadId = Thread.currentThread().getId();
        System.out.println("[Agent] " + getAgentName() + " running on thread " + threadId);

		
		try {
			primula.agent.util.DHTutil.setAgentIP(getAgentID(), Inet4Address.getByName(IPAddress.myIPAddress));

		} catch (UnknownHostException e1) {
			throw new RuntimeException("IPアドレス設定が不正です");
		}

		if (Javaflow == null) {
			//susspend(=migrate())が中で呼ばれていればその時点での状態を表すインスタンス、run()が終了していればnullが返る
			Javaflow = Continuation.startWith(this);
			
		} else {
			//同上 
			migrateTime = System.nanoTime() - previousMigrateTime;
			AgentInfo info = DHTutil.getAgentInfo(agentID);
			info.migrateTime = this.migrateTime;
			Javaflow = Javaflow.resume();
		}

		while (backupFlag) {
			backupFlag = false;
			AgentAPI.backup(this, ++BACKUPNUM, true);
			Javaflow = Javaflow.resume();
		}

		if (Javaflow != null) {//Javaflowに値が入っている->継続が行われた＝migrate関数が使われた

			KeyValuePair<InetAddress, Integer> address = null;
			try {
				this.address = this.address != null ? this.address : Inet4Address.getByName(IPAddress.IPAddress);
				address = new KeyValuePair<InetAddress, Integer>(this.address, 55878);
				//primula.agent.util.DHTutil.setAgentIP(getAgentID(), this.address);
				//this.address = null; runAgentの最初に実行すればいらない説　というか行先よりもいる場所のほうが正しい　ループバックアドレスとかあるし
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			this.ContinuationFlag = false;
			//System.out.println("//Migration//");
			//System.err.println(this.getAgentName() + ":Migration to " + address.getKey().getHostAddress());

			System.out.println("hi 3");
			primula.agent.util.DHTutil.removeAgentIP(getAgentID());

			AgentAPI.migration(address, this);
			return;
		}
	}


	private void finished() {
		String myname = this.getAgentName();
		clock = ClockSpeed.getClockSpeed();
		MemoryMeasure mm = new MemoryMeasure();

		if (!DHTutil.containsSpec(myname)) {
			ed = System.currentTimeMillis();
			processingtime = ed - time;
			usingmemory = mm.memory_measure(Thread.currentThread().getId());
			SphereSpec ss = new SphereSpec(clock, processingtime, usingmemory);
			DHTutil.setSpec(myname, ss);
			//System.err.println(this.getAgentName() + " usedmemory : " + usingmemory);
		}

		//System.err.println(this.getAgentName() + ": finished");
	}

	//	public void strongMig(KeyValuePair<InetAddress, Integer> address, AbstractAgent agent) {
	//		agent.migrate();
	//		AgentAPI.migration(address, agent);
	//	}

	/**
	 * 処理を中断しmigrateさせる
	 */


	protected @continuable void migrate() {

		System.out.println("hi 1");
		if (this.address != null) {

			if (this.address.isLoopbackAddress()
					|| this.address.getHostAddress().equals(IPAddress.myIPAddress)
					|| !forcedmove) {

				System.out.println("hi 1 1"+this.address.isLoopbackAddress()+this.address.getHostAddress().equals(IPAddress.myIPAddress)
						+forcedmove);
				return;
			}
		}
		
		if(this.progress > 0.9) {
			System.out.println("hi 1 2");
			return;
		}
		
		long now = System.nanoTime();
		long interval = Scheduler.getAgentRemigrateTime(); // 3秒なら 3_000_000_000L など

		// 経過時間が interval より短いなら、まだ再移動禁止
//		if (now - previousMigrateTime < interval) {
//		    System.out.println("hi 1 3: previous=" + previousMigrateTime + ", now=" + now);
//		    return;
//		}

		// ここまで来たら移動OKなので、今回を「前回移動」に更新
		previousMigrateTime = now;
		
		if (!ContinuationFlag) {
			ContinuationFlag = true;

			System.out.println("hi 1 4");
		}
			previousMigrateTime = System.nanoTime();
			Continuation.suspend();

			System.out.println("hi 2");
	}

	/**
	 * 行先アドレスを指定したマイグレーション
	 * @param address
	 */
	public @continuable void migrate(InetAddress address) {
		System.out.println("hi 1");
		this.address = address;
		this.migrate();
	}

	/**
	 * 行先アドレスを指定したマイグレーション
	 * @param address
	 */
	public @continuable void migrate(String address) {
		System.out.println("hi 1");
		InetAddress addr;
		try {
			addr = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("IPアドレス文字列が不正です");
		}
		this.address = addr;
		this.migrate();
	}
	
	
	private double updateEMA(double newSample, double oldWeight) {
		double newWeight = LearningRate * newSample + (1-LearningRate) * oldWeight;
		return newWeight;
	}

	/**
	 * バックアップファイルとしてContinuationデータを記録する
	 */
	public void backup() {
		backupFlag = true;
		Continuation.suspend();
	}

	/**
	 * 外部から中断命令があった場合に実行される処理
	 */
	public abstract void requestStop();

	private void setAgentID(String id) {
		if (agentID == null) {
			this.agentID = id;
		}
	}

	/**
	 * マルチエージェントであるか否かを判定するためのフラグを返す
	 * @return
	 */
	public boolean get_multiFlag() {
		return multiFlag;
	}

	/**
	 * マルチスレッドの要素となるエージェントをリスト化する
	 * @return
	 */
	public ArrayList<AbstractAgent> get_AgentList() {
		ArrayList<AbstractAgent> agent_list = new ArrayList<AbstractAgent>();
		return agent_list;
	}

	/**
	 * フラグを取得し、フラグの値に応じて
	 * シングルエージェントのためのsingleRunかマルチエージェントのためのmultiRunを呼び出す
	 * @param i
	 */
	/*
	public void say(int i){
		System.out.println(this.getClass()+" : say method called...");
	}
	*/

	private void setMyIP() {
		if (myIP == null) {
			try {
				this.myIP = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}

	boolean status_flag = false;

	@Override
	public @continuable ContinuationData call() {
		if (Javaflow == null && callMethod_startFlag == false) {
			callMethod_startFlag = true;
			System.out.println("call() :::Javaflow = null");
			ContinuationData data = new ContinuationData();
			data.data = Continuation.startWith((Runnable) this);
			data.status_flag = true;
			Javaflow = data.data;
			data.flag = checkFlag;
			if (Javaflow == null)
				data.null_flag = true;
			return data;
		} else {
			System.out.println("call() :::Javaflow = not null");
			ContinuationData data = new ContinuationData();
			data.data = Continuation.continueWith(Javaflow);
			Javaflow = data.data;
			data.flag = checkFlag;
			if (Javaflow == null)
				data.null_flag = true;
			return data;
		}
	}

	/**
	 * JavaFlowを利用する際にオーバーロードする
	 * マルチスレッドエージェントのメインエージェントの場合はそのままにしておく
	 */
	@Override
	public @continuable void run() {
		int null_counter = 0;
		boolean start_flag = false;

		boolean mulFlag = this.get_multiFlag();
		ArrayList<AbstractAgent> agent_List = this.get_AgentList();

		ArrayList<Integer> agent_List_switchflag = new ArrayList<Integer>();
		for (int i = 0; i < agent_List.size(); i++)
			agent_List_switchflag.add(0);

		if (mulFlag == true) {

			while (endFlag != true) {
				ArrayList<ContinuationData> agent_List_Copy = new ArrayList<ContinuationData>();

				for (int h = 0; h < agent_List.size(); h++) {
					ContinuationData c_Data = new ContinuationData();
					agent_List_Copy.add(c_Data);
				}

				ArrayList<Continuation> continuation_Strage = new ArrayList<Continuation>();

				ArrayList<Future<ContinuationData>> result_List = new ArrayList<Future<ContinuationData>>();

				ExecutorService service = Executors.newCachedThreadPool();

				if (start_flag == false) {
					for (int i = 0; i < agent_List.size(); i++) {
						result_List.add(service.submit((Callable) agent_List.get(i)));

						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						if (null_counter == agent_List.size() - 1 && agent_List_switchflag.get(i) != 0)
							agent_List.get(i).Javaflow = null;
					}
					start_flag = true;
				}

				int test = 0;
				int count = 0;

				while (continuation_Strage.size() < agent_List.size()) {
					null_counter = 0;

					for (int j = 0; j < agent_List.size() - continuation_Strage.size(); j++) {
						if (agent_List.get(j).Javaflow != null) {

							agent_List_switchflag.set(j, 1);

							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							try {
								agent_List_Copy.get(j).data = result_List.get(j).get().get_data();
								agent_List_Copy.get(j).agent = agent_List.get(j);
								agent_List_Copy.get(j).flag = result_List.get(j).get().get_flag();
							} catch (InterruptedException e) {
								e.printStackTrace();
							} catch (ExecutionException e) {
								e.printStackTrace();
							}

							if (agent_List_Copy.get(j).get_flag() == false) {

								continuation_Strage.add(agent_List_Copy.get(j).get_data());
								agent_List_Copy.remove(j);

							} else {

								if (continuation_Strage.size() > 0) {

									continuation_Strage.add(agent_List_Copy.get(j).get_data());
									agent_List_Copy.remove(j);

								} else {
									service.submit((Callable) agent_List_Copy.get(j).agent);
								}
							}
						} else {
							if (agent_List_switchflag.get(j) == 1) {
								agent_List_switchflag.set(j, 2);
							}
						}

						if (null_counter == agent_List.size() - 1 && agent_List_switchflag.get(j) == 0)
							agent_List_switchflag.set(j, 1);

						if (agent_List_switchflag.get(j) == 2)
							null_counter++;

						if (null_counter == agent_List.size()) {
							return;
						}

					}
					if (continuation_Strage.size() == agent_List.size())
						break;
				}

				Continuation.suspend();

				for (int i = 0; i > agent_List.size(); i++)
					if (agent_List_switchflag.get(i) != 2)
						agent_List_switchflag.set(i, 0);

				migrateFlag = true;
				start_flag = false;
				

				agent_List_Copy.clear();
				result_List.clear();
				continuation_Strage.clear();

			}
		}
	}

	/*author goto*/
	public void kill() {
		//		Thread.currentThread().stop(5);
	}

	public void RegistarHistory(String ip) {
		long time = System.currentTimeMillis() - this.time;
		for (int i = 0; i < his.size(); i++) {
			time -= his.get(i).time;
		}
		//history h=new history(ip,time);
		//his.add(h);
		his.add(new history(ip, time));
	}

	public long getHistoryTime(int num) {
		//		his.push(new);
		return his.get(num).time;
	}
	

	public synchronized void moveByExternal(String nextDirectionIP) {
		this.nextDestination = nextDirectionIP;
		this.shouldMove = true;
	}

}
