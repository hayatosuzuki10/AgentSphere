package scheduler2022;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import primula.api.core.assh.ConsolePanel;
import primula.util.IPAddress;
import scheduler2022.collector.DynamicPcInfoCollector;
import scheduler2022.network.NetworkSpeedReceiverThread;
import scheduler2022.server.DynamicPCInfoPublisher;
import scheduler2022.server.EmbeddedHttpServer;
import scheduler2022.server.SchedulerConfig;
import scheduler2022.server.SchedulerMessageServer;
import scheduler2022.server.SchedulerMessenger;
import scheduler2022.server.StaticPCInfoPublisher;
import scheduler2022.strategy.LoadAverageStrategy;
import scheduler2022.strategy.SchedulerStrategy;
import scheduler2022.strategy.ScoreBasedStrategy;
import scheduler2022.strategy.Strategy2022;
import scheduler2022.util.DHTutil;
import scheduler2022.util.IInfoUpdateListener;
import sphereConnection.EasySphereNetworkManeger;

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
    public static double scoreThreshold = 1;
    public static boolean isServer = true;

    // ★ ここは元コード通り「全IP集合」っぽい用途で使ってるので残す
    private static Set<String> allIPAddresses = new HashSet<String>();

    private static long timeStaticPCInfoChanged = 0;
    private static long timeStampExpire = 4000L;
    private static long previousActivateAgentTime = 0;
    private static long agentObserveTime = 3_000L;       // 3 秒
    private static long agentRemigrateTime = 3_000L;     // 3 秒

    private static volatile ConcurrentHashMap<String, StaticPCInfo> spis = new ConcurrentHashMap<>();
    private static volatile ConcurrentHashMap<String, DynamicPCInfo> dpis = new ConcurrentHashMap<>();
    public static volatile StaticPCInfo SPI;


    public static DynamicPCInfoDetector analyze;
    public static volatile JfrMonitorThread jfr;
    public static volatile JfrMonitorThread.Snapshot snapshot;

    private final DynamicPcInfoCollector collector = new DynamicPcInfoCollector();
    private final scheduler2022.collector.StaticPCInfoCollector staticCollector =
            new scheduler2022.collector.StaticPCInfoCollector();

    private final static DynamicPCInfoPublisher dpiPublisher = new DynamicPCInfoPublisher();
    private final static StaticPCInfoPublisher spiPublisher = new StaticPCInfoPublisher();

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

    @Override
    public void run() {
        if (isServer)
            startHttpServerSecurely();

        startMessageServerSecurely();
        startNetworkSpeedReceiverSecurely();

        jfr = new JfrMonitorThread(snap -> {
            snapshot = snap;
        });
        jfr.start();

        setStaticPCInfo();
        DHTutil.setAcceptable(IPAddress.myIPAddress, true);

        startLoop();
    }

    // ↓-----------run関数用private関数--------------↓

    private void startHttpServerSecurely() {
        try {
            EmbeddedHttpServer server = new EmbeddedHttpServer();
            server.start();  // HTTPサーバーを起動
            System.out.println("AgentSphereを起動中...");
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

    private void setStaticPCInfo() {
        StaticPCInfo spi = staticCollector.collect();
        Scheduler.analyze = new DynamicPCInfoDetector(spi);

        spis.put(IPAddress.myIPAddress, spi);

        // ★ Static を全体に投げるなら publisher 側の宛先設計に合わせて
        spiPublisher.publish(spi);
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
            updateMyDynamicPCInfo();


            if (isServer) {
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

    // ↓-----------tick関数用private関数--------------↓

    private void updateMyDynamicPCInfo() {
        long gcCount = 0L;
        long gcPause = 0L;
        if (snapshot != null) {
            gcCount = snapshot.gcCount;
            gcPause = snapshot.gcPauseMillis;
        }

        DynamicPCInfo dpi = collector.collect(
                allIPAddresses,
                getReceiverPort() + 1,
                isFirst(),
                gcCount,
                gcPause
        );

        DynamicPCInfo prevDPI = dpis.get(IPAddress.myIPAddress);

        long now = System.currentTimeMillis();
        if (
                prevDPI == null
                        || prevDPI.timeStanp + timeStampExpire <= now
                        || !prevDPI.isForecast
                        || dpi.isForecast
        ) {
            getDpis().put(IPAddress.myIPAddress, dpi);
        }


        // ★ 変更が大きいときのみ送る
        getPublisher().publishIfChanged(dpi);

        DynamicPCInfo dpiCopy = dpi.deepCopy();
        Scheduler.analyze.add(dpiCopy);
        if (Scheduler.analyze.size() >= 10) {
            Scheduler.analyze.poll();
        }

        javax.swing.SwingUtilities.invokeLater(() ->
                ConsolePanel.setPanelTitle("LA=" + dpi.LoadAverage + " AgentNum=" + dpi.AgentsNum)
        );

        infoUpdateListeners.parallelStream().forEach(l -> l.pcInfoUpdate(dpi));
    }

    private void updateIPSetChangedTime() {
        Set<String> prev = allIPAddresses;     // 前回
        Set<String> cur = currentIPs();        // 今回（自分も含む）

        if (hasIPSetChanged(prev, cur)) {
            allIPAddresses = cur;
            spiPublisher.publish(spis.get(IPAddress.myIPAddress));
            setTimeStaticPCInfoChanged(System.currentTimeMillis());
        }
    }

    // null を安全に空集合へ
    private static Set<String> safeSet(Set<String> s) {
        return (s == null) ? java.util.Collections.emptySet() : s;
    }

    // 現在のIP集合を取得（自分自身も含める）
    // ★ DHTutil.getAllSuvivalIPaddresses() が壊れているなら
    //   ここも getAliveIPs() に置き換えた方が安全
    private static Set<String> currentIPs() {
        // 以前: DHTutil.getAllSuvivalIPaddresses()
        // 今回: “受信ベース” の alive
        Set<String> cur = new java.util.HashSet<>(Scheduler.getAliveIPs());
        cur.add(IPAddress.myIPAddress);
        return cur;
    }

    // 変化したかどうか（順序に依らず集合等価で判定）
    private static boolean hasIPSetChanged(Set<String> prev, Set<String> cur) {
        return !safeSet(prev).equals(safeSet(cur));
    }

    // ↑-----------tick関数用private関数--------------↑
    // ↑-----------run関数用private関数--------------↑

    public static Map<String, Double> getMigratehint() {
        return migratehint;
    }

    public static Queue<String> getMigrateTickets() {
        return migrateTickets;
    }

    public static SchedulerStrategy getStrategy() {
        return schedulerStrategy;
    }

    public static void storeDPI() {
        DynamicPCInfo dpi = Scheduler.getDpis().get(IPAddress.myIPAddress).deepCopy();
        if (dpiBeforeChange != null) {
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

        if (instanceRef != null) instanceRef.reschedule();

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
        }

        if (isServer) {
            SchedulerConfig config = new SchedulerConfig(
                    strategyName, newInterval, newAgentObserveTime,
                    newAgentRemigrateProhibitTime, newEMAAlpha
            );

            // ★ 生存ノードへ送る（受信ベース）
            for (String ip : getAliveIPs()) {
                if (ip.equals(IPAddress.myIPAddress)) continue;
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

        return (free / total) < 0.05;
    }

    public static long getTimeStaticPCInfoChanged() {
        return timeStaticPCInfoChanged;
    }

    public static void setTimeStaticPCInfoChanged(long timeStaticPCInfoChanged) {
        Scheduler.timeStaticPCInfoChanged = timeStaticPCInfoChanged;
    }

    public static boolean canActivateAgent() {
        if (System.currentTimeMillis() - Scheduler.previousActivateAgentTime > Scheduler.getAgentObserveTime()) {
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

    public static ConcurrentHashMap<String, DynamicPCInfo> getDpis() {
        return dpis;
    }

    public static void setDpis(ConcurrentHashMap<String, DynamicPCInfo> dpis) {
        Scheduler.dpis = dpis;
    }

    public static DynamicPCInfoPublisher getPublisher() {
        return dpiPublisher;
    }

    public static ConcurrentHashMap<String, StaticPCInfo> getSpis() {
        return spis;
    }

    public static void setSpis(ConcurrentHashMap<String, StaticPCInfo> spis) {
        Scheduler.spis = spis;
    }


    public static Set<String> getAliveIPs() {
        try {
            EasySphereNetworkManeger mgr =
                (EasySphereNetworkManeger)
                SingletonS2ContainerFactory
                    .getContainer()
                    .getComponent("EasySphereNetworkManeger");

            if (mgr == null) return Set.of();
            Set<String> ips = mgr.getIPTable();
            return (ips == null) ? Set.of() : ips;

        } catch (Throwable t) {
            System.err.println("[Scheduler] getAliveIPs failed: " + t);
            return Set.of();
        }
    }
    

}