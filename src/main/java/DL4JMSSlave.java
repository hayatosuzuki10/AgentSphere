import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

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
import primula.api.core.assh.command.demo;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import scheduler2022.Scheduler;

public class DL4JMSSlave extends AbstractAgent implements IMessageListener {

    private String parentId;
    private String masterIp;
    private int masterPort = 55878;
    private String homeIP = IPAddress.myIPAddress;

    private volatile boolean running = true;
    private volatile boolean inTraining = false;
    private volatile boolean canMigrate = false;
    private volatile boolean finish = false;
    

    private final int batchSize = 32;
    private final int localEpochs = 1;

    private boolean loaded = false;

    private int partIndex = 0;
    private int totalParts = 1;

    private List<DataSet> localData = null;

    /* ========= setters ========= */

    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setMasterIp(String masterIp) { this.masterIp = masterIp; }
    public void setMasterPort(int masterPort) { this.masterPort = masterPort; }
    public void setPartIndex(int idx) { this.partIndex = idx; }
    public void setTotalParts(int tot) { this.totalParts = tot; }

    /* ========= run ========= */

    @Override
    public @continuable void run() {

        System.out.println("[DL4JSlave][RUN] START id=" + getAgentID()
                + " ip=" + IPAddress.myIPAddress
                + " part=" + partIndex + "/" + totalParts);

        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        loadLocalTrainingData();
        loaded = true;

        while (running) {
        	
        	if(finish)	break;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            if (canMigrate && !inTraining) {
                System.out.println("[DL4JSlave][MIGRATE] START from=" + IPAddress.myIPAddress);
                canMigrate = false;
                localData = null;
                loaded = false;
                try {
                    MessageAPI.removeMessageListener(this);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                String dest = Scheduler.getNextDestination(this);
                migrate(dest);
                try {
                    MessageAPI.registerMessageListener(this);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                loadLocalTrainingData();
                loaded = true;

                System.out.println("[DL4JSlave][MIGRATE] RESUMED at=" + IPAddress.myIPAddress);
            }
        }

        System.out.println("[DL4JSlave][RUN] FINISH -> migrate home");
        migrate(homeIP);
        demo.reportAgentHistory(getAgentID(), getAgentName(), buildHistoryText());
    }

    /* ========= message receive ========= */

    @Override
    public void receivedMessage(AbstractEnvelope env) {

        System.out.println("[DL4JSlave][RECV] from="
                + " running=" + running
                + " inTraining=" + inTraining
                + " canMigrate=" + canMigrate);

        byte[] payload =
                (byte[]) ((StandardContentContainer) env.getContent()).getContent();

        if (payload == null || payload.length < 1) {
            System.out.println("[DL4JSlave][RECV] EMPTY payload");
            return;
        }

        byte type = payload[0];
        System.out.println("[DL4JSlave][RECV] payload type=" + type
                + " len=" + payload.length);

        if (type != 2) {
            System.out.println("[DL4JSlave][RECV] IGNORE unknown type=" + type);
            return;
        }

        if (!loaded) {
            System.out.println("[DL4JSlave][WAIT] model/data not ready");
            while (!loaded) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }

        if (localData == null) {
            System.out.println("[DL4JSlave][ERROR] localData is NULL");
            return;
        }

        inTraining = true;

        try {
            ByteBuffer buf = ByteBuffer.wrap(payload);
            buf.get(); // type

            int round = buf.getInt();
            int totalRounds = buf.getInt();

            System.out.println("[DL4JSlave][STATE] round=" + round
                    + "/" + totalRounds
                    + " part=" + partIndex + "/" + totalParts);

            byte[] modelBytes = new byte[buf.remaining()];
            buf.get(modelBytes);

            long tRecv = System.currentTimeMillis();

            System.out.println("[DL4JSlave][TRAIN] START round=" + round);

            MultiLayerNetwork model =
                    ModelSerializer.restoreMultiLayerNetwork(
                            new ByteArrayInputStream(modelBytes));

            ListDataSetIterator<DataSet> iter =
                    new ListDataSetIterator<>(localData, batchSize);

            long tTrain0 = System.currentTimeMillis();
            for (int e = 0; e < localEpochs; e++) {
                model.fit(iter);
                iter.reset();
            }
            long trainMs = System.currentTimeMillis() - tTrain0;

            System.out.println("[DL4JSlave][TRAIN] END round=" + round
                    + " trainMs=" + trainMs);

            long tSer0 = System.currentTimeMillis();
            byte[] updated = serializeModel(model);
            long serMs = System.currentTimeMillis() - tSer0;

            byte[] sendPayload =
                    wrapTrainedPayload((byte) 3, round, trainMs, serMs, updated);

            InetAddress ip = InetAddress.getByName(homeIP);
            KeyValuePair<InetAddress, Integer> dst =
                    new KeyValuePair<>(ip, masterPort);

            long crc = crc32(updated);
            System.out.println("[DL4JSlave][SEND] START round=" + round
                    + " bytes=" + updated.length
                    + " crc=" + crc
                    + " targetIP=" + homeIP
                    + " targetID=" + parentId);

            StandardEnvelope newEnv = new StandardEnvelope(
                    new AgentAddress(parentId),
                    new StandardContentContainer(sendPayload)
            );
            MessageAPI.send(
                    dst,
                    newEnv
            );

            System.out.println("[DL4JSlave][SEND] DONE round=" + round);

            long totalMs = System.currentTimeMillis() - tRecv;
            System.out.println("[DL4JSlave][ROUND] COMPLETE round=" + round
                    + " totalMs=" + totalMs);

            if (round > totalRounds - 1) {
            	finish = true;
                System.out.println("[DL4JSlave][STOP] finished all rounds "
                        + round + "/" + totalRounds + "finish flag = " + finish);
                running = false;
                canMigrate = false;
            } else {
                canMigrate = true;
            }

        } catch (Exception e) {
            System.out.println("[DL4JSlave][ERROR] exception during training");
            e.printStackTrace();
        } finally {
            inTraining = false;
            Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
            Nd4j.getMemoryManager().invokeGc();
            System.gc();
        }
    }

    /* ========= utils ========= */

    static long crc32(byte[] b) {
        CRC32 c = new CRC32();
        c.update(b);
        return c.getValue();
    }

    private void loadLocalTrainingData() {
        System.out.println("[DL4JSlave][DATA] loading CIFAR-10");

        try {
            final int rngSeed = 123;

            DataSetIterator trainIt =
                    new Cifar10DataSetIterator(
                            batchSize, null, DataSetType.TRAIN, null, rngSeed);

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

            System.out.println("[DL4JSlave][DATA] loaded part "
                    + partIndex + "/" + totalParts
                    + " size=" + localData.size());

        } catch (Exception e) {
            System.out.println("[DL4JSlave][ERROR] data load failed");
            e.printStackTrace();
        }
    }

    private byte[] wrapTrainedPayload(
            byte type, int round, long trainMs, long serMs, byte[] modelBytes) {

        ByteBuffer buf =
                ByteBuffer.allocate(1 + 4 + 8 + 8 + modelBytes.length);
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