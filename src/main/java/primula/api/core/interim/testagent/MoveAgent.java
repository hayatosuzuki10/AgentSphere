/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.testagent;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.network.message.MoveCheckAgent;

/**
 *
 * @author kurosaki
 */
public class MoveAgent extends AbstractAgent{

    @Override
    public void runAgent() {
        MoveCheckAgent agent = new MoveCheckAgent();
        AgentAPI.runAgent(agent);
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
