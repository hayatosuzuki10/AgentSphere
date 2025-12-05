/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.resource;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import primula.agent.AbstractAgent;
import primula.api.SystemAPI;
import primula.api.core.interim.manage.GeneratedAgentList;
import primula.api.core.network.AgentPack;
import primula.api.core.network.receiver.AgentRegistrator;
import primula.util.KeyValuePair;

/**
 *
 * @author yamamoto
 */
public class ReadyRunAgentPool {

    private ConcurrentLinkedQueue<KeyValuePair<String, AbstractAgent>> queue = new ConcurrentLinkedQueue<KeyValuePair<String, AbstractAgent>>();
    private ArrayList<ReadyRunAgentPoolListener> agentListeners = new ArrayList<ReadyRunAgentPoolListener>();
    private static ReadyRunAgentPool agentPool;
    private static GeneratedAgentList generatedAgentList = GeneratedAgentList.getInstance();
    private static AgentRegistrator agentServer = AgentRegistrator.getInstance();

    private ReadyRunAgentPool() {
    }

    public static synchronized ReadyRunAgentPool getInstance() {
        if (agentPool == null) {
            agentPool = new ReadyRunAgentPool();
        }
        return agentPool;
    }

    public synchronized void addAgent(AgentPack agentPack) {
        //System.out.println("1");
        //TODO:クラスローダーの起動
        addAgent(agentPack, agentPack.getAgentSphereId());//←変更後
    }

    public synchronized void addAgent(AgentPack agentPack, String group) {
        //System.out.println("2");
        //TODO:クラスローダーの起動
        addAgent(agentPack.getAgent(), group);
    }

    public synchronized void addAgent(AbstractAgent agent) {
        //System.out.println("3");
        addAgent(agent, (String) SystemAPI.getConfigData("DefaultAgentGroupName"));
    }

    public synchronized void addAgent(AbstractAgent agent, String group) {
        //System.out.println("4");
        agentServer.receive(agent.getAgentID());
        queue.add(new KeyValuePair<String, AbstractAgent>(group, agent));
        notifyAgentAdded(agent.getAgentID(),agent.getAgentName());
        generatedAgentList.updateList(agent, group);     
    }

    public synchronized int getSize() {
        return queue.size();
    }

    public synchronized KeyValuePair<String, AbstractAgent> poll() {
        return queue.poll();
    }

    public synchronized void addReadyRunAgentPoolListener(ReadyRunAgentPoolListener listener) {
        agentListeners.add(listener);
    }

    public synchronized void removeReadyRunAgentPoolListener(ReadyRunAgentPoolListener listener) {
        agentListeners.remove(listener);
    }

    private void notifyAgentAdded(String agentID, String agentName) {
        for (ReadyRunAgentPoolListener listener : agentListeners) {
            listener.readyRunAgentAdded(agentID, agentName);
        }
    }
}
