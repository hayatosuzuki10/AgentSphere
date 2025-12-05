package sphereConcurrent;

import java.util.UUID;

import primula.agent.AbstractAgent;

/**
 * 性能評価＆テスト用クラス
 * @author akita
 *
 */
public class SingleStayWorker extends AbstractAgent{
    UUID serviceId;
    SphereTask sphereTask;

	SingleStayWorker(UUID serviceId, SphereTask task) {
        super();
        this.serviceId = serviceId;
        this.sphereTask = task;
    }

	@Override
	public void runAgent() {
            sphereTask.execute();
            ManyThreadExecutorService service = (ManyThreadExecutorService)SphereExecutors.getExecutorService(serviceId);
            SphereFuture future = service.getFuture(sphereTask.uuid);
            boolean flag = future.setResult(sphereTask);
            if(flag);
	}

	@Override
	public void requestStop() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
