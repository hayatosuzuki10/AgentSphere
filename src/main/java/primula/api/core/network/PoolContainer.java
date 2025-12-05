/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network;

import java.net.SocketAddress;

import primula.api.core.agent.loader.ClassDataCollectionContainer;

/**
 *
 * @author yamamoto
 */
public class PoolContainer {

    private SocketAddress agentSphereInfo;
    private ClassDataCollectionContainer dataCollectionContainer;

    public PoolContainer(ClassDataCollectionContainer dataCollectionContainer) {
        this(dataCollectionContainer, null);
    }

    public PoolContainer(ClassDataCollectionContainer dataCollectionContainer, SocketAddress agentSphereInfo) {
        this.dataCollectionContainer = dataCollectionContainer;
        this.agentSphereInfo = agentSphereInfo;
    }

    /**
     * @return the dataCollectionContainer
     */
    public ClassDataCollectionContainer getDataCollectionContainer() {
        return dataCollectionContainer;
    }

    /**
     * @return the agentSphereInfo
     */
    public SocketAddress getAgentSphereInfo() {
        return agentSphereInfo;
    }
}
