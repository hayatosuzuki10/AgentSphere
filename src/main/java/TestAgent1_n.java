
import primula.agent.AbstractAgent;

public class TestAgent1_n extends AbstractAgent {

	int count = 0;

	@Override
	public synchronized void run() {

		this.migrate();
		System.out.println("AgentID : " + getAgentID());
		System.out.println("Start.");

		while (count < 10) {
			try {
				wait(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//if(count==5) AgentAPI.backup(this, ++BACKUPNUM, true);
			System.out.println("count : " + (++count));
		}

		System.out.println("End.");

	}

	@Override
	public void requestStop() {
	}

}
