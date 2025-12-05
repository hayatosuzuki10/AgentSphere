package sphereConcurrent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

import primula.api.AgentAPI;
import primula.api.NetworkAPI;

public class SphereExecutors implements Serializable{
	/**
	 *
	 */
	private static final long serialVersionUID = 100L;
	private static HashMap<UUID, SphereExecutorService> executorMap;

	static{
		executorMap = new HashMap<UUID, SphereExecutorService>();
	}

	/**
	 * 起動マシンのコア数で自動生成
	 * @return コア数分の実行エージェントがいるExecutorService
	 */
	public static SphereExecutorService newSphereExecutorService(){
		int core = Runtime.getRuntime().availableProcessors();
		return newSphereExecutorService(core);
	}

        public static SphereExecutorService newNetWorkSphereExecutorService(){
            int core = Runtime.getRuntime().availableProcessors();
            return newNetWorkSphereExecutorService(core * (NetworkAPI.getAddresses().size()+1));
	}

        public static SphereExecutorService newSphereExecutorService(int workers){
            SphereExecutorService sphereExecutorService = new LoopAgentExecutorService();
            UUID uuid = sphereExecutorService.getId();
            executorMap.put(uuid, sphereExecutorService);
            for(int i=0;i<workers;i++){
            	TestWorker worker = new TestWorker(uuid);
            	//NomadWorker worker = new NomadWorker(uuid);
                //Worker worker = new NomadWorker(uuid);
                sphereExecutorService.addExecutor(worker.getAgentID());
                AgentAPI.runAgent(worker);
            }

            return sphereExecutorService;
	}

        public static SphereExecutorService newNetWorkSphereExecutorService(int workers){
            SphereExecutorService sphereExecutorService = new LoopAgentExecutorService();
            UUID uuid = sphereExecutorService.getId();
            executorMap.put(uuid, sphereExecutorService);

            for(int i=0;i<workers;i++){
                Worker worker = new NomadWorker(uuid);
                sphereExecutorService.addExecutor(worker.getAgentID());
                AgentAPI.runAgent(worker);
            }

            return sphereExecutorService;
	}

	public static SphereExecutorService newManyThreadSphereExecutorService(){
		SphereExecutorService sphereExecutorService = new ManyThreadExecutorService();
		UUID uuid = sphereExecutorService.getId();
		executorMap.put(uuid, sphereExecutorService);

		return sphereExecutorService;
	}

	static SphereExecutorService getExecutorService(UUID exeid) {
		return executorMap.get(exeid);
	}
}
