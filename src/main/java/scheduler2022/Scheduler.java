package scheduler2022;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import primula.agent.AbstractAgent;
import primula.api.core.agent.AgentInstanceInfo;
import primula.api.core.assh.ConsolePanel;
import primula.util.IPAddress;
import scheduler2022.collector.PCInfoCollector;
import scheduler2022.network.NetworkSpeedReceiverThread;
import scheduler2022.server.EmbeddedHttpServer;
import scheduler2022.server.SchedulerConfig;
import scheduler2022.server.SchedulerMessageServer;
import scheduler2022.server.SchedulerMessenger;
import scheduler2022.strategy.LoadAverageStrategy;
import scheduler2022.strategy.SchedulerStrategy;
import scheduler2022.strategy.ScoreBasedStrategy;
import scheduler2022.strategy.ScoreBasedStrategy2;
import scheduler2022.strategy.Strategy2022;
import scheduler2022.util.DHTutil;
import scheduler2022.util.IInfoUpdateListener;

public class Scheduler implements Runnable {
	private static int count = 0;
	CentralProcessor cp = new SystemInfo().getHardware().getProcessor();

	private static List<IInfoUpdateListener> infoUpdateListeners = new CopyOnWriteArrayList<IInfoUpdateListener>();
	private static int receiverPort = 5000;
	private static boolean first = true;
	public static Map<String, Double> migratehint;
	public static Queue<String> migrateTickets;
	public static SchedulerStrategy schedulerStrategy = new LoadAverageStrategy();
	public boolean hasStop = false;
	public static long dpiTimeLimit = 3000;
	private static long timeBeforeDPIChange;
	private static long timeAfterDPIChange;
	public static DynamicPCInfo dpiBeforeChange;
	public static DynamicPCInfo previousDPIBeforeChange;
	private static double emaAlpha = 0.1;
	public static double scoreThreshold = 2;
	public static boolean isServer = false;
	private static Set<String> allIPAddresses = new HashSet<String>();
	private static long timeStaticPCInfoChanged = 0;
	private static long timeStampExpire = 4000L;
	private static long previousActivateAgentTime = 0;
	private static long agentObserveTime = 3_000_000_000L;
	private static long agentRemigrateTime = 3_000_000_000L;
	public static volatile DynamicPCInfo latestDPI;
	public static volatile StaticPCInfo SPI;
	

    public static Map<String, AgentInstanceInfo> agentInfo = new HashMap<>();
	
	public static DynamicPCInfoDetector analyze;
	

	
	/**
	 * スケジューラの情報更新間隔を表す定数(ms)
	 */
	public static int UPDATE_SPAN = 1000;
	private static Scheduler instanceRef;

	public Scheduler() {
		instanceRef = this;
		count++;
		System.out.println("Class Name is " + this.getClass().getName());
	}
	
