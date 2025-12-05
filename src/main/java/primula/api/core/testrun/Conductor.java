package primula.api.core.testrun;

public class Conductor {
	private Human yamada;

	public Conductor() {
		yamada = new Human();
	}

	public void start() {
		yamada.start();
	}
}
