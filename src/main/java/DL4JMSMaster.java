import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.javaflow.api.continuable;
import org.deeplearning4j.datasets.fetchers.DataSetType;
import org.deeplearning4j.datasets.iterator.impl.Cifar10DataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.DropoutLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.assh.command.demo;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import scheduler2022.util.DHTutil;

public class DL4JMSMaster extends AbstractAgent implements IMessageListener {

    private static final int NUM_SLAVES  = 2;
    private static final int ROUNDS      = 10;
    private static final int BATCH_SIZE  = 32;

    /** Primula Message の送受信ポート（あなたの環境に合わせて固定） */
    private static final int MSG_PORT = 55878;

    /** Masterが生成したSlaveのAgentID */
    private final List<String> slaveAgentIds = new ArrayList<>();

    /** roundごとに「どのslaveから何が返ってきたか」 */
    private final Map<String, SlaveReport> lastReportsBySlave = new ConcurrentHashMap<>();

    /** 受信したモデル（平均化用） */
    private final Map<String, ModelState> receivedStates = new ConcurrentHashMap<>();

    private MultiLayerNetwork masterModel;

    private static class ModelState {
        INDArray params;
        INDArray updaterState;
    }

    /** slaveから返ってきた時間情報 */
    private static class SlaveReport {
        int round;
        long trainMs;
        long serMs;
        long recvAt;
    }

    @continuable
    public void run() {
        long totalStart = System.currentTimeMillis();

        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
            safeReportToDemo("DL4JMSMaster FAILED: listener register error: " + e);
            return;
        }

        // -------------------------
        // Slave 起動（配置/移動はScheduler任せ）
        // -------------------------
        slaveAgentIds.clear();

        for (int i = 0; i < NUM_SLAVES; i++) {
            DL4JMSSlave slave = new DL4JMSSlave();
            slave.setParentId(getAgentID());
            slave.setMasterIp(IPAddress.myIPAddress);
            slave.setMasterPort(MSG_PORT);
            slave.setPartIndex(i);
            slave.setTotalParts(NUM_SLAVES);

            AgentAPI.runAgent(slave);
            slaveAgentIds.add(slave.getAgentID());
        }

        // -------------------------
        // モデル構築
        // -------------------------
        masterModel = buildModel();

