import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.javaflow.api.continuable;
import org.deeplearning4j.datasets.fetchers.DataSetType;
import org.deeplearning4j.datasets.iterator.impl.Cifar10DataSetIterator;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;
import scheduler2022.Scheduler;

public class DL4JMSSlave extends AbstractAgent implements IMessageListener {

    private String parentId;
    private String masterIp;
    private int masterPort = 55878;

    private volatile boolean running = true;
    private final int batchSize = 32;
    private final int localEpochs = 1;

    private boolean loaded = false;

    private int partIndex = 0;
    private int totalParts = 1;

    private List<DataSet> localData = null;

    // setter
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setMasterIp(String masterIp) { this.masterIp = masterIp; }
    public void setMasterPort(int masterPort) { this.masterPort = masterPort; }
    public void setPartIndex(int idx) { this.partIndex = idx; }
    public void setTotalParts(int tot) { this.totalParts = tot; }

    public @continuable void run() {
        System.out.println("[Slave] 起動: " + getAgentName() + " id=" + getAgentID());

        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        loadLocalTrainingData();
        loaded = true;

        while (running) {
        	nextDestination = Scheduler.getNextDestination(this);
            migrate(nextDestination);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void receivedMessage(AbstractEnvelope env) {
        try {
            byte[] payload =
                (byte[]) ((StandardContentContainer) env.getContent()).getContent();
            if (payload == null || payload.length < 1) return;

            byte type = payload[0];

            // type=2 : [type][round(int)][modelBytes...]
            if (type == 2) {

                if (!loaded) {
                    while (!loaded) {
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    }
                }

                if (localData == null) {
                    System.out.println("[Slave] エラー：学習データがまだ用意されていません");
                    return;
                }

                ByteBuffer buf = ByteBuffer.wrap(payload);
                buf.get(); // type

                int round = buf.getInt();

                byte[] modelBytes = new byte[buf.remaining()];
                buf.get(modelBytes);

                MultiLayerNetwork model = null;
                ListDataSetIterator<DataSet> iter = null;

                long tRecv = System.currentTimeMillis();

                try {
                	
                    System.out.println("[Slave] Round " + round + " 学習開始");

                    model = ModelSerializer.restoreMultiLayerNetwork(
                            new ByteArrayInputStream(modelBytes));

                    iter = new ListDataSetIterator<>(localData, batchSize);

                    long tTrain0 = System.currentTimeMillis();
                    for (int e = 0; e < localEpochs; e++) {
                        model.fit(iter);
                        iter.reset();
                    }
                    long trainMs = System.currentTimeMillis() - tTrain0;

                    long tSer0 = System.currentTimeMillis();
                    byte[] updated = serializeModel(model);
                    long serMs = System.currentTimeMillis() - tSer0;

                    // reply: [type=3][round][trainMs][serMs][modelBytes...]
                    byte[] sendPayload = wrapTrainedPayload((byte)3, round, trainMs, serMs, updated);

                    InetAddress ip = InetAddress.getByName(masterIp);
                    KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<>(ip, masterPort);

                    MessageAPI.send(
                        dst,
                        new StandardEnvelope(
                            new AgentAddress(parentId),
                            new StandardContentContainer(sendPayload)
                        )
                    );

                    long totalMs = System.currentTimeMillis() - tRecv;
                    System.out.println("[Slave] Round " + round + " 返送完了 totalMs=" + totalMs
                            + " trainMs=" + trainMs + " serMs=" + serMs);

                } finally {
                    iter = null;
                    if (model != null) {
                        try { model.clear(); } catch (Exception ignore) {}
                        model = null;
                    }
                    Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
                    Nd4j.getMemoryManager().invokeGc();
                    System.gc();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLocalTrainingData() {
        System.out.println("[Slave] CIFAR-10 データ読み込み開始");

        try {
            final int rngSeed = 123;

            DataSetIterator trainIt = new Cifar10DataSetIterator(
                    batchSize, null, DataSetType.TRAIN, null, rngSeed
            );

            List<DataSet> all = new ArrayList<>();
            while (trainIt.hasNext()) {
                DataSet ds = trainIt.next();
                ds.detach();
                all.add(ds);
            }

            int total = all.size();
            int partSize = total / totalParts;

            int start = partIndex * partSize;
            int end = (partIndex == totalParts - 1) ? total : start + partSize;

            localData = new ArrayList<>(all.subList(start, end));
            all.clear();

            System.out.println("[Slave] データ読み込み完了: part " + partIndex + "/" + totalParts
                    + " start=" + start + " end=" + end + " size=" + localData.size());

            Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
            Nd4j.getMemoryManager().invokeGc();
            System.gc();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[Slave] データ読み取りエラー");
        }
    }

    /** reply payload: [type][round(int)][trainMs(long)][serMs(long)][modelBytes...] */
    private byte[] wrapTrainedPayload(byte type, int round, long trainMs, long serMs, byte[] modelBytes) {
        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + 8 + 8 + modelBytes.length);
        buf.put(type);
        buf.putInt(round);
        buf.putLong(trainMs);
        buf.putLong(serMs);
        buf.put(modelBytes);
        return buf.array();
    }

    private static byte[] serializeModel(MultiLayerNetwork model) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ModelSerializer.writeModel(model, baos, true);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public String getStrictName() { return getAgentID(); }
    @Override public String getSimpleName() { return getAgentName(); }
    @Override public void requestStop() {}
}