package sphereConcurrent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;


public class LoopAgentExecutorService implements Serializable,SphereExecutorService{

    /**
     *
     */
    private static final long serialVersionUID = 100L;
    LinkedList<SphereTask> taskList;
    HashMap<UUID, SphereFuture> futureMap;
    UUID uuid;
    Status status;
    LinkedList<String> executors;
    Geniusable genius;

    //コンストラクタ
    public LoopAgentExecutorService() {
        this.futureMap = new HashMap<UUID, SphereFuture>();
        this.uuid = UUID.randomUUID();
        this.status = Status.RUNNING;
        this.taskList = new LinkedList<SphereTask>();
        this.executors = new LinkedList<String>();
        this.genius = new NonGenius(taskList, executors);
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
        this.taskList.add(task);
        notifyAll();

        return future;
    }

    //ShutDown
    public synchronized void shutdown(){
        this.status = Status.SHUTDOWN;
        notifyAll();
        while(this.executors.size() != 0){
        	try {
				wait();
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
        }
        futureMap.clear();
    }


    synchronized SphereFuture getFuture(UUID callableId) {
        return futureMap.get(callableId);
    }

    synchronized SphereTask pollTask(){
    	while( ( ! this.status.equals(Status.SHUTDOWN)) && this.taskList.isEmpty()){
    		try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;//とりあえず
			}
    	}
    	if(this.status.equals(Status.SHUTDOWN)){
    		return null;
    	}
    	else{
    		return this.taskList.poll();
    	}
    }

    synchronized LinkedList<SphereTask> pollTasks(){
    	while( ( !this.status.equals(Status.SHUTDOWN)) && this.taskList.isEmpty()){
    		try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;//とりあえず
			}
    	}
    	if(this.status.equals(Status.SHUTDOWN)){
    		return null;
    	}
    	else{
            LinkedList<SphereTask> tasks = new LinkedList<SphereTask>();
            for(int i = genius.pollTasksSize(); (!this.taskList.isEmpty()) && i>0 ;i--){
                tasks.add(this.taskList.poll());
            }
            return tasks;
    	}
    }

    synchronized boolean removeExecutor(String executorId){
        boolean flag = this.executors.remove(executorId);
    	if(!flag) System.err.println("null worker");
    	notifyAll();
    	return true;
    }

    public synchronized boolean addExecutor(String executorId){
    	this.executors.add(executorId);
        notifyAll();
    	return true;
    }

    public synchronized String execMachine(){
        return this.genius.execMachine();
    }

    @Override
    public UUID getId() {
            // TODO 自動生成されたメソッド・スタブ
            return this.uuid;
    }
}
