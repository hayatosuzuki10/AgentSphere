package primula.api.core.agent;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import primula.agent.AbstractAgent;
import primula.api.SystemAPI;
import primula.api.core.agent.loader.ClassDataCollectionContainer;
import primula.api.core.interim.manage.GeneratedAgentList;
import primula.api.core.network.AgentPack;
import primula.api.core.resource.ReadyRunAgentPool;
import primula.api.core.resource.ReadyRunAgentPoolListener;
import primula.api.core.resource.ReadySendAgentPool;
import primula.api.core.resource.ReadySendAgentPoolListener;
import primula.util.KeyValuePair;

/**
 * Agentを管理するクラス
 * @author yamamoto
 */
public class AgentManager implements IAgentManager {
    public static BlockingQueue<KeyValuePair<String, Boolean>> queue = new LinkedBlockingQueue<KeyValuePair<String, Boolean>>();
    private RunningAgentPool runningAgentPool = new RunningAgentPool();
    private ReadySendAgentPool readySendAgentPool = ReadySendAgentPool.getInstance();
    private ReadyRunAgentPool readyRunAgentPool = ReadyRunAgentPool.getInstance();
    private GeneratedAgentList generatedAgentList = GeneratedAgentList.getInstance();

    public AgentManager() {
    }

    @Override
    public synchronized void runAgent(AbstractAgent agent) {
        readyRunAgentPool.addAgent(agent);
    }

    @Override
    public void runAgent(AbstractAgent agent, String group) {
        readyRunAgentPool.addAgent(agent, group);
    }

    @Override
    public void initializeCoreModele() {

        readyRunAgentPool.addReadyRunAgentPoolListener(new ReadyRunAgentPoolListener() {

            @Override
            public void readyRunAgentAdded(String agentID , String agentName) {
                KeyValuePair<String, AbstractAgent> val = readyRunAgentPool.poll();
                AgentThread agentThread = new AgentThread(val.getValue()); // 指定のエージェントを起動
                agentThread.setName(val.getValue().getClass().getName()+"-"+agentThread.getName()); // エージェント名(クラス名)-スレッド名
                runningAgentPool.addAndRunAgent(agentThread, val.getKey());
            }
        });

        runningAgentPool.addRunningAgentPoolListener(new RunningAgentPoolListener() {

            @Override
            public void agentFinished(String agentID, String agentName) {
                //
            }
        });

        generatedAgentList.startup();
    }

    @Override
    public void finalizeCoreModule() {
        //    throw new UnsupportedOperationException();
//        try {
//
//            fetchThread.requestStop();
//            fetchThread.join();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(AgentManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    @Override
    public synchronized HashMap<String, List<AgentInstanceInfo>> getAgentInfos() {
        return runningAgentPool.getAgentInfos();
    }

    @Override
    public synchronized void migrate(KeyValuePair<InetAddress, Integer> address, AbstractAgent agent) {

        AgentPack agentPack = new AgentPack(
            SystemAPI.getAgentSphereId(),
            Integer.parseInt(SystemAPI.getConfigData("DefaultPort").toString()),
            agent,
            null
        );

        
        readySendAgentPool.addSendAgent(new KeyValuePair<>(address, agentPack));
        
    }

    @Override
    public synchronized void migrate(KeyValuePair<InetAddress, Integer> address, AbstractAgent agent, ClassDataCollectionContainer collectionContainer) {
        //throw new UnsupportedOperationException("Not supported yet."); 変更前
        AgentPack agentPack = new AgentPack(SystemAPI.getAgentSphereId(), Integer.parseInt(SystemAPI.getConfigData("DefaultPort").toString()), agent, collectionContainer);
        readySendAgentPool.addSendAgent(new KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack>(address, agentPack));
    }

    @Override
    public void requestStop(AbstractAgent agent) {
        // TODO 自動生成されたメソッド・スタブ
        agent.requestStop();
    }

    @Override
    public void registReadyRunAgentPoolListener(ReadyRunAgentPoolListener listener) {
        readyRunAgentPool.addReadyRunAgentPoolListener(listener);
    }

    @Override
    public void removeReadyRunAgentPoolListener(ReadyRunAgentPoolListener listener) {
        readyRunAgentPool.removeReadyRunAgentPoolListener(listener);
    }

    @Override
    public void registReadySendAgentPoolListener(ReadySendAgentPoolListener listener) {
        readySendAgentPool.addReadySendAgentPoolListener(listener);
    }

    @Override
    public void removeReadySendAgentPoolListener(ReadySendAgentPoolListener listener) {
        readySendAgentPool.removeReadySendAgentPoolListener(listener);
    }

    @Override
    public void registRunningAgentPoolListener(RunningAgentPoolListener listener){
        runningAgentPool.addRunningAgentPoolListener(listener);
    }

    @Override
    public void removeRunningAgentPoolListener(RunningAgentPoolListener listener){
        runningAgentPool.removeRunningAgentPoolListener(listener);
    }
    
    @Override
    public AbstractAgent getAgentByID(String agentID) {
    	AbstractAgent agent = runningAgentPool.getAgentByID(agentID);

    	return agent;
    }

}
