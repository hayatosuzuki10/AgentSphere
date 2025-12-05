/**
 *
 */
package primula.api.core.agent;

import java.util.ArrayList;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;

/**
 * @author yamamoto
 *
 */
class AgentThread extends Thread{

    private AbstractAgent Agent;
    public long threadId;
    private boolean running = true;
    private final long intervalMillis = 3000; // 3秒ごと
    private ArrayList<AgentThreadListener> listeners = new ArrayList<AgentThreadListener>();

    public AgentThread(AbstractAgent agent) {
        Agent = agent;
        setDaemon(true); // バックグラウンドスレッドとして稼働させる
    }

    public AbstractAgent getAgent() {
        return Agent;
    }

    @Override
    public @continuable void run() {
        Agent.runAgent();
        
        NotifyLisnters();
    }

    public void Abort() {
    }

    public void RegisterAgentThreadListener(AgentThreadListener listener) {
        listeners.add(listener);
    }

    public void RemoveAgentThreadListener(AgentThreadListener listener) {
        listeners.remove(listener);
    }

    private void NotifyLisnters() {
        for (AgentThreadListener agentThreadListener : listeners) {
            agentThreadListener.AgentThreadEnd();
        }
    }
}
