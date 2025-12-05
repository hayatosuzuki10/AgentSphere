/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.manage;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;

/**
 *
 * @author kurosaki
 */
public class ManagerAgent extends AbstractAgent{

    @Override
    public void runAgent() {
        Manager agent = new Manager();
        AgentAPI.runAgent(agent);
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
