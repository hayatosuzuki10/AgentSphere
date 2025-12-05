import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
//import org.deeplearning4j.examples.quickstart.modeling.convolution.LeNetMNIST;
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
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import primula.agent.AbstractAgent;

public class CustomAgent extends AbstractAgent {
	private static Logger log = LoggerFactory.getLogger(CustomAgent.class);
	static int batchSize = 64; // テスト用バッチサイズ
    static int nEpochs = 1; // トレーニングエポック数
    static int nChannels = 1; // 入力チャンネル数
    static int outputNum = 10; // 出力クラス数
    static int seed = 123; // 乱数のシード
    public static MultiLayerNetwork loadedModel;
    public CustomAgent() {
        //this.model=null;
    }

    public static void main(String[] args) {
        CustomAgent agent = new CustomAgent();
        DL4JAgent NoMigAgent = new DL4JAgent();  // DL4JAgent
        // マイグレーション
        //NoMigAgent.runAgent();
        agent.migrate("127.0.0.1");
        System.out.println("Migrated");
        // マイグレーション後の機械学習実行
        agent.runAgent();
        NoMigAgent.runAgent();
		//agent.migrate("192.168.1.14");
        // 結果の表示
        
        compareModels(agent,NoMigAgent);
    }

     private static void compareModels(CustomAgent agent, DL4JAgent noMigAgent) {
        // モデルの比較
    	//DataSetIterator mnistTrain = null;
    	DataSetIterator mnistTest  = null;
//    	try {
//			mnistTrain = new MnistDataSetIterator(batchSize, true, 12345);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
        try {
			mnistTest = new MnistDataSetIterator(batchSize, false, 12345);
		} catch (IOException e) {
			e.printStackTrace();
		}
        double accuracyModel1 = loadedModel.evaluate(mnistTest).accuracy();
        double accuracyModel2 = noMigAgent.accuracyModel();

        System.out.println("Model 1 Accuracy: " + accuracyModel1);
        System.out.println("Model 2 Accuracy: " + accuracyModel2);

        if (accuracyModel1 > accuracyModel2) {
            System.out.println("Model 1 is better.");
        } else if (accuracyModel1 < accuracyModel2) {
            System.out.println("Model 2 is better.");
        } else {
            System.out.println("Both models have the same accuracy.");
        }
    }
     public void runAgent() {
         // モデルのトレーニング
         //MultiLayerNetwork model = 

         // トレーニングしたモデルを保存
         byte[] serializedModel = saveModel(trainModel(batchSize, nEpochs));

         // 保存したモデルを再ロードして利用する場合
         loadedModel = loadModel(serializedModel);

         log.info("****************Example finished********************");
         log.info("Finish");
         
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

 	private MultiLayerNetwork trainModel(int batchSize, int numEpochs) {
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
         MultiLayerNetwork model = buildModel();

         // モデルのトレーニング
         log.info("Train model...");
         model.setListeners(new ScoreIterationListener(10),
                 new EvaluativeListener(mnistTest, 1, InvocationType.EPOCH_END)); // 10回ごとにスコアを表示し、エポックごとにテストセットで評価
         model.fit(mnistTrain, numEpochs);
         model.setListeners(new ArrayList<>());

         return model;
 	}

 	private MultiLayerNetwork buildModel() {
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

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
		
	}


	
	
	
	public static class DL4JAgent {
    	private DataSetIterator mnistTrain;
        private MultiLayerNetwork model;
        

        public DL4JAgent() {
            // モデルの初期化などの処理
            this.model = buildModel();
            try {
				this.mnistTrain = new MnistDataSetIterator(batchSize, true, new Random(seed).nextInt(30));
			} catch (IOException e) {
				e.printStackTrace();
			}
        }

        public MultiLayerNetwork getModel() {
            return model;
        }
        
        //モデルのaccuracy
        public double accuracyModel() {
        	double accuracy = model.evaluate(mnistTrain).accuracy();
            return accuracy;
        }

        public void runAgent() {
        	DataSetIterator mnistTest  = null;
            try {
    			mnistTest = new MnistDataSetIterator(batchSize, false, new Random(seed).nextInt(30000));
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
            model.setListeners(new ScoreIterationListener(10),
                    new EvaluativeListener(mnistTest, 1, InvocationType.EPOCH_END));
            model.fit(mnistTrain, nEpochs);
        }

        private MultiLayerNetwork buildModel() {
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
	}
}
