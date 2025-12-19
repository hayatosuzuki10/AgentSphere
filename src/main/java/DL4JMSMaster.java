import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
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

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;

public class DL4JMSMaster extends AbstractAgent implements IMessageListener {

    final int height = 32, width = 32, channels = 3, outputNum = 10;
    final int batchSize = 32, rngSeed = 123;
    private static final int NUM_SLAVES = 2, ROUNDS = 10; //ROUNDSが学習回数になります。この場合だと10回
    private int currentRound = 0;

    private final List<KeyValuePair<InetAddress,Integer>> slaveTargets = new ArrayList<>();
    private final List<String> slaveIds = new ArrayList<>();
    
    private static class ModelState {
        INDArray params;        // θ
        INDArray updaterState;  // Adam の m, v などが入っている stateViewArray
    }
    
    private final Map<String, ModelState> receivedStates = new ConcurrentHashMap<>();
    private MultiLayerNetwork masterModel;

    public @continuable void run() {

        System.out.println("[Master] 起動: " + getAgentName());
        
        long tMasterStart = System.currentTimeMillis();
        
        try { MessageAPI.registerMessageListener(this); } //リスナ登録
        catch (Exception e) { e.printStackTrace(); return; }

        try {
        	//---------------------------------------
        	//以下エージェントの生成+migration処理
        	//---------------------------------------
        	
        	//①IPアドレスを指定し、送信先を静的に決める
            slaveTargets.add(new KeyValuePair<>(Inet4Address.getByName("172.28.15.74"), 55878));
            slaveTargets.add(new KeyValuePair<>(Inet4Address.getByName("172.28.15.68"), 55878));

            //②エージェントの生成+各種設定
            DL4JMSSlave s1 = new DL4JMSSlave();  //エージェントの生成
            s1.setParentId(getAgentID());        //親のエージェントID
            s1.setMasterIp(getMyIpForSlave());   //親のIPアドレス設定(これも静的に指定してます。一番下にあります)
            s1.setPartIndex(0);                  //学習データの参照開始場所
            s1.setTotalParts(NUM_SLAVES);      //Slaveの数学習データを分割する

            DL4JMSSlave s2 = new DL4JMSSlave();  //上と同じ
            s2.setParentId(getAgentID());
            s2.setMasterIp(getMyIpForSlave());
            s2.setPartIndex(1);            
            s2.setTotalParts(NUM_SLAVES);  

            slaveIds.add(s1.getAgentID());  //SlaveのエージェントIDを取得
            slaveIds.add(s2.getAgentID());

            //③各マシンへmigration
            AgentAPI.migration(slaveTargets.get(0), s1);
            AgentAPI.migration(slaveTargets.get(1), s2);

        } catch (Exception e) { e.printStackTrace(); return; }


        //---------------------------------------
    	//学習モデルの構築
    	//---------------------------------------
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(rngSeed)
                .updater(new Adam(0.001))
                .weightInit(WeightInit.RELU)          // ReLU 向け初期化
                .list()
                // --- Block 1 ---
                .layer(new ConvolutionLayer.Builder(3,3)
                    .nIn(channels)
                    .nOut(32)
                    .stride(1,1)
                    .activation(Activation.IDENTITY)   // BN後にReLUをかける
                    .build())
                .layer(new BatchNormalization())
                .layer(new ActivationLayer(Activation.RELU))

                .layer(new ConvolutionLayer.Builder(3,3)
                    .nOut(32)
                    .stride(1,1)
                    .activation(Activation.IDENTITY)
                    .build())
                .layer(new BatchNormalization())
                .layer(new ActivationLayer(Activation.RELU))

                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                    .kernelSize(2,2).stride(2,2).build())

                // --- Block 2 ---
                .layer(new ConvolutionLayer.Builder(3,3)
                    .nOut(64)
                    .stride(1,1)
                    .activation(Activation.IDENTITY)
                    .build())
                .layer(new BatchNormalization())
                .layer(new ActivationLayer(Activation.RELU))

