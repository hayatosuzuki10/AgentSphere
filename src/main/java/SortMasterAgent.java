import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;
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
        public long sortMs;
    }

    public static class SortInitMessage implements Serializable {
        public int totalRounds;
    }

    public static class StopMessage implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    /* =====================
     * 設定
     * ===================== */

    private final int arraySize = 2_000_000;
    private final int numWorkers = 8;
    private int rounds = 20;
    private static final int MSG_PORT = 55878;

    /* =====================
     * 状態
     * ===================== */

    private AbstractAgent[] slaves = new AbstractAgent[numWorkers];
    private int[][] sortedSegments = new int[numWorkers][];
    private boolean[] finishedWorker = new boolean[numWorkers];

    private volatile boolean roundFinished = false;
    private volatile int expectedRound = -1;

    /* =====================
     * run
     * ===================== */

    @Override
    public void run() {
    	setAgentClassInfo(
    		    2000,         // CPU
    		    0,            // GPU
    		    5_000_000,    // netUp (5MB/s)
    		    5_000_000,    // netDown
    		    30_000_000,   // heap
    		    1,            // gc
    		    0,            // diskRead
    		    0,            // diskWrite
    		    0             // migrateTime
    		);
        long totalStart = System.currentTimeMillis();

        System.out.println("[SortMaster] START id=" + getAgentID()
                + " ip=" + IPAddress.myIPAddress
                + " rounds=" + rounds);

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

        /* === INIT 送信 === */
        SortInitMessage init = new SortInitMessage();
        init.totalRounds = rounds;

        for (int w = 0; w < numWorkers; w++) {
            send(slaves[w], init);
            System.out.println("[SortMaster] INIT sent to " + slaves[w].getAgentID()
                    + " totalRounds=" + rounds);
        }

        /* =====================
         * round ループ
         * ===================== */
        for (int round = 0; round < rounds; round++) {

            expectedRound = round;
            roundFinished = false;
            Arrays.fill(finishedWorker, false);
            Arrays.fill(sortedSegments, null);

            System.out.println("\n[SortMaster] ===== Round " + round + " START =====");

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

            System.out.println("[SortMaster] Round " + round
                    + " TASK sent to all workers");

            /* 完了待ち */
            while (!roundFinished) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }

            int[] result = mergeAll(sortedSegments);

            System.out.println("[SortMaster] ===== Round " + round
                    + " FINISHED sortedOK=" + isSorted(result) + " =====");
        }

        /* === STOP 送信 === */
        StopMessage stop = new StopMessage();
        for (int w = 0; w < numWorkers; w++) {
            send(slaves[w], stop);
            System.out.println("[SortMaster] STOP sent to " + slaves[w].getAgentID());
        }

        long end = System.currentTimeMillis();
        System.out.println("[SortMaster] END totalTimeMs=" + (end - totalStart));

        safeReportToDemo("SortMaster finished in " + (end - totalStart) + " ms");
        demo.reportAgentHistory(getAgentID(), getAgentName(), buildHistoryText());
    }

    /* =====================
     * 受信処理
     * ===================== */

    @Override
    public synchronized void receivedMessage(AbstractEnvelope envelope) {

        Object raw = ((StandardContentContainer) envelope.getContent()).getContent();
        if (!(raw instanceof SortResultMessage)) return;

        SortResultMessage msg = (SortResultMessage) raw;

        System.out.println("[SortMaster] RESULT received"
                + " round=" + msg.round
                + " worker=" + msg.workerId
                + " sortMs=" + msg.sortMs);

        if (msg.round != expectedRound) {
            System.out.println("[SortMaster] IGNORE result (expected="
                    + expectedRound + ")");
            return;
        }

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

    public void setRounds(int rounds) { this.rounds = rounds; }

    private void send(AbstractAgent agent, Serializable msg) {
        try {
        	InetAddress ipAddr = null;
        	while (true) {
        	    Object raw = primula.agent.util.DHTutil.getAgentIP(agent.getAgentID());
        	    if(raw instanceof InetAddress) {
            		ipAddr = (InetAddress) raw;
            		break;
            	}
        	    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        	}
            KeyValuePair<InetAddress, Integer> ip =
                    new KeyValuePair<>(ipAddr, MSG_PORT);

            MessageAPI.send(
                ip,
                new StandardEnvelope(
                    new AgentAddress(agent.getAgentID()),
                    new StandardContentContainer(msg)
                )
            );
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