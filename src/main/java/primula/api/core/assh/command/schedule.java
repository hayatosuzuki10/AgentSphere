package primula.api.core.assh.command;

import java.util.List;

import scheduler2022.Scheduler;

public class schedule extends AbstractCommand{

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		// TODO 自動生成されたメソッド・スタブ
		Scheduler sch = new Scheduler();
		Thread th = new Thread(sch);
		th.setName("AgentSchedule2022");
		th.start();
		System.out.println("スケジューラ起動！！");

		return null;
	}
}
