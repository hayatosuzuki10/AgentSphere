import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.javaflow.api.continuable;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.InvocationType;
import org.deeplearning4j.optimize.listeners.EvaluativeListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import primula.agent.AbstractAgent;

//LeNetMNIST
//https://github.com/deeplearning4j/deeplearning4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/quickstart/modeling/convolution/LeNetMNIST.java

public class DL4JtestMNIST5LayerB extends AbstractAgent { 

    private static Logger log = LoggerFactory.getLogger(DL4JtestMNIST5LayerB.class);

	public @continuable void run() {
		log.info("Starting Migration\n");

		this.migrate();
		System.out.println("Migrated");

		Random rnd = new Random();
		
		//int nChannels = 1; // Number of input channels
		//int outputNum = 10; // The number of possible outcomes
		int batchSize = 64; // Test batch size
		int nEpochs = 1; // Number of training epochs
		//int seed = 123; //
		int seeds = rnd.nextInt(50000);
		System.out.println("seeds="+seeds+"\n");
		/*
		 * Create an iterator using the batch size for one iteration
		 */
		log.info("Load data....");
		DataSetIterator mnistTrain = null;
		try {
			mnistTrain = new MnistDataSetIterator(batchSize, true, seeds);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		DataSetIterator mnistTest = null;
		try {
			mnistTest = new MnistDataSetIterator(batchSize, false, seeds);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		/*
		 * Construct the neural network
		 */
		log.info("Build model....");

		MultiLayerNetwork model = null;
		try {
			model = MultiLayerNetwork.load(new File("C:/DeepLearning4JExamples/Otameshi4.zip"), true);
		} catch (IOException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}

		//model.init();

		log.info("Train model...");
		model.setListeners(new ScoreIterationListener(10),
				new EvaluativeListener(mnistTest, 1, InvocationType.EPOCH_END)); // Print score every 10 iterations and
																					// evaluate on test set every epoch
		model.fit(mnistTrain, nEpochs);

		String path = "C:/DeepLearning4JExamples/Otameshi6.zip";//FilenameUtils.concat(System.getProperty("java.io.tmpdir"), "lenetmnist.zip");

		try {
			model.save(new File(path), true);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		log.info("****************Example finished********************");

		log.info("finish");
	}

	@Override
	public void requestStop() {
		// TODO Auto-generated method stub

	}

}