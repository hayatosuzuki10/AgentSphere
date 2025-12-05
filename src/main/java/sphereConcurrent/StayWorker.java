package sphereConcurrent;

import java.util.UUID;

/**
 * 性能評価＆テスト用クラス
 * @author akita
 *
 */
public class StayWorker extends Worker{
	UUID serviceId;
	SphereTask sphereTask;

	StayWorker(UUID serviceId){
		super();
		this.serviceId = serviceId;
		sphereTask = null;
	}

	@Override
	public void runAgent() {
		// TODO 自動生成されたメソッド・スタブ
		LoopAgentExecutorService exeserv = (LoopAgentExecutorService)SphereExecutors.getExecutorService(serviceId);
		while((sphereTask = exeserv.pollTask()) != null){
			sphereTask.execute();
			exeserv.getFuture(sphereTask.uuid).setResult(sphereTask);
		}
		exeserv.removeExecutor(getAgentID());

	}
	@Override
	public void requestStop() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
