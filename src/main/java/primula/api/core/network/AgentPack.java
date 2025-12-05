/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network;

import java.io.Serializable;

import primula.agent.AbstractAgent;
import primula.api.core.agent.loader.ClassDataCollectionContainer;

/**
 *
 * @author yamamoto
 */
public class AgentPack implements Serializable {

    private AbstractAgent agent;
    private ClassDataCollectionContainer classDataCollectionContainer;
    private String agentSphereId;
    private int port;

    public AgentPack() {
    }

    public AgentPack(String agentSphereId, int port, AbstractAgent agent, ClassDataCollectionContainer classDataCollectionContainer) {
        this.agentSphereId = agentSphereId;
        this.port = port;
        this.classDataCollectionContainer = classDataCollectionContainer;
        this.agent = agent;
    }

    public String getAgentSphereId() {
        return agentSphereId;
    }

    public int getPort() {
        return port;
    }

    /**
     * @return the agent
     */
    public AbstractAgent getAgent() {
        return agent;
    }

    /**
     * @param agent the agent to set
     */
    public void setAgent(AbstractAgent agent) {
        this.agent = agent;
    }

    /**
     * @return the classDataCollectionContainer
     */
    public ClassDataCollectionContainer getClassDataCollectionContainer() {
        return classDataCollectionContainer;
    }

    /**
     * @param classDataCollectionContainer the classDataCollectionContainer to set
     */
    public void setClassDataCollectionContainer(ClassDataCollectionContainer classDataCollectionContainer) {
        this.classDataCollectionContainer = classDataCollectionContainer;
    }
}
