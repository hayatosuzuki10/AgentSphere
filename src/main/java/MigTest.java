import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;

public class MigTest extends AbstractAgent {
	private int max = 100000000;

	public @continuable void run() {
		System.out.println("Starting Migration\n");

		this.migrate();
		System.out.println("Migrated");

		for (int i = 0; i < 100; i++) {
			//if(i==50)this.backup();
			for (int j = 0; j < max; j++) {

			}
			System.out.println(i);
		}
		System.out.println("finish");
	}

	@Override
	public void requestStop() {
		// TODO Auto-generated method stub

	}

}
