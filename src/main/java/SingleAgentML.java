import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.PoolingType;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.InvocationType;
import org.deeplearning4j.optimize.listeners.EvaluativeListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import primula.agent.AbstractAgent;

public class SingleAgentML extends AbstractAgent {

    private static Logger log = LoggerFactory.getLogger(SingleAgentML.class);

    @Override
    public void runAgent() {
        int batchSize = 64; // テスト用バッチサイズ
        int numEpochs = 1; // トレーニングエポック数

        // モデルのトレーニング
        MultiLayerNetwork model = trainModel(batchSize, numEpochs);

        // トレーニングしたモデルを保存
        byte[] serializedModel = saveModel(model);

        // トレーニング後のモデルを使用した推論などをここで実行
        performInference(model);

        // 保存したモデルを再ロードして利用する場合
        MultiLayerNetwork loadedModel = loadModel(serializedModel);

        log.info("****************Example finished********************");
        log.info("Finish");
    }

    private MultiLayerNetwork trainModel(int batchSize, int numEpochs) {
        int nChannels = 1; // 入力チャンネル数
        int outputNum = 10; // 出力クラス数
        int seed = 123; // 乱数のシード

     // MNISTデータセットの読み込み
        DataSetIterator mnistTrain = null;
        try {
            mnistTrain = new MnistDataSetIterator(batchSize, true, new Random(seed).nextInt(30000));
        } catch (IOException e) {
            e.printStackTrace();
        }

        DataSetIterator mnistTest = null;
        try {
            mnistTest = new MnistDataSetIterator(batchSize, false, new Random(seed).nextInt(30000));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // モデルの構築
        MultiLayerNetwork model = buildModel(nChannels, outputNum, seed);

        // モデルのトレーニング
        log.info("Train model...");
        model.setListeners(new ScoreIterationListener(10),
                new EvaluativeListener(mnistTest, 1, InvocationType.EPOCH_END)); // 10回ごとにスコアを表示し、エポックごとにテストセットで評価
        model.fit(mnistTrain, numEpochs);
        model.setListeners(new ArrayList<>());

        return model;
    }

    private MultiLayerNetwork buildModel(int nChannels, int outputNum, int seed) {
        // モデルの構築
        log.info("Build model....");
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).l2(0.0005)
                .weightInit(WeightInit.XAVIER).updater(new Adam(1e-3)).list()
                .layer(new ConvolutionLayer.Builder(5, 5)
                        .nIn(nChannels).stride(1, 1).nOut(20).activation(Activation.IDENTITY).build())
                .layer(new SubsamplingLayer.Builder(PoolingType.MAX).kernelSize(2, 2).stride(2, 2).build())
                .layer(new ConvolutionLayer.Builder(5, 5)
                        .stride(1, 1).nOut(50).activation(Activation.IDENTITY).build())
                .layer(new SubsamplingLayer.Builder(PoolingType.MAX).kernelSize(2, 2).stride(2, 2).build())
                .layer(new DenseLayer.Builder().activation(Activation.RELU).nOut(500).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(outputNum).activation(Activation.SOFTMAX).build())
                .setInputType(InputType.convolutionalFlat(28, 28, 1)).build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        return model;
    }
    
    
    private void performInference(MultiLayerNetwork model) {
        // MNISTテストデータセットからランダムな画像を取得
        DataSetIterator mnistTest = null;
        try {
            mnistTest = new MnistDataSetIterator(1, false, new Random(123).nextInt(30000));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataSet testData = mnistTest.next();

        // 画像データを取得
        INDArray features = testData.getFeatures();

        // 推論を行い、結果を取得
        INDArray output = model.output(features);

        // 結果を出力
        log.info("Predictions:");
        log.info(output.toString());
    }

    private byte[] saveModel(MultiLayerNetwork model) {
        // モデルの保存
        log.info("Save model...");
        ByteArrayOutputStream baOutStr = new ByteArrayOutputStream();
        try {
            ModelSerializer.writeModel(model, baOutStr, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baOutStr.toByteArray();
    }

    private MultiLayerNetwork loadModel(byte[] serializedModel) {
        // 保存したモデルの読み込み
        log.info("Load model...");
        MultiLayerNetwork loadedModel = null;
        try {
            loadedModel = ModelSerializer.restoreMultiLayerNetwork(new ByteArrayInputStream(serializedModel), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loadedModel;
    }

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
		
	}
}
