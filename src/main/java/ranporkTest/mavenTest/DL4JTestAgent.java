package ranporkTest.mavenTest;

import java.io.File;
import java.io.IOException;

import org.apache.commons.javaflow.api.continuable;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import primula.agent.AbstractAgent;

public class DL4JTestAgent extends AbstractAgent {

	@Override
	public @continuable void run() {
		try {
		//Define a simple MultiLayerNetwork:
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
				.weightInit(WeightInit.XAVIER)
				.updater(new Nesterovs(0.1, 0.9))
				.list()
				.layer(new DenseLayer.Builder().nIn(4).nOut(3).activation(Activation.TANH).build())
				.layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
						.activation(Activation.SOFTMAX).nIn(3).nOut(3).build())
				.build();

		MultiLayerNetwork net = new MultiLayerNetwork(conf);
		net.init();

		//Save the model
		File locationToSave = new File("MyMultiLayerNetwork.zip"); //Where to save the network. Note: the file is in .zip format - can be opened externally
		boolean saveUpdater = true; //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
		net.save(locationToSave, saveUpdater);

		//Load the model
		MultiLayerNetwork restored = MultiLayerNetwork.load(locationToSave, saveUpdater);

		System.out.println("Saved and loaded parameters are equal:      " + net.params().equals(restored.params()));
		System.out.println("Saved and loaded configurations are equal:  "
				+ net.getLayerWiseConfigurations().equals(restored.getLayerWiseConfigurations()));
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

}
