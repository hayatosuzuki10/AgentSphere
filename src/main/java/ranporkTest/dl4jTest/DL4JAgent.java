package ranporkTest.dl4jTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.javaflow.api.continuable;
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
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.KeyValuePair;
import ranporkTest.util.CountLoadAverageAgent;
import ranporkTest.util.TimerAgent;
import scheduler2022.RecommendedDest;

/* MnistDataSetIteratorがConnection timed out: connectの例外をはく場合
 * 十中八九"JVMの"プロキシ越えができていない
 * 実行構成のvm引数に以下の記述を加える(プロキシ周りの設定)
-Dhttp.proxyHost=133.220.149.120 -Dhttp.proxyPort=8080
-Dhttps.proxyHost=133.220.149.120 -Dhttps.proxyPort=8080
-Dhttp.nonProxyHosts=localhost|127.0.0.1|133.220.*.*
 */

public class DL4JAgent extends AbstractAgent {

	private static Logger log = LoggerFactory.getLogger(DL4JAgent.class);

	MultiLayerNetwork generateMyfit(int numEpochs, int batchSize) {

		int nChannels = 1; // Number of input channels
		int outputNum = 10; // The number of possible outcomes
		int seed = 123; //
		/*
		 * Create an iterator using the batch size for one iteration
		 */
		log.info("Load data....");
		Random random = new Random();

		DataSetIterator mnistTrain = null;
		try {
			mnistTrain = new MnistDataSetIterator(batchSize, true, random.nextInt(30000));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		DataSetIterator mnistTest = null;
		try {
			mnistTest = new MnistDataSetIterator(batchSize, false, random.nextInt(30000));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		MultiLayerNetwork model = modelFit(nChannels, outputNum, seed);

		log.info("Train model...");
		model.setListeners(new ScoreIterationListener(10),
				new EvaluativeListener(mnistTest, 1, InvocationType.EPOCH_END)); // Print score every 10 iterations and
																																	// evaluate on test set every epoch
		model.fit(mnistTrain, numEpochs);
		model.setListeners(new ArrayList<>());

		return model;
	}

	MultiLayerNetwork modelFit(int nChannels, int outputNum, int seed) {
		log.info("Build model....");
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).l2(0.0005)
				.weightInit(WeightInit.XAVIER).updater(new Adam(1e-3)).list().layer(new ConvolutionLayer.Builder(5, 5)
						// nIn and nOut specify depth. nIn here is the nChannels and nOut is the number
						// of filters to be applied
						.nIn(nChannels).stride(1, 1).nOut(20).activation(Activation.IDENTITY).build())
				.layer(new SubsamplingLayer.Builder(PoolingType.MAX).kernelSize(2, 2).stride(2, 2).build())
				.layer(new ConvolutionLayer.Builder(5, 5)
						// Note that nIn need not be specified in later layers
						.stride(1, 1).nOut(50).activation(Activation.IDENTITY).build())
				.layer(new SubsamplingLayer.Builder(PoolingType.MAX).kernelSize(2, 2).stride(2, 2).build())
				.layer(new DenseLayer.Builder().activation(Activation.RELU).nOut(500).build())
				.layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nOut(outputNum)
						.activation(Activation.SOFTMAX).build())
				.setInputType(InputType.convolutionalFlat(28, 28, 1)) // See note below
				.build();

		MultiLayerNetwork model = new MultiLayerNetwork(conf);
		model.init();

		return model;
	}

	MultiLayerNetwork myfit(MultiLayerNetwork model, int numEpochs, int batchSize) {
		Random random = new Random();
		DataSetIterator mnistTrain = null;
		try {
			mnistTrain = new MnistDataSetIterator(batchSize, true, random.nextInt(30000));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		DataSetIterator mnistTest = null;
		try {
			mnistTest = new MnistDataSetIterator(batchSize, false, random.nextInt(30000));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		model.setListeners(new ScoreIterationListener(10),
				new EvaluativeListener(mnistTest, 1, InvocationType.EPOCH_END)); // Print score every 10 iterations and
																																	// evaluate on test set every epoch
		model.fit(mnistTrain, numEpochs);
		model.setListeners(new ArrayList<>());
		return model;

	}

	byte[] Savemodel(MultiLayerNetwork model) {
		ByteArrayOutputStream baOutStr = new ByteArrayOutputStream();
		try {
			ModelSerializer.writeModel(model, baOutStr, true);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return baOutStr.toByteArray();
	}

	MultiLayerNetwork loadmodel(byte[] arr) {
		ByteArrayInputStream baInStr = new ByteArrayInputStream(arr);
		try {
			return ModelSerializer.restoreMultiLayerNetwork(baInStr);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			throw new IllegalArgumentException();
		}
	}

	public @continuable void run() {
		log.info("Starting DL4JAgent");
		{
			KeyValuePair<InetAddress, Integer> ToSlave = null;
			ToSlave = new KeyValuePair<InetAddress, Integer>(getmyIP(), 55878);
			AgentAPI.migration(ToSlave, new TimerAgent());
			AgentAPI.migration(ToSlave, new CountLoadAverageAgent());
		}
		int batchSize = 64; // Test batch size
		int nEpochs = 1; // Number of training epochs

		byte[] arr = Savemodel(generateMyfit(nEpochs, batchSize));

		migrate(RecommendedDest.recomDest(this.getAgentName()));

		myfit(loadmodel(arr), nEpochs, batchSize);

		log.info("****************Example finished********************");

		log.info("finish");
		{
			KeyValuePair<InetAddress, Integer> ToSlave = null;
			ToSlave = new KeyValuePair<InetAddress, Integer>(getmyIP(), 55878);
			AgentAPI.migration(ToSlave, new TimerAgent());
			AgentAPI.migration(ToSlave, new CountLoadAverageAgent());
		}

	}

	@Override
	public void requestStop() {
		// TODO Auto-generated method stub

	}

}
