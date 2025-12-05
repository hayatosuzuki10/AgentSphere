package sphereConcurrent;

import java.util.UUID;


/**
 * 性能評価＆テスト用クラス
 * @author akita
 *
 * @param <V> 実行タスクの型
 */
public class SingleTaskExecutor<V> implements Runnable{

	UUID serviceId;
	SphereTask<V> sphereTask;

	public SingleTaskExecutor(UUID serviceId, SphereTask<V> sphereTask) {
		this.serviceId = serviceId;
		this.sphereTask = sphereTask;
	}

	@Override
	public void run() {
		//callable execute
		sphereTask.execute();

		//set result
		ManyThreadExecutorService service = (ManyThreadExecutorService)SphereExecutors.getExecutorService(serviceId);
		SphereFuture future = service.getFuture(sphereTask.uuid);
		boolean flag = future.setResult(sphereTask);
		if(flag);
	}

}
