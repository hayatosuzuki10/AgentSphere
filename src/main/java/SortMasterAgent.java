import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.assh.command.demo;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class SortMasterAgent extends AbstractAgent implements IMessageListener {

    /* =====================
     * 内部メッセージ
     * ===================== */

    public static class SortTaskMessage implements Serializable {
        public int workerId;
        public int round;
        public int[] segment;
        public String masterId;
    }

    public static class SortResultMessage implements Serializable {
        public int workerId;
        public int round;
        public int[] sortedSegment;
        public long sortMs;   // ★ 修論用：Slave 側ソート時間
    }

    /* =====================
     * 設定
     * ===================== */
    
    private String homeIP = IPAddress.myIPAddress;

    private final int arraySize = 2_000_000;
    private final int numWorkers = 8;
    private final int rounds = 20;
    private static final int MSG_PORT = 55878;

    /* =====================
     * 状態
     * ===================== */

    private AbstractAgent[] slaves = new AbstractAgent[numWorkers];
    private int[][] sortedSegments = new int[numWorkers][];
    private boolean[] finishedWorker = new boolean[numWorkers];

    private volatile boolean roundFinished = false;
    private volatile int expectedRound = -1;

    @Override
    public void run() {

        long totalStart = System.currentTimeMillis();
        StringBuilder report = new StringBuilder();

        report.append("==== Sort Distributed Result ====\n");
        report.append("MasterID   : ").append(getAgentID()).append("\n");
        report.append("MasterIP   : ").append(IPAddress.myIPAddress).append("\n");
        report.append("Workers    : ").append(numWorkers).append("\n");
        report.append("ArraySize  : ").append(arraySize).append("\n");
        report.append("Rounds     : ").append(rounds).append("\n");
        report.append("StartTime  : ")
              .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
              .format(new Date(totalStart))).append("\n\n");

        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            safeReportToDemo("SortMaster FAILED: listener error " + e);
            return;
        }

        /* === Slave 起動 === */
        for (int i = 0; i < numWorkers; i++) {
            Object inst = loadAgentInstance("SortSlaveAgent");
            if (!(inst instanceof AbstractAgent)) {
                System.err.println("[SortMaster] Failed to create slave " + i);
                continue;
            }
            slaves[i] = (AbstractAgent) inst;

            // ★ Master 情報を Slave に渡す
            if (slaves[i] instanceof SortSlaveAgent) {
                ((SortSlaveAgent) slaves[i]).setMasterInfo(
                        getAgentID(),
                        IPAddress.myIPAddress,
                        MSG_PORT
                );
            }

            AgentAPI.runAgent(slaves[i]);
            System.out.println("[SortMaster] Slave started id=" + slaves[i].getAgentID());
        }

        /* === round ループ === */
        for (int round = 0; round < rounds; round++) {
            expectedRound = round;
            roundFinished = false;
            Arrays.fill(finishedWorker, false);
            Arrays.fill(sortedSegments, null);

            long roundStart = System.currentTimeMillis();

            int[] data = createRandomArray(round);
            int[][] ranges = divideRanges(data.length, numWorkers);

            for (int w = 0; w < numWorkers; w++) {
                SortTaskMessage msg = new SortTaskMessage();
                msg.workerId = w;
                msg.round = round;
                msg.segment = Arrays.copyOfRange(data, ranges[w][0], ranges[w][1]);
                msg.masterId = getAgentID();
                send(slaves[w], msg);
            }

            /* 完了待ち */
            while (!roundFinished) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }

            int[] result = mergeAll(sortedSegments);
            long roundMs = System.currentTimeMillis() - roundStart;

            report.append("[Round ").append(round).append("]\n");
            report.append("  roundTimeMs : ").append(roundMs).append("\n");
            report.append("  sortedOK    : ").append(isSorted(result)).append("\n\n");
        }

        long end = System.currentTimeMillis();

        report.append("TotalTimeMs : ").append(end - totalStart).append("\n");
        report.append("EndTime     : ")
              .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
              .format(new Date(end))).append("\n");

        System.out.println(report.toString());
        safeReportToDemo(report.toString());

        //migrate(homeIP);
        demo.reportAgentHistory(getAgentID(), buildHistoryText());
    }

    /* =====================
     * 受信処理
     * ===================== */

    @Override
    public synchronized void receivedMessage(AbstractEnvelope envelope) {

        Object raw = ((StandardContentContainer) envelope.getContent()).getContent();
        if (!(raw instanceof SortResultMessage)) return;

        SortResultMessage msg = (SortResultMessage) raw;
        if (msg.round != expectedRound) return;

        sortedSegments[msg.workerId] = msg.sortedSegment;
        finishedWorker[msg.workerId] = true;

        for (boolean f : finishedWorker) {
            if (!f) return;
        }
        roundFinished = true;
    }

    /* =====================
     * utility
     * ===================== */

    private int[] createRandomArray(int seed) {
        int[] a = new int[arraySize];
        Random r = new Random(seed);
        for (int i = 0; i < a.length; i++) a[i] = r.nextInt();
        return a;
    }

    private void send(AbstractAgent agent, Serializable msg) {
        try {
            KeyValuePair<InetAddress, Integer> ip =
                    new KeyValuePair<>(agent.getmyIP(), MSG_PORT);
            StandardEnvelope env = new StandardEnvelope(
                    new AgentAddress(agent.getAgentID()),
                    new StandardContentContainer(msg)
            );
            MessageAPI.send(ip, env);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int[][] divideRanges(int n, int k) {
        int[][] r = new int[k][2];
        int base = n / k, rem = n % k, cur = 0;
        for (int i = 0; i < k; i++) {
            int sz = base + (i < rem ? 1 : 0);
            r[i][0] = cur;
            r[i][1] = cur + sz;
            cur += sz;
        }
        return r;
    }

    private int[] mergeAll(int[][] segs) {
        int[] acc = segs[0];
        for (int i = 1; i < segs.length; i++) acc = merge(acc, segs[i]);
        return acc;
    }

    private int[] merge(int[] a, int[] b) {
        int[] r = new int[a.length + b.length];
        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length)
            r[k++] = (a[i] <= b[j]) ? a[i++] : b[j++];
        while (i < a.length) r[k++] = a[i++];
        while (j < b.length) r[k++] = b[j++];
        return r;
    }

    private boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++)
            if (a[i - 1] > a[i]) return false;
        return true;
    }

    private Object loadAgentInstance(String className) {
        Object obj = null;
        ChainContainer cc;
        GhostClassLoader gcl;
        String path = ".\\target\\classes";

        gcl = GhostClassLoader.unique;
        cc = gcl.getChainContainer();

        try {
            cc.resistNewClassLoader(new StringSelector(path), new File(path));
            Class<?> cls = gcl.loadClass(className);
            obj = cls.newInstance();
        } catch (Exception e) {
            Logger.getLogger(getClass()).log(Level.ERROR, null, e);
        }
        return obj;
    }

    private void safeReportToDemo(String text) {
        try {
            demo.reportMasterFinished(getAgentID(), text);
        } catch (Throwable t) {
            System.err.println("[SortMaster] report to demo failed: " + t);
        }
    }

    @Override public void requestStop() {}
    @Override public String getStrictName() { return getAgentID(); }
    @Override public String getSimpleName() { return getAgentName(); }
}