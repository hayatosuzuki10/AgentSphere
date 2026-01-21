package scheduler2022.collector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import primula.api.core.agent.AgentClassInfo;
import primula.api.core.assh.ConsolePanel;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.DynamicPCInfoDetector;
import scheduler2022.InformationCenter;
import scheduler2022.JfrMonitorThread;
import scheduler2022.JudgeOS;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;
import scheduler2022.repository.PCInfoRepository;
import scheduler2022.util.DHTutil;
import scheduler2022.util.IInfoUpdateListener;

/**
 * 動的PC情報（DynamicPCInfo）を定期的に収集し、
 * - DHT（DHTutil）に登録
 * - InfomationCenter に反映
 *
 * するためのコレクタ。
 *
 * <p>
 * Runnable を実装しているので、呼び出し側で Thread に乗せて実行する。
 * 例:
 * <pre>
 *   Thread t = new Thread(new DynamicPcInfoCollector());
 *   t.setDaemon(true);
 *   t.start();
 * </pre>
 */
public class PCInfoCollector implements Runnable {

    /** 自ノードの IP アドレス */
    private final String myIP = IPAddress.myIPAddress;

    /** 収集間隔 [ms] */
    private final long intervalMillis;
    

	CentralProcessor cp = new SystemInfo().getHardware().getProcessor();
	
	Set<String> allIPs = DHTutil.getAllSuvivalIPaddresses();
	
	private DynamicPCInfo myDPI = new DynamicPCInfo();
    

	private final DynamicPcInfoCollector collector = new DynamicPcInfoCollector();
	private final static PCInfoRepository pcInfoRepo = new PCInfoRepository();
	private final scheduler2022.collector.StaticPCInfoCollector staticCollector =
	        new scheduler2022.collector.StaticPCInfoCollector();
	
	private static List<IInfoUpdateListener> infoUpdateListeners = new CopyOnWriteArrayList<IInfoUpdateListener>();
	

    private Map<String, DynamicPCInfo> allDPIs = new HashMap<>();
    private Map<String, StaticPCInfo> allSPIs = new HashMap<>();
    
    private Map<String, AgentClassInfo> allAgentInfo = new HashMap<>();

	public static volatile JfrMonitorThread jfr;
	public static volatile JfrMonitorThread.Snapshot snapshot;

    public PCInfoCollector() {
        this(1000L); // デフォルト 1秒周期
    }

    public PCInfoCollector(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
    	
		jfr = new JfrMonitorThread(snap -> {
		    // ここでダッシュボードに送る / ログる / 判定する など
			snapshot = snap;

		});
		
		
		jfr.start();
		
		setStaticPCInfo();
        // 割り込みされるまでループ
        while (!Thread.currentThread().isInterrupted()) {
        	
        	
            try {
                // 1) 自ノードの DynamicPCInfo を収集
            	updateMyDynamicPCInfo();
                // 必要ならここで JFR の Snapshot から socketReadBytes 等を埋める

                // 2) DHT に自分の DPI を登録
                DHTutil.setPcInfo(myIP, myDPI);

                // 3) InfomationCenter に自ノードの DPI を登録
                //    （deepCopy しておくと他スレッドからの書き換えを防げる）
                InformationCenter.setMyDPI(myDPI);

                // 4) 生存中ノードの IP 一覧を取得
                allIPs = DHTutil.getAllSuvivalIPaddresses(); // ← 名前に typo あるけど仕様に合わせる

            	InformationCenter.setAllIPs(allIPs);
                
                // 5) 各ノードの DPI を DHT から取得して Map にまとめる
                for (String ip : allIPs) {
                    DynamicPCInfo dpi = DHTutil.getPcInfo(ip);
                    if (dpi != null) {
                        allDPIs.put(ip, dpi);
                        for (var agent : dpi.Agents.values()) {
                            AgentClassInfo info = DHTutil.getAgentInfo(agent.Name);
                            allAgentInfo.put(agent.Name, info);
                        }
                    }
                    StaticPCInfo spi = DHTutil.getStaticPCInfo(ip);
                    if(spi != null) {
                    	allSPIs.put(ip, spi);
                    }
                }

                // 6) InfomationCenter に「他ノードの DPI 一覧」として反映
                InformationCenter.setOtherDPIs(allDPIs);
                InformationCenter.setOtherSPIs(allSPIs);
                InformationCenter.setAllAgentClassInfos(allAgentInfo);
                


                // 7) 次回収集までスリープ
                Thread.sleep(intervalMillis);

            } catch (InterruptedException e) {
                // 割り込みされたらフラグを立ててループを抜ける
                Thread.currentThread().interrupt();
                break;

            } catch (Throwable t) {
                // 収集中の例外はログだけ出して継続する
                t.printStackTrace();
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
	private void updateMyDynamicPCInfo() {
		
	    double tmp = cp.getSystemLoadAverage(1)[0];
	    if (tmp < 0) return;
	    if (JudgeOS.isWindows()) tmp += 1.0;

	    final double la = tmp; 
	    DynamicPCInfo prevDPI = InformationCenter.getMyDPI();
	    DynamicPCInfo dpi;
	    if (prevDPI != null && prevDPI.isForecast
	            && prevDPI.timeStanp + Scheduler.getTimeStampExpire() > System.currentTimeMillis()) {

	        dpi = prevDPI;

	    } else {
	        // ここを Collector に委譲
	    	dpi = collector.collect(
	                allIPs,
	                Scheduler.getReceiverPort() + 1,
	                Scheduler.isFirst(),
	                snapshot.gcCount,          // JFRからの値
	                snapshot.gcPauseMillis     // JFRからの値
	        );
	        pcInfoRepo.saveDynamic(IPAddress.myIPAddress, dpi);
	    }
	    myDPI = dpi;
	    DynamicPCInfo dpiCopy = dpi.deepCopy();
	    Scheduler.analyze.add(dpiCopy);
	    if(Scheduler.analyze.size() >= 10) {
	    	Scheduler.analyze.poll();
	    }
	    javax.swing.SwingUtilities.invokeLater(() ->
	    	ConsolePanel.setPanelTitle("LA=" + la + " AgentNum=" + dpi.AgentsNum));
	    infoUpdateListeners.parallelStream().forEach(l -> l.pcInfoUpdate(dpi));
	    StaticPCInfo spi = DHTutil.getStaticPCInfo(IPAddress.myIPAddress);
	    if(spi == null || spi.CPU == null || spi.CPU.BenchMarkScore == 0) {
	    	setStaticPCInfo();
	    }
	}
	

	private void setStaticPCInfo() {
	    StaticPCInfo spi = staticCollector.collect();
	    Scheduler.analyze = new DynamicPCInfoDetector(spi);
	    scheduler2022.util.DHTutil.setStaticPCInfo(primula.util.IPAddress.myIPAddress, spi);
	    InformationCenter.setMySPI(spi);
	
	}
	

	public static PCInfoRepository getPcInfoRepo() {
		return pcInfoRepo;
	}
	
}