                .layer(new ConvolutionLayer.Builder(3,3)
                    .nOut(64)
                    .stride(1,1)
                    .activation(Activation.IDENTITY)
                    .build())
                .layer(new BatchNormalization())
                .layer(new ActivationLayer(Activation.RELU))

                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                    .kernelSize(2,2).stride(2,2).build())

                // --- 全結合 + Dropout ---
                .layer(new DenseLayer.Builder()
                    .nOut(256)
                    .activation(Activation.RELU)
                    .build())
                .layer(new DropoutLayer(0.5))  // 50% Dropout

                // --- 出力層 ---
                .layer(new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                    .activation(Activation.SOFTMAX)
                    .nOut(outputNum)
                    .build())

                .setInputType(InputType.convolutional(height, width, channels))
                .build();

        masterModel = new MultiLayerNetwork(conf);
        masterModel.init();


        //---------------------------------------
    	//ラウンドの開始
    	//---------------------------------------
        long tRoundsStart = System.currentTimeMillis();
        for (currentRound = 1; currentRound <= ROUNDS; currentRound++) {

            System.out.println("\n[Master] ==== Round " + currentRound + " ====");
            receivedStates.clear();

            long tModelSend = System.currentTimeMillis();
            
            //①学習モデルを全Slaveへ送信
            byte[] modelBytes = serializeModel(masterModel);
            broadcastModelToAll(modelBytes);
            
            long tModelSendEnd = System.currentTimeMillis();
            System.out.println("[Time] Round " + currentRound + " モデル送信時間: " 
                               + (tModelSendEnd - tModelSend) + " ms");
            
            long tWaitStart = System.currentTimeMillis();

            //②全Slaveで、学習済みの学習モデルを受け取るか、300秒たつまで待機
            long deadline = System.currentTimeMillis() + 300_000; //タイムアウトの時間(この場合300秒)
            while (System.currentTimeMillis() < deadline) {
                if (receivedStates.size() >= NUM_SLAVES) break;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            
            long tWaitEnd = System.currentTimeMillis();
            System.out.println("[Time] Round " + currentRound + " Slave応答待ち時間: " 
                               + (tWaitEnd - tWaitStart) + " ms");

            //③Slaveから受け取った学習モデルの平均化処理
            //  ここで平均化した学習モデルを次のラウンドで配布する
            if (!receivedStates.isEmpty()) {

                long tAvgStart = System.currentTimeMillis();

                INDArray avgParams = null;
                INDArray avgUpdater = null;
                int cnt = 0;

                for (ModelState st : receivedStates.values()) {
                    if (avgParams == null) {
                        avgParams = st.params.dup();
                        if (st.updaterState != null)
                            avgUpdater = st.updaterState.dup();
                    } else {
                        avgParams.addi(st.params);
                        if (avgUpdater != null && st.updaterState != null)
                            avgUpdater.addi(st.updaterState);
                    }
                    cnt++;
                }

                avgParams.divi(cnt);
                masterModel.setParams(avgParams);

                if (avgUpdater != null && masterModel.getUpdater() != null) {
                    avgUpdater.divi(cnt);

                    masterModel.getUpdater().setStateViewArray(
                            masterModel, avgUpdater, false);
                }

                long tAvgEnd = System.currentTimeMillis();
                System.out.println("[Time] Round " + currentRound + " モデル平均化時間: "
                        + (tAvgEnd - tAvgStart) + " ms");

                System.out.println("[Master] 平均完了 (" + cnt + "台)");
            }
        }
        
        long tRoundsEnd = System.currentTimeMillis();
        System.out.println("[Time] 全ラウンド学習時間: " + (tRoundsEnd - tRoundsStart) + " ms");


        //----------------------
        //学習モデルの評価
        //----------------------
        System.out.println("\n[Master] CIFAR-10 評価開始");
        long tEvalStart = System.currentTimeMillis();
        
        //①評価用の学習データをロード
        DataSetIterator test = new Cifar10DataSetIterator(
                batchSize,
                null,
                DataSetType.TEST,
                null,
                rngSeed
        );
        
        //②評価＋ログに出力
        Evaluation eval = masterModel.evaluate(test);
        System.out.println(eval.stats());
        
        long tEvalEnd = System.currentTimeMillis();
        System.out.println("[Time] 評価時間: " + (tEvalEnd - tEvalStart) + " ms");

        SystemInfo si = new SystemInfo();
        for (GraphicsCard gpu : si.getHardware().getGraphicsCards()) {
            System.out.println("GPU: " + gpu.getName() + " / VRAM " + gpu.getVRam()/(1024*1024) + "MB");
        }
        long tMasterEnd = System.currentTimeMillis();
        System.out.println("[Time] Master 全体処理時間: " + (tMasterEnd - tMasterStart) + " ms");
    }

    //-------------------------------------------------------
    //Slaveから学習済み学習モデルを受け取り、ModelStateへ格納
    //-------------------------------------------------------
    @Override
    public void receivedMessage(AbstractEnvelope env) {
        try {
            byte[] payload = (byte[]) ((StandardContentContainer) env.getContent()).getContent();

            byte type = payload[0];
            byte[] body = java.util.Arrays.copyOfRange(payload, 1, payload.length);

            if (type == 3) {
                MultiLayerNetwork m =
                        ModelSerializer.restoreMultiLayerNetwork(new ByteArrayInputStream(body));

                ModelState st = new ModelState();
                st.params = m.params().dup();

                if (m.getUpdater() != null && m.getUpdater().getStateViewArray() != null)
                    st.updaterState = m.getUpdater().getStateViewArray().dup();

                receivedStates.put("recv-" + System.nanoTime(), st);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    //学習モデルを全Slaveに送信する関数
    private void broadcastModelToAll(byte[] modelBytes) {
        for (int i = 0; i < slaveTargets.size(); i++) {

            byte[] payload = wrapWithType((byte)2, modelBytes); // type=2 → model

            MessageAPI.send(
                slaveTargets.get(i),
                new StandardEnvelope(new AgentAddress(slaveIds.get(i)),
                        new StandardContentContainer(payload))
            );
        }
        System.out.println("[Master] モデル送信完了");
    }


    //シリアライズした学習モデルを送信できる形にする関数
    private byte[] wrapWithType(byte type, byte[] body) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(type);  // 1バイトのタイプ
        try { baos.write(body); }
        catch (IOException e) { throw new RuntimeException(e); }
        return baos.toByteArray();
    }

    
    //学習モデルをシリアライズする関数
    private static byte[] serializeModel(MultiLayerNetwork model) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ModelSerializer.writeModel(model, baos, true);
            return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }


    


    @Override public String getStrictName() { return this.getAgentID(); }
    @Override public String getSimpleName() { return this.getAgentName(); }
    @Override public void requestStop() {}

    private String getMyIpForSlave() { return "172.28.15.94"; }  //MasterのIPアドレスを静的に指定
}

