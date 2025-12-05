package sphereConcurrent;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.UUID;

import primula.api.AgentAPI;
import primula.api.ScheduleAPI;
import primula.api.SystemAPI;
import primula.util.KeyValuePair;


class NomadWorker extends Worker {
    /**
	 *
	 */
	private static final long serialVersionUID = 100L;
	UUID serviceId;
    LinkedList<SphereTask> sphereTasks;
    LinkedList<SphereTask> execedTasks;
    String makedMachine;
    String autonomous;
    KeyValuePair<InetAddress, Integer> next;

    NomadWorker(UUID serviceId) {
        super();
        this.serviceId = serviceId;
        this.sphereTasks = new LinkedList<SphereTask>();
        this.execedTasks = new LinkedList<SphereTask>();
        makedMachine = SystemAPI.getAgentSphereId();
        autonomous = null;
        this.next = new KeyValuePair<InetAddress, Integer>(null, 55878);
    }

    @Override
    public void runAgent() {
    	//生成されたマシンでない場合
    	//持ってるタスクをすべて終わらせて生成されたマシンへ帰る
        if(! this.makedMachine.equals(SystemAPI.getAgentSphereId())){
        	System.out.println("⊂二二二(　^ω＾）二二⊃");
            allexec();
            next.setKey(this.getmyIP());
			//AgentAPI.migration(NetworkAPI.getAddressByAgentSphereId(makedMachine), this);
			AgentAPI.migration(next, this);
			return;
        }
        else{
            allexec();
            setresult();
            LoopAgentExecutorService exeserv = (LoopAgentExecutorService)SphereExecutors.getExecutorService(serviceId);
            //String execmachine;
            while((this.sphereTasks = exeserv.pollTasks()) != null){
            	System.err.println("WARNNING");
                //execmachine = autonomous();
                next.setKey(ScheduleAPI.getHighPerformancedMachine());
                /*
            	try {
					next.setKey(Inet4Address.getByName(IPAddress.IPAddress));
				} catch (UnknownHostException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
				*/
                if( ! this.getmyIP().equals(next.getKey())){
                    AgentAPI.migration(next, this);
					return;
                }
                allexec();
                setresult();
            }
            exeserv.removeExecutor(getAgentID());
        }
    }

    /**
     * 自律的にマシンを選定するアルゴリズムを記述してください。
     * ここに記述したアルゴリズムに従って、次に実行する受け取ったタスク群の実行マシンを選定します。
     * このプロトタイプ実装では<code>SphereExecutors</code>へ移動先を問い合わせています。
     */
    protected String autonomous(){
    	//if(this.autonomous == null){
    		this.autonomous = ((LoopAgentExecutorService)SphereExecutors.getExecutorService(serviceId)).execMachine();
    	//}
    	return this.autonomous;
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * 保持している未実行のタスクをすべて実行する
     */
    private void allexec(){
        if(! this.sphereTasks.isEmpty()){
            SphereTask task;
            while((task = this.sphereTasks.poll()) != null){
                task.execute();
                this.execedTasks.add(task);
            }
        }
    }

    /**
     * 実行済みタスクの結果をセットします。
     */
    private void setresult(){
        if(! this.execedTasks.isEmpty()){
            LoopAgentExecutorService service = (LoopAgentExecutorService)SphereExecutors.getExecutorService(serviceId);
            SphereTask task;
            while((task = this.execedTasks.poll()) != null){
                SphereFuture future = service.getFuture(task.uuid);
                boolean flag = future.setResult(task);
                if( ! flag) System.err.println("setresult error");
            }
        }
    }
}
