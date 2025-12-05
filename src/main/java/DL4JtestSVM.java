import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.javaflow.api.continuable;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import primula.agent.AbstractAgent;
import util.PlotUtil;

public class DL4JtestSVM extends AbstractAgent {

	static boolean visualize = true;
	
	RecordReaderDataSetIterator Dataset_train(String str , int size) {
		RecordReader rr = new CSVRecordReader();
		try {
			rr.initialize(new FileSplit(new File(str, "linear_data_train.csv")));
		} catch (IOException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
		return new RecordReaderDataSetIterator(rr, size, 0, 2);
	}
	
	RecordReaderDataSetIterator Dataset_test(String str , int size) {
		RecordReader rr = new CSVRecordReader();
		try {
			rr.initialize(new FileSplit(new File(str, "linear_data_eval.csv")));
		} catch (IOException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
		return new RecordReaderDataSetIterator(rr, size, 0, 2);
	}
	
	void evals(int numOutputs , DataSetIterator testIter , 		MultiLayerNetwork model) {
		Evaluation eval = new Evaluation(numOutputs);
		while (testIter.hasNext()) {
			DataSet t = testIter.next();
			INDArray features = t.getFeatures();
			INDArray labels = t.getLabels();
			INDArray predicted = model.output(features, false);
			eval.eval(labels, predicted);
		}
		System.out.println(eval.stats());
	}
	
	public static void generateVisuals(MultiLayerNetwork model, DataSetIterator trainIter, DataSetIterator testIter)
			throws Exception {
		if (visualize) {
			double xMin = 0;
			double xMax = 1.0;
			double yMin = -0.2;
			double yMax = 0.8;
			int nPointsPerAxis = 100;

			// Generate x,y points that span the whole range of features
			INDArray allXYPoints = PlotUtil.generatePointsOnGraph(xMin, xMax, yMin, yMax, nPointsPerAxis);
			// Get train data and plot with predictions
			PlotUtil.plotTrainingData(model, trainIter, allXYPoints, nPointsPerAxis);
			TimeUnit.SECONDS.sleep(3);
			// Get test data, run the test data through the network to generate predictions,
			// and plot those predictions:
			PlotUtil.plotTestData(model, testIter, allXYPoints, nPointsPerAxis);
		}
	}
	
	public @continuable void run() {
		System.out.println("Starting Migration\n");

		this.migrate();
		System.out.println("Migrated");

		String dataLocalPath;

		int seed = 123;
		double learningRate = 0.01;
		int batchSize = 50;
		int nEpochs = 30;

		int numInputs = 2;
		int numOutputs = 2;
		int numHiddenNodes = 20;

		dataLocalPath = "C://DeepLearning4JExamples/dl4j-examples";// DownloaderUtility.CLASSIFICATIONDATA.Download();
		// System.out.println("detaLocalPAthは"+dataLocalPath+"です！");
		// Load the training data:
		/*RecordReader rr = new CSVRecordReader();
		try {
			rr.initialize(new FileSplit(new File(dataLocalPath, "linear_data_train.csv")));
		} catch (IOException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}*/
		DataSetIterator trainIter = Dataset_train(dataLocalPath,batchSize);//new RecordReaderDataSetIterator(rr, batchSize, 0, 2);

		// Load the test/evaluation data:
		/*RecordReader rrTest = new CSVRecordReader();
		try {
			rrTest.initialize(new FileSplit(new File(dataLocalPath, "linear_data_eval.csv")));
		} catch (IOException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}*/
		DataSetIterator testIter = Dataset_test(dataLocalPath,batchSize);;//new RecordReaderDataSetIterator(rrTest, batchSize, 0, 2);

		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).weightInit(WeightInit.XAVIER)
				.updater(new Nesterovs(learningRate, 0.9)).list()
				.layer(new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes).activation(Activation.RELU).build())
				.layer(new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD).activation(Activation.SOFTMAX)
						.nIn(numHiddenNodes).nOut(numOutputs).build())
				.build();

		MultiLayerNetwork model = new MultiLayerNetwork(conf);
		model.init();
		model.setListeners(new ScoreIterationListener(10)); // Print score every 10 parameter updates

		model.fit(trainIter, nEpochs);

		//migrate(getmyIP()); //今のところ無理(FileSplitがシリアライザブルじゃない)
		
		System.out.println("Evaluate model....");
		
		evals(numOutputs,testIter,model);
		
		/*Evaluation eval = new Evaluation(numOutputs);
		while (testIter.hasNext()) {
			DataSet t = testIter.next();
			INDArray features = t.getFeatures();
			INDArray labels = t.getLabels();
			INDArray predicted = model.output(features, false);
			eval.eval(labels, predicted);
		}
		// An alternate way to do the above loop
		// Evaluation evalResults = model.evaluate(testIter);

		// Print the evaluation statistics
		System.out.println(eval.stats());*/

		System.out.println("\n****************Example finished********************");
		// Training is complete. Code that follows is for plotting the data &
		// predictions only
		try {
			generateVisuals(model, trainIter, testIter);
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		System.out.println("finish");
		migrate(getmyIP()); //今のところ無理(FileSplitがシリアライザブルじゃない)
	}

	/*public static void generateVisuals(MultiLayerNetwork model, DataSetIterator trainIter, DataSetIterator testIter)
			throws Exception {
		if (visualize) {
			double xMin = 0;
			double xMax = 1.0;
			double yMin = -0.2;
			double yMax = 0.8;
			int nPointsPerAxis = 100;

			// Generate x,y points that span the whole range of features
			INDArray allXYPoints = PlotUtil.generatePointsOnGraph(xMin, xMax, yMin, yMax, nPointsPerAxis);
			// Get train data and plot with predictions
			PlotUtil.plotTrainingData(model, trainIter, allXYPoints, nPointsPerAxis);
			TimeUnit.SECONDS.sleep(3);
			// Get test data, run the test data through the network to generate predictions,
			// and plot those predictions:
			PlotUtil.plotTestData(model, testIter, allXYPoints, nPointsPerAxis);
		}
	}*/

	@Override
	public void requestStop() {
		// TODO Auto-generated method stub

	}

}
