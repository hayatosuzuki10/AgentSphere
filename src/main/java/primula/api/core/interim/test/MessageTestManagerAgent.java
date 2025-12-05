/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.SystemAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.util.KeyValuePair;

/**
 *
 * @author yamamoto
 */
public class MessageTestManagerAgent extends AbstractAgent implements IMessageListener {

    private AbstractEnvelope envelope;

    @Override
    public synchronized void runAgent() {
        try {
            MessageAPI.registerMessageListener(this);
            AgentAddress agentAddress = new AgentAddress(getAgentID());
            MessageTestAgent agent = new MessageTestAgent(SystemAPI.getAgentSphereId(), agentAddress);
            AgentAPI.migration(
                    new KeyValuePair<InetAddress, Integer>(
                    InetAddress.getByName(SystemAPI.getConfigData("FirstAccessAddress").toString()),
                    Integer.parseInt(SystemAPI.getConfigData("FirstAccessAddressPort").toString())),
                    agent);
            try {
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageTestManagerAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(((StandardContentContainer) envelope.getContent()).getContent());
            MessageAPI.removeMessageListener(this);
        } catch (UnknownHostException ex) {
            Logger.getLogger(MessageTestManagerAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
        }
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public synchronized void receivedMessage(AbstractEnvelope envelope) {
        this.envelope = envelope;
        notify();
    }

    @Override
    public String getStrictName() {
        return getAgentID();
    }

    @Override
    public String getSimpleName() {
        return "MessageTestManagerAgent";
    }
}
