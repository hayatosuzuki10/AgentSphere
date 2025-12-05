import java.io.IOException;

import org.apache.commons.javaflow.api.continuable;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
//import org.deeplearning4j.examples.quickstart.modeling.feedforward.classification.MNISTSingleLayer;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import primula.agent.AbstractAgent;

//MNISTSingleLayer
//https://github.com/deeplearning4j/deeplearning4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/quickstart/modeling/feedforward/classification/MNISTSingleLayer.java

public class DL4JtestMNIST1Layer extends AbstractAgent {

    private static Logger log = LoggerFactory.getLogger(DL4JtestMNIST1Layer.class);

	public @continuable void run() {
		log.info("Starting Migration\n");

		this.migrate();
		// number of rows and columns in the input pictures
		final int numRows = 28;
		final int numColumns = 28;
		int outputNum = 10; // number of output classes

		int batchSize = 128; // batch size for each epoch
		int rngSeed = 123; // random number seed for reproducibility

		int numEpochs = 2; // number of epochs to perform

		// Get the DataSetIterators:
		DataSetIterator mnistTrain = null;
		try {
			mnistTrain = new MnistDataSetIterator(batchSize, true, rngSeed);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		DataSetIterator mnistTest = null;
		try {
			mnistTest = new MnistDataSetIterator(batchSize, false, rngSeed);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		log.info("Build model....");
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(rngSeed) // include a random seed for
																							// reproducibility
				// use stochastic gradient descent as an optimization algorithm
				.updater(new Nesterovs(0.006, 0.9)).l2(1e-4).list().layer(new DenseLayer.Builder() // create the first,
																									// input layer with
																									// xavier
																									// initialization
						.nIn(numRows * numColumns).nOut(1000).activation(Activation.RELU).weightInit(WeightInit.XAVIER)
						.build())
				.layer(new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD) // create hidden layer
						.nIn(1000).nOut(outputNum).activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER).build())
				.build();

		MultiLayerNetwork model = new MultiLayerNetwork(conf);
		model.init();
		// print the score with every 1 iteration
		model.setListeners(new ScoreIterationListener(1));

		log.info("Train model....");
		if(mnistTrain == null) {log.info("ヌルでした");}
		else {
			model.fit(mnistTrain, numEpochs);

		log.info("Evaluate model....");
		Evaluation eval = model.evaluate(mnistTest);
		log.info(eval.stats());
		log.info("****************Example finished********************");

		log.info("finish");}
	}

	@Override
	public void requestStop() {
		// TODO Auto-generated method stub

	}

}
