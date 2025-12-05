package ranporkTest.util;

import java.time.Duration;
import java.time.Instant;

import primula.agent.AbstractAgent;

public class TimerAgent extends AbstractAgent {

	private static boolean running = false;
	private static Instant start = null;

	@Override
	public void run() {
		if (!running) {
			start = Instant.now();
			running = true;
		} else {
			Instant end = Instant.now();
			Duration elapse = Duration.between(start, end);
			System.out.println(this.getClass().getName() + ":total time " + (elapse.toNanos() / 1000000.0)
										+ "[ms]");
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

}
