/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sphereConcurrent;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.NotFoundException;
import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.NetworkAPI;
import primula.api.SystemAPI;

/**
 * 性能評価＆テスト用クラス
 * @author akita
 */
public class SingleRemoteWorker extends AbstractAgent{
    UUID myPool;
    SphereTask sphereTask;
    boolean execed;
    String makedMachine;
    
    public SingleRemoteWorker(UUID myPool, SphereTask task) {
        super();
        this.myPool = myPool;
        this.sphereTask = task;
        execed = false;
        makedMachine = SystemAPI.getAgentSphereId();
    }
    
    @Override
    public void runAgent() {
        //callable execute
        if(! execed){
            //callable execute
            sphereTask.execute();
            execed = true;
            if( ! makedMachine.equals(SystemAPI.getAgentSphereId())){
                try {
                    AgentAPI.migration(NetworkAPI.getAddressByAgentSphereId(makedMachine), this);
                    return;
                } catch (NotFoundException ex) {
                    Logger.getLogger(NomadWorker.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            //set result in non migrate
            ManyThreadExecutorService service = (ManyThreadExecutorService)SphereExecutors.getExecutorService(myPool);
            SphereFuture future = service.getFuture(sphereTask.uuid);
            boolean flag = future.setResult(sphereTask);
            if(flag);
        }
        else{ //after migrate
            //set result
            //System.err.println(""+this.sphereTask.result);
            ManyThreadExecutorService service = (ManyThreadExecutorService)SphereExecutors.getExecutorService(myPool);
            SphereFuture future = service.getFuture(sphereTask.uuid);
            boolean flag = future.setResult(sphereTask);
            if(!flag) System.err.println("setresult error");
        }
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
