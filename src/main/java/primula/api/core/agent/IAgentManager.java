/**
 *
 */
package primula.api.core.agent;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.core.ICoreModule;
import primula.api.core.agent.loader.ClassDataCollectionContainer;
import primula.api.core.resource.ReadyRunAgentPoolListener;
import primula.api.core.resource.ReadySendAgentPoolListener;
import primula.util.KeyValuePair;

/**
 * @author yamamoto
 *
 */
public interface IAgentManager extends ICoreModule {

    /**
     * Agentを起動します
     * @param agent
     */
    void runAgent(AbstractAgent agent);

    /**
     *  Agentを起動します
     * @param agent
     * @param group
     */
    void runAgent(AbstractAgent agent, String group);

    /**
     * クラスが送信先のAgentSphereにある場合に使用できます
     * @param agentSphereInfo
     * @param agent
     */
    void migrate(KeyValuePair<InetAddress, Integer> address, AbstractAgent agent);

    /**
     * クラスが送信先のAgentSphereにない場合に使用します
     * @param agentSphereInfo
     * @param agent
     * @param collectionContainer
     */
    void migrate(KeyValuePair<InetAddress, Integer> address, AbstractAgent agent, ClassDataCollectionContainer collectionContainer);

    /**
     * @author okubo
     * 起動しているAgentを一時停止します
     * @param agent
     */
    void requestStop(AbstractAgent agent);

    /**
     * 現在起動しているAgent情報を取得します
     * @return
     */
    HashMap<String, List<AgentInstanceInfo>> getAgentInfos();

    void registReadyRunAgentPoolListener(ReadyRunAgentPoolListener listener);

    void removeReadyRunAgentPoolListener(ReadyRunAgentPoolListener listener);

    void registReadySendAgentPoolListener(ReadySendAgentPoolListener listener);

    void removeReadySendAgentPoolListener(ReadySendAgentPoolListener listener);

    void registRunningAgentPoolListener(RunningAgentPoolListener listener);

    void removeRunningAgentPoolListener(RunningAgentPoolListener listener);
   
	AbstractAgent getAgentByID(String agentID);
}
