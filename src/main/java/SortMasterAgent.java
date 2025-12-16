import java.io.File;
import java.io.IOException;
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
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;

public class SortMasterAgent extends AbstractAgent implements IMessageListener {

    /* =====================
     * 内部メッセージクラス
     * ===================== */

    public static class SortTaskMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        public int workerId;
        public int[] segment;
        public String masterId;
        public String masterHost;
    }

    public static class SortResultMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        public int workerId;
        public int[] sortedSegment;
    }

    /* =====================
     * Master 本体
     * ===================== */

    private int arraySize = 10_000_000;
    private int numWorkers = 8;

    private int[] data;
    private int[][] sortedSegments = new int[numWorkers][];
    private boolean[] finishedWorker = new boolean[numWorkers];

    private volatile boolean finished = false;
    private long sleepMillis = 1000L;

    private String slaveClassName = "SortSlaveAgent";

    @Override
    public void run() {

        System.out.println("[SortMaster] START id=" + getAgentID());

        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        prepareData();

        int[][] ranges = divideRanges(data.length, numWorkers);

        String masterId = getStrictName();
        String masterHost = getmyIP().getHostAddress();

        for (int w = 0; w < numWorkers; w++) {

            Object inst = loadAgentInstance(slaveClassName);
            if (!(inst instanceof AbstractAgent)) continue;

            AbstractAgent slave = (AbstractAgent) inst;
            AgentAPI.runAgent(slave);

            SortTaskMessage msg = new SortTaskMessage();
            msg.workerId = w;
            msg.segment = Arrays.copyOfRange(data, ranges[w][0], ranges[w][1]);
            msg.masterId = masterId;
            msg.masterHost = masterHost;

            try {
                KeyValuePair<InetAddress, Integer> ip =
                        new KeyValuePair<>(slave.getmyIP(), 55878);

                StandardEnvelope env = new StandardEnvelope(
                        new AgentAddress(slave.getAgentID()),
                        new StandardContentContainer(msg)
                );

                MessageAPI.send(ip, env);

                System.out.println("[SortMaster] sent task to worker " + w);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        while (!finished) {
            try { Thread.sleep(sleepMillis); }
            catch (InterruptedException e) {}
        }

        int[] result = mergeAll(sortedSegments);
        System.out.println("[SortMaster] FINISHED. sorted=" + isSorted(result));
    }

    /* =====================
     * 受信処理
     * ===================== */

    @Override
    public void receivedMessage(AbstractEnvelope envelope) {

        Object raw = ((StandardContentContainer) envelope.getContent()).getContent();
        if (!(raw instanceof SortResultMessage)) return;

        SortResultMessage msg = (SortResultMessage) raw;
        sortedSegments[msg.workerId] = msg.sortedSegment;
        finishedWorker[msg.workerId] = true;

        boolean allDone = true;
        for (boolean f : finishedWorker) if (!f) allDone = false;

        if (allDone) finished = true;
    }

    /* =====================
     * utility
     * ===================== */

    private void prepareData() {
        data = new int[arraySize];
        Random r = new Random(0);
        for (int i = 0; i < arraySize; i++) data[i] = r.nextInt();
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
        for (int i = 1; i < segs.length; i++) {
            acc = merge(acc, segs[i]);
        }
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
        Class<?> cls;
        ChainContainer cc;
        GhostClassLoader gcl;
        String path = ".\\bin";

        gcl = GhostClassLoader.unique;
        cc = gcl.getChainContainer();

        try {
            cc.resistNewClassLoader(new StringSelector(path), new File(path));
        } catch (IOException e) {
            Logger.getLogger(getClass()).log(Level.TRACE, null, e);
        }

        try {
            cls = gcl.loadClass(className);
            obj = cls.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return obj;
    }

    @Override public void requestStop() {}
    @Override public String getStrictName() { return getAgentID(); }
    @Override public String getSimpleName() { return getAgentName(); }
}