        // -------------------------
        // レポート文字列（最後にdemoへ渡す）
        // -------------------------
        StringBuilder report = new StringBuilder();
        report.append("==== DL4J Federated Learning Result ====\n");
        report.append("MasterAgentID : ").append(getAgentID()).append("\n");
        report.append("MasterIP      : ").append(IPAddress.myIPAddress).append("\n");
        report.append("MSG_PORT      : ").append(MSG_PORT).append("\n");
        report.append("NUM_SLAVES    : ").append(NUM_SLAVES).append("\n");
        report.append("ROUNDS        : ").append(ROUNDS).append("\n");
        report.append("BATCH_SIZE    : ").append(BATCH_SIZE).append("\n");
        report.append("StartTime     : ")
              .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(totalStart)))
              .append("\n\n");

        long totalSendMs = 0, totalWaitMs = 0, totalAvgMs = 0;

        // -------------------------
        // 学習ラウンド
        // -------------------------
        for (int round = 1; round <= ROUNDS; round++) {
            receivedStates.clear();
            lastReportsBySlave.clear();

            long tSend0 = System.currentTimeMillis();
            byte[] modelBytes = serializeModel(masterModel);
            sendModelToAllSlaves(round, modelBytes);
            long sendMs = System.currentTimeMillis() - tSend0;

            long tWait0 = System.currentTimeMillis();
            waitForSlaves(round);
            long waitMs = System.currentTimeMillis() - tWait0;

            long tAvg0 = System.currentTimeMillis();
            averageModels();
            long avgMs = System.currentTimeMillis() - tAvg0;

            totalSendMs += sendMs;
            totalWaitMs += waitMs;
            totalAvgMs  += avgMs;

            report.append("[Round ").append(round).append("]\n");
            report.append("  receivedModels: ").append(receivedStates.size()).append("/").append(NUM_SLAVES).append("\n");
            report.append("  sendMs        : ").append(sendMs).append("\n");
            report.append("  waitMs        : ").append(waitMs).append("\n");
            report.append("  avgMs         : ").append(avgMs).append("\n");

            // slave timings（返ってきたものだけ）
            for (String sid : slaveAgentIds) {
                SlaveReport sr = lastReportsBySlave.get(sid);
                if (sr == null) {
                    report.append("  slave ").append(sid).append(": (no timing)\n");
                } else {
                    report.append("  slave ").append(sid)
                          .append(": trainMs=").append(sr.trainMs)
                          .append(" serMs=").append(sr.serMs)
                          .append("\n");
                }
            }
            report.append("\n");
        }

        long trainEnd = System.currentTimeMillis();

        // -------------------------
        // 評価
        // -------------------------
        double accuracy = evaluateModel();
        long end = System.currentTimeMillis();

        report.append("Accuracy            : ").append(accuracy).append("\n");
        report.append("Training Time(ms)   : ").append(trainEnd - totalStart).append("\n");
        report.append("Total Time(ms)      : ").append(end - totalStart).append("\n");
        report.append("TotalSendMs(sum)    : ").append(totalSendMs).append("\n");
        report.append("TotalWaitMs(sum)    : ").append(totalWaitMs).append("\n");
        report.append("TotalAvgMs(sum)     : ").append(totalAvgMs).append("\n");
        report.append("EndTime             : ")
              .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(end)))
              .append("\n");

        System.out.println(report.toString());
        safeReportToDemo(report.toString());
    }

    /* =======================
     * 受信
     * ======================= */
    @Override
    public void receivedMessage(AbstractEnvelope env) {
        try {
            byte[] payload =
                    (byte[]) ((StandardContentContainer) env.getContent()).getContent();

            if (payload == null || payload.length < 1) return;

            byte type = payload[0];

            // type=3: [type][round(int)][trainMs(long)][serMs(long)][modelBytes...]
            if (type == 3) {
                ByteBuffer buf = ByteBuffer.wrap(payload);
                buf.get(); // type

                int round = buf.getInt();
                long trainMs = buf.getLong();
                long serMs = buf.getLong();

                byte[] modelBytes = new byte[buf.remaining()];
                buf.get(modelBytes);

                MultiLayerNetwork model =
                        ModelSerializer.restoreMultiLayerNetwork(new ByteArrayInputStream(modelBytes));

                ModelState state = new ModelState();
                state.params = model.params().dup();
                if (model.getUpdater() != null && model.getUpdater().getStateViewArray() != null) {
                    state.updaterState = model.getUpdater().getStateViewArray().dup();
                }

                receivedStates.put("recv-" + System.nanoTime(), state);

                // 送ってきたslaveIdは env からは取れない場合があるので、
                // 現実的には「受信順でOK」で十分だが、ここでは “最後に更新されたslave” として記録する
                // → ちゃんと slaveId を付けたいなら payload に slaveId を入れる
                SlaveReport sr = new SlaveReport();
                sr.round = round;
                sr.trainMs = trainMs;
                sr.serMs = serMs;
                sr.recvAt = System.currentTimeMillis();

                // 近い代替：roundごとに「未記録のslaveへ順に埋める」
                for (String sid : slaveAgentIds) {
                    if (!lastReportsBySlave.containsKey(sid)) {
                        lastReportsBySlave.put(sid, sr);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =======================
     * 送信
     * ======================= */
    private void sendModelToAllSlaves(int round, byte[] modelBytes) {
        byte[] payload = wrapMasterPayload((byte) 2, round, modelBytes);

        for (String agentId : slaveAgentIds) {
            try {
                var info = DHTutil.getAgentInfo(agentId);
                if (info == null || info.ipAddress == null) continue;

                InetAddress ip = InetAddress.getByName(info.ipAddress.trim());
                KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<>(ip, MSG_PORT);

                MessageAPI.send(
                        dst,
                        new StandardEnvelope(
                                new AgentAddress(agentId),
                                new StandardContentContainer(payload))
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** master→slave payload: [type][round(int)][modelBytes...] */
    private byte[] wrapMasterPayload(byte type, int round, byte[] modelBytes) {
        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + modelBytes.length);
        buf.put(type);
        buf.putInt(round);
        buf.put(modelBytes);
        return buf.array();
    }

    private void waitForSlaves(int round) {
        long deadline = System.currentTimeMillis() + 300_000;
        while (System.currentTimeMillis() < deadline) {
            if (receivedStates.size() >= NUM_SLAVES) break;
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
    }

    private void averageModels() {
        if (receivedStates.isEmpty()) return;

        INDArray avgParams = null;
        INDArray avgUpdater = null;
        int count = 0;

        for (ModelState st : receivedStates.values()) {
            if (avgParams == null) {
                avgParams = st.params.dup();
                if (st.updaterState != null) avgUpdater = st.updaterState.dup();
            } else {
                avgParams.addi(st.params);
                if (avgUpdater != null && st.updaterState != null) avgUpdater.addi(st.updaterState);
            }
            count++;
        }

        avgParams.divi(count);
        masterModel.setParams(avgParams);

        if (avgUpdater != null && masterModel.getUpdater() != null) {
            avgUpdater.divi(count);
            masterModel.getUpdater().setStateViewArray(masterModel, avgUpdater, false);
        }
    }

    /* =======================
     * 評価
     * ======================= */
    private double evaluateModel() {
        try {
            DataSetIterator test =
                    new Cifar10DataSetIterator(BATCH_SIZE, null, DataSetType.TEST, null, 123);
            Evaluation eval = masterModel.evaluate(test);
            System.out.println(eval.stats());
            return eval.accuracy();
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    /* =======================
     * モデル構築
     * ======================= */
    private MultiLayerNetwork buildModel() {
        final int height = 32, width = 32, channels = 3, outputNum = 10;

        MultiLayerConfiguration conf =
                new NeuralNetConfiguration.Builder()
                        .seed(123)
                        .updater(new Adam(0.001))
                        .weightInit(WeightInit.RELU)
                        .list()
                        .layer(new ConvolutionLayer.Builder(3,3)
                                .nIn(channels).nOut(32)
                                .activation(Activation.IDENTITY).build())
                        .layer(new BatchNormalization())
                        .layer(new ActivationLayer(Activation.RELU))

                        .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                                .kernelSize(2,2).stride(2,2).build())

                        .layer(new DenseLayer.Builder()
                                .nOut(256).activation(Activation.RELU).build())
                        .layer(new DropoutLayer(0.5))

                        .layer(new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                                .activation(Activation.SOFTMAX)
                                .nOut(outputNum).build())
                        .setInputType(InputType.convolutional(height, width, channels))
                        .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        return net;
    }

    private static byte[] serializeModel(MultiLayerNetwork model) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ModelSerializer.writeModel(model, baos, true);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void safeReportToDemo(String resultText) {
        try {
            demo.reportMasterFinished(getAgentID(), resultText);
        } catch (Throwable t) {
            System.err.println("[DL4JMSMaster] report to demo failed: " + t);
        }
    }

    @Override public String getStrictName() { return getAgentID(); }
    @Override public String getSimpleName() { return getAgentName(); }
    @Override public void requestStop() {}
}