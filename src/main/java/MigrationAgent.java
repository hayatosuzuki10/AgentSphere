  
import primula.agent.AbstractAgent;

public class MigrationAgent extends AbstractAgent {

	@Override
	public void run(){
		System.out.println("Migration Test Start.");

		this.migrate();

		System.out.println("Migration Test End.");
	}

	@Override
	public void requestStop() {
	}

}
