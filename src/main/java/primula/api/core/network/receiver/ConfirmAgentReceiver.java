/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.receiver;

import primula.api.core.ICoreModule;

/**
 *
 * @author kurosaki
 */
/*
 * 現在使用されてない。そのうち消すと思う
 */
public class ConfirmAgentReceiver implements ICoreModule{
    private AgentReceiveThread agentReceivedThread = new AgentReceiveThread();

    @Override
    public void initializeCoreModele() {
        agentReceivedThread.start();
    }

    @Override
    public void finalizeCoreModule() {
        agentReceivedThread.requestStop();
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
}
