package sphereConcurrent;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.NotFoundException;
import primula.api.AgentAPI;
import primula.api.NetworkAPI;
import primula.api.SystemAPI;

/**
 *　性能評価＆テスト用クラス
 */
public class ManyThreadExecutorService implements SphereExecutorService{


   private static final long serialVersionUID = 100L;
   HashMap<UUID, SphereFuture> futureMap;
   UUID uuid;
   Status status;
   Geniusable genius;

   //コンストラクタ
   public ManyThreadExecutorService() {
       this.futureMap = new HashMap<UUID, SphereFuture>();
       this.uuid = UUID.randomUUID();
       this.status = Status.RUNNING;
       this.genius = new NonGenius(null,null);//使うのはマシン割り当てのみなので
   }


   //Submit Serializable必須なのでSphereCallableのみ対応
   public synchronized <T> SphereFuture<T> submit(SphereCallable<T> sphereCallable){
       if(this.status != Status.RUNNING){
           throw new RejectedExecutionException("This Service is Un Running");
       }

       SphereTask<T> task = new SphereTask<T>(sphereCallable);
       SphereFuture<T> future = new SphereFuture<T>(task);
       this.futureMap.put(task.uuid, future);
        //実行投入
        /*
        SingleTaskExecutor<T> worker = new SingleTaskExecutor<T>(uuid, task);
        new Thread(worker).start();
        //*/
        /*
        SingleStayWorker worker = new SingleStayWorker(uuid,task);
        AgentAPI.runAgent(worker);
        //*/
        //*
        String execmachine = genius.execMachine();
        if(SystemAPI.getAgentSphereId().equals(execmachine)){
            AgentAPI.runAgent(new SingleRemoteWorker(uuid, task));
        }else{
            try {
                AgentAPI.migration(NetworkAPI.getAddressByAgentSphereId(execmachine), new SingleRemoteWorker(uuid, task));
            } catch (NotFoundException ex) {
                Logger.getLogger(ManyThreadExecutorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       //*/
       return future;
   }

   //ShutDown
   public synchronized void shutdown(){
       this.status = Status.SHUTDOWN;
       futureMap.clear();
   }


   synchronized SphereFuture getFuture(UUID callableId) {
       return futureMap.get(callableId);
   }

	@Override
	public UUID getId() {
		return this.uuid;
	}


	@Override
	public boolean addExecutor(String executorId) {
		return false;
	}

}