	public void run() {
		if(isServer)
			startHttpServerSecurely();
		
		startMessageServerSecurely();
		startNetworkSpeedReceiverSecurely();
		startPCInfoCollctorSecurely();


		
	    DHTutil.setAcceptable(IPAddress.myIPAddress, true);
	    Runtime rt = Runtime.getRuntime();
	    long max = rt.maxMemory();      // -Xmx 相当（最大ヒープ）
	    long total = rt.totalMemory();  // 現在JVMが確保してるヒープ
	    long free = rt.freeMemory();    // totalの中で空いてる分
	    System.out.printf("[Heap] max=%.2fGB total=%.2fGB free=%.2fGB used=%.2fGB%n",
	            max/1e9, total/1e9, free/1e9, (total-free)/1e9);
		startLoop();

	    // 終了時のクリーンアップ
	    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	        try { ses.shutdownNow(); } finally { removeInfo(); }
	    }));
		
		
	}
	
	// ↓-----------run関数用private関数--------------↓
	
	private void startHttpServerSecurely() {
			try {
				EmbeddedHttpServer server = new EmbeddedHttpServer();
				server.start();  // HTTPサーバーを起動

				// あとは通常どおり AgentSphere を起動
				System.out.println("AgentSphereを起動中...");
				// Scheduler起動 or Agent登録など...

			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	private void startMessageServerSecurely() {
	    try {
	        SchedulerMessageServer server = new SchedulerMessageServer(8888);
	        server.start();
	    } catch (IOException e) {
	        System.err.println("[FATAL] MessageServer start failed on port 8888: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	private void startPCInfoCollctorSecurely() {
	    try {
	        Thread t = new Thread(new PCInfoCollector());
	        t.setDaemon(true);
	        t.start();
	    } catch (Exception e) {
	        System.err.println("[WARN] NetworkSpeedReceiver failed: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	private void startNetworkSpeedReceiverSecurely() {
	    try {
	        Thread receiver = new NetworkSpeedReceiverThread(getReceiverPort());
	        receiver.setDaemon(true);
	        receiver.start();
	    } catch (Exception e) {
	        System.err.println("[WARN] NetworkSpeedReceiver failed: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	



	private void startLoop() {
	    loopFuture = ses.scheduleAtFixedRate(this::tick, 0, UPDATE_SPAN, TimeUnit.MILLISECONDS);
	}

	private final ScheduledExecutorService ses =
	    Executors.newSingleThreadScheduledExecutor(r -> {
	        Thread t = new Thread(r, "SchedulerLoop");
	        t.setDaemon(true);
	        return t;
	    });
	private ScheduledFuture<?> loopFuture;


	private void tick() {
	    if (hasStop) return;
	    try {
	        if(isServer) {
	        	updateIPSetChangedTime();
	        }
	        schedulerStrategy.initialize();
	        schedulerStrategy.excuteMainLogic();
	        schedulerStrategy.cleanUp();
	        javax.swing.SwingUtilities.invokeLater(ConsolePanel::autoscroll);
	        Scheduler.first = false;
	    } catch (Throwable t) {
	        t.printStackTrace();
	    }
	}


	
	
	private void updateIPSetChangedTime() {

	    Set<String> prev = allIPAddresses;  // 前回値（null可）
	    Set<String> cur  = currentIPs();                    // 今回値（自分を含む）


	    if(hasIPSetChanged(prev, cur)) {
		    allIPAddresses = cur; // 最新値に更新
		    setTimeStaticPCInfoChanged(System.currentTimeMillis());
	    }
	}
	
	
	
	// null を安全に空集合へ
	private static Set<String> safeSet(Set<String> s) {
	    return (s == null) ? java.util.Collections.emptySet() : s;
	}

	// 現在のIP集合を取得（自分自身も含める）
	private static Set<String> currentIPs() {
	    Set<String> cur = new java.util.HashSet<>(InformationCenter.getOthersIPs());
	    cur.add(IPAddress.myIPAddress);
	    return cur;
	}

	// 変化したかどうか（順序に依らず集合等価で判定）
	private static boolean hasIPSetChanged(Set<String> prev, Set<String> cur) {
	    return !safeSet(prev).equals(safeSet(cur));
	}



	
	
	public void removeInfo() {
		DHTutil.removePcInfo(IPAddress.myIPAddress);
		DHTutil.removeStaticPCInfo(IPAddress.myIPAddress);
	}

	// ↑-----------tick関数用private関数--------------↑


	// ↑-----------run関数用private関数--------------↑
	
	/**
	 * そのタイミングでのmigrateのヒントとなる正規化されたロードアベレージベースの比率表を取得します
	 * @see Scheduler#makemigrateHints
	 */
	public static Map<String, Double> getMigratehint() {
		return migratehint;
	}

	/**
	 * ロードアベレージと個数ベースによるスケジューリングの結果移動することを推奨する
	 * AgentSphereのIPアドレスがキューイングされているキューを返します
	 * <p>
	 * このキューはConcurrentLinkedQueueによるスレッドセーフ実装です
	 * @return
	 */
	public static Queue<String> getMigrateTickets() {
		return migrateTickets;
	}


	public static SchedulerStrategy getStrategy() {
		return schedulerStrategy;
	}
	
	public static void storeDPI() {
		DynamicPCInfo dpi = InformationCenter.getMyDPI();
		if(dpiBeforeChange != null) {
			previousDPIBeforeChange = dpiBeforeChange.deepCopy();
			timeBeforeDPIChange = timeAfterDPIChange;
		}
		dpiBeforeChange = dpi;
		timeAfterDPIChange = System.currentTimeMillis();
	}
	
	public static boolean hasSignificantDPIChange() {
		
		return false;
	}


	public static int getReceiverPort() {
		return receiverPort;
	}


	public static void setReceiverPort(int newReceiverPort) {
		receiverPort = newReceiverPort;
	}


	public static boolean isFirst() {
		return first;
	}


	public static void setFirst(boolean first) {
		Scheduler.first = first;
	}
	

	public static void registerInfoUpdateListener(IInfoUpdateListener listener) {
		infoUpdateListeners.add(listener);
	}

	public static void removeInfoUpdateListener(IInfoUpdateListener listener) {
		infoUpdateListeners.remove(listener);
	}
	
	


	
	public static void setStrategyAndInterval(
			String strategyName, 
			int newInterval, 
			long newAgentObserveTime, 
			long newAgentRemigrateProhibitTime, 
			double newEMAAlpha
			) {
		UPDATE_SPAN = newInterval;

		Scheduler.agentObserveTime = newAgentObserveTime;
		
		Scheduler.agentRemigrateTime = newAgentRemigrateProhibitTime;
		
		Scheduler.emaAlpha = newEMAAlpha;
		if (instanceRef != null) instanceRef.reschedule(); // Scheduler のインスタンス参照を持つ
		switch (strategyName) {
			case "Strategy2022":
				schedulerStrategy = new Strategy2022();
				break;
			case "LoadAverageStrategy":
				schedulerStrategy = new LoadAverageStrategy();
				break;
			case "ScoreBasedStrategy":
				schedulerStrategy = new ScoreBasedStrategy();
				break;
			case "ScoreBasedStrategy2":
				schedulerStrategy = new ScoreBasedStrategy2();
				break;
		}
		
		if(isServer) {
			SchedulerConfig config = new SchedulerConfig(strategyName, newInterval, newAgentObserveTime, newAgentRemigrateProhibitTime, newEMAAlpha);
			for(String ip : InformationCenter.getOthersIPs()) {
				System.out.println(ip);
				SchedulerMessenger.sendChangeSchedulerStrategyRequest(ip, 8888, config);
			}
		}
		System.out.println(schedulerStrategy.getClass().getName());
		
	}
	
	private void reschedule() {
	    if (loopFuture != null) loopFuture.cancel(false);
	    startLoop();
	}
	
	public int getCount() {
		return count;
	}

	public void countMinus() {
		count--;
	}

	public void countReset() {
		count = 0;
	}

	static boolean isLimited() {
		Runtime runtime = Runtime.getRuntime();
		double total = runtime.maxMemory();
		double free = runtime.freeMemory();

		if ((free / total) < 0.05)
			return true;
		return false;
	}

	public static long getTimeStaticPCInfoChanged() {
		return timeStaticPCInfoChanged;
	}

	public static void setTimeStaticPCInfoChanged(long timeStaticPCInfoChanged) {
		Scheduler.timeStaticPCInfoChanged = timeStaticPCInfoChanged;
	}
	
	
	public static boolean canActivateAgent() {
		if(System.currentTimeMillis() - Scheduler.previousActivateAgentTime > Scheduler.getAgentObserveTime()) {
			Scheduler.previousActivateAgentTime = System.currentTimeMillis();
			return true;
		}
		return false;
	}
	
	public static void setAgentObserveTime(long time) {
		Scheduler.agentObserveTime = time;
	}

	public static long getAgentObserveTime() {
		return agentObserveTime;
	}

	public static long getAgentRemigrateTime() {
		return agentRemigrateTime;
	}

	public static void setAgentRemigrateTime(long agentRemigrateTime) {
		Scheduler.agentRemigrateTime = agentRemigrateTime;
	}

	public static double getEmaAlpha() {
		return emaAlpha;
	}

	public static void setEmaAlpha(double emaAlpha) {
		Scheduler.emaAlpha = emaAlpha;
	}


	public static long getTimeStampExpire() {
		return timeStampExpire;
	}

	public static void setTimeStampExpire(long timeStampExpire) {
		Scheduler.timeStampExpire = timeStampExpire;
	}
	
	public static String getNextDestination(AbstractAgent agent) {
		boolean shouldMove = Scheduler.getStrategy().shouldMove(agent);
        String nextDestination = Scheduler.getStrategy().getDestination(agent);

        // Strategy2022 のときだけ上書きしたいなら「{}」必須
        if (Scheduler.getStrategy().getClass().getName().contains("Strategy2022")) {
            nextDestination = RecommendedDest.recomDest(agent.getAgentName());
        }
        if (shouldMove
        	    && !IPAddress.myIPAddress.equals(nextDestination)) {
        	System.out.println("will move");
        	System.out.println("[DemoAgent] 強制移動。移動先: " + nextDestination);
        	
        }
        return nextDestination;
	}

}
