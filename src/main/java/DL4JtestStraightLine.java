import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import primula.agent.AbstractAgent;

//https://github.com/deeplearning4j/deeplearning4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/quickstart/modeling/feedforward/regression/CSVDataModel.java

public class DL4JtestStraightLine extends AbstractAgent { // プロットされた点に対しなるべく近い?直線を書く(多分???) CSV..model

	private static Logger log = LoggerFactory.getLogger(DL4JtestStraightLine.class);

	public @continuable void run() {
		log.info("Starting Migration\n");

		this.migrate();
		log.info("Migrated");

		String USEfile = "Book1.csv"; // 学習したいファイルを変えるのはここ(Book1.csvを変えて) C://DeepLearning4JExamples/Book1.csv
		Path path = Paths.get("C://DeepLearning4JExamples/Book1.csv");// ここも変えて

		boolean visualize = true;
		String dataLocalPath;

		dataLocalPath = "C://DeepLearning4JExamples";// DownloaderUtility.DATAEXAMPLES.Download();
														// //C直下にDeepLearning4JExamplesというディレクトリを作る
		String filename = new File(dataLocalPath, USEfile).getAbsolutePath(); // DeepLearning4JExamplesの中にプロットのためのcsvファイルを作る(1列目はx座標,2列目はy座標のデータ)
		DataSet ds = null;
		try {
			ds = readCSVDataset(filename);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		long lineCount = 0;
		try {
			lineCount = Files.lines(path).count();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		System.out.println("行数の合計は"+lineCount+"です。\n");
		ArrayList<DataSet> DataSetList = new ArrayList<>();
		DataSetList.add(ds);

		plotDataset(DataSetList); // Plot the data, make sure we have the right data.

		MultiLayerNetwork net = fitStraightline(ds);

		// Get the min and max x values, using Nd4j
		NormalizerMinMaxScaler preProcessor = new NormalizerMinMaxScaler();
		preProcessor.fit(ds);
		int nSamples = (int) lineCount;
		INDArray x = Nd4j.linspace(preProcessor.getMin().getInt(0), preProcessor.getMax().getInt(0), nSamples)
				.reshape(nSamples, 1);
		INDArray y = net.output(x);
		DataSet modeloutput = new DataSet(x, y);
		DataSetList.add(modeloutput);

		// plot on by default
		if (visualize) {
			plotDataset(DataSetList); // Plot data and model fit.
		}
		log.info("finish");
	}

	private static MultiLayerNetwork fitStraightline(DataSet ds) {
		int seed = 12345;
		int nEpochs = 400;
		double learningRate = 0.00001;
		int numInputs = 1;
		int numOutputs = 1;

		//
		// Hook up one input to the one output.
		// The resulting model is a straight line.
		//
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).weightInit(WeightInit.XAVIER)
				.updater(new Nesterovs(learningRate, 0.9)).list()
				.layer(new DenseLayer.Builder().nIn(numInputs).nOut(numOutputs).activation(Activation.IDENTITY).build())
				.layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.IDENTITY)
						.nIn(numOutputs).nOut(numOutputs).build())
				.build();

		MultiLayerNetwork net = new MultiLayerNetwork(conf);
		net.init();
		net.setListeners(new ScoreIterationListener(1));

		for (int i = 0; i < nEpochs; i++) {
			net.fit(ds);
		}

		return net;
	}

	private static DataSet readCSVDataset(String filename) throws IOException, InterruptedException {
		int batchSize = 1000;
		RecordReader rr = new CSVRecordReader();
		rr.initialize(new FileSplit(new File(filename)));

		DataSetIterator iter = new RecordReaderDataSetIterator(rr, batchSize, 1, 1, true);
		return iter.next();
	}

	private static void plotDataset(ArrayList<DataSet> DataSetList) {

		XYSeriesCollection c = new XYSeriesCollection();

		int dscounter = 1; // use to name the dataseries
		for (DataSet ds : DataSetList) {
			INDArray features = ds.getFeatures();
			INDArray outputs = ds.getLabels();

			int nRows = features.rows();
			XYSeries series = new XYSeries("S" + dscounter);
			for (int i = 0; i < nRows; i++) {
				series.add(features.getDouble(i), outputs.getDouble(i));
			}

			c.addSeries(series);
		}

		String title = "title";
		String xAxisLabel = "xAxisLabel";
		String yAxisLabel = "yAxisLabel";
		PlotOrientation orientation = PlotOrientation.VERTICAL;
		boolean legend = false;
		boolean tooltips = false;
		boolean urls = false;
		// noinspection ConstantConditions
		JFreeChart chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, c, orientation, legend,
				tooltips, urls);
		JPanel panel = new ChartPanel(chart);

		JFrame f = new JFrame();
		f.add(panel);
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.pack();
		f.setTitle("Training Data");

		f.setVisible(true);
	}

	@Override
	public void requestStop() {
		// TODO Auto-generated method stub

	}

}