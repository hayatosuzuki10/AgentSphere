package sphereConcurrent;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.UUID;

import primula.agent.AbstractAgent;
import primula.api.SystemAPI;
import primula.util.KeyValuePair;

public class TestWorker extends AbstractAgent {
	private static final long serialVersionUID = 1L;
	String makedMachine;
	UUID serviceId;
	LinkedList<SphereTask> sphereTasks;
	LinkedList<SphereTask> execedTasks;
	KeyValuePair<InetAddress, Integer> next;

	TestWorker(UUID serviceId) {
		this.serviceId = serviceId;
		makedMachine = SystemAPI.getAgentSphereId();
		this.sphereTasks = new LinkedList<SphereTask>();
		this.execedTasks = new LinkedList<SphereTask>();
		this.next = new KeyValuePair<InetAddress, Integer>(null, 55878);
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void runAgent() {
		allexec();
		setresult();
		LoopAgentExecutorService exeserv = (LoopAgentExecutorService) SphereExecutors
				.getExecutorService(serviceId);
		while ((this.sphereTasks = exeserv.pollTasks()) != null) {
//			System.err.println(this.getAgentID() + "@" + sphereTasks);
			allexec();
			setresult();
		}
		exeserv.removeExecutor(getAgentID());
	}

	/**
	 * 保持している未実行のタスクをすべて実行する
	 */
	private void allexec() {
		if (!this.sphereTasks.isEmpty()) {
//			System.out.println("true");
			SphereTask task;
			while ((task = this.sphereTasks.poll()) != null) {
				task.execute();
				this.execedTasks.add(task);
			}
		}
		else {
//			System.out.println("else");
		}
	}

	/**
	 * 実行済みタスクの結果をセットします。
	 */
	private void setresult() {
		if (!this.execedTasks.isEmpty()) {
			LoopAgentExecutorService service = (LoopAgentExecutorService) SphereExecutors.getExecutorService(serviceId);
			SphereTask task;
			while ((task = this.execedTasks.poll()) != null) {
				SphereFuture future = service.getFuture(task.uuid);
				boolean flag = future.setResult(task);
				if (!flag)
					System.err.println("setresult error");
			}
		}
	}

}
