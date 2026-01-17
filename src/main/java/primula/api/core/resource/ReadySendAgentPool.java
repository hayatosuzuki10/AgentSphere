/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.resource;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import primula.api.core.network.AgentPack;
import primula.util.KeyValuePair;

/**
 *
 * @author yamamoto
 */
public class ReadySendAgentPool {

    private ConcurrentLinkedQueue<KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack>> queue = new ConcurrentLinkedQueue<KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack>>();
    private ArrayList<ReadySendAgentPoolListener> agentListeners = new ArrayList<ReadySendAgentPoolListener>();
    private static ReadySendAgentPool agentPool;

    private ReadySendAgentPool() {
    }

    public static synchronized ReadySendAgentPool getInstance() {
        if (agentPool == null) {
            agentPool = new ReadySendAgentPool();
        }
        return agentPool;
    }

    public synchronized void addSendAgent(KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> pair) {

        queue.add(pair);
        notifyAgentAdded(pair.getValue().getAgent().getAgentID(), pair.getValue().getAgent().getAgentName());
    }

    public synchronized int getSize() {
        return queue.size();
    }

    public synchronized KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> poll() {
        return queue.poll();
    }

    public synchronized void addReadySendAgentPoolListener(ReadySendAgentPoolListener listener) {
        agentListeners.add(listener);
    }

    public synchronized void removeReadySendAgentPoolListener(ReadySendAgentPoolListener listener) {
        agentListeners.remove(listener);
    }

    private void notifyAgentAdded(String agentID, String agentName) {
    	for (ReadySendAgentPoolListener listener : agentListeners) {
        	
            listener.readySendAgentAdded(agentID, agentName);
        }
        
    }
}
