package ranporkTest.util;

import primula.agent.AbstractAgent;
import primula.api.core.assh.MainPanel;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;
import scheduler2022.util.IInfoUpdateListener;

public class CountLoadAverageAgent extends AbstractAgent implements IInfoUpdateListener {
	private static boolean runningState = false;
	private static CountLoadAverageAgent countLoadAverageAgent = null;
	private static DynamicPCInfo maxLoadAverage;

		private static DynamicPCInfo maxagentnum;

	@Override
	public void run() {
		if (!runningState) {
			Scheduler.registerInfoUpdateListener(this);
			countLoadAverageAgent = this;
			runningState=true;
		} else {
			Scheduler.removeInfoUpdateListener(countLoadAverageAgent);
			StringBuilder str = new StringBuilder();
			str.append(this.getClass().getName() + ":-maxLoadAverage-" + "\n")
					.append(this.getClass().getName() + ":LoadAverage " + maxLoadAverage.LoadAverage
							+ "\n")
					.append(this.getClass().getName() + ":Agentsnum " + maxLoadAverage.AgentsNum + "\n")
					.append(this.getClass().getName() + ":-maxAgentnum-" + "\n")
					.append(this.getClass().getName() + ":LoadAverage " + maxagentnum.LoadAverage + "\n")
					.append(this.getClass().getName() + ":Agentsnum " + maxagentnum.AgentsNum);
			System.out.println(str.toString());

			MainPanel.autoscroll();
			runningState=false;
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void pcInfoUpdate(DynamicPCInfo info) {
		if (maxLoadAverage.LoadAverage < info.LoadAverage) {
			maxLoadAverage = info;
		}
		if (maxagentnum.AgentsNum <= info.AgentsNum) {
			maxagentnum = info;
		}
	}

}
