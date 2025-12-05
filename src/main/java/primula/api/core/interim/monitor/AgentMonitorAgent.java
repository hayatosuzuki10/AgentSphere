/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.monitor;

import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.NotFoundException;
import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.NetworkAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;

/**
 *
 * @author onda
 */
public class AgentMonitorAgent extends AbstractAgent implements IMessageListener {

    AbstractEnvelope envelope;

    @Override
    public String getSimpleName() {
        return getAgentName();
    }

    @Override
    public void runAgent() {
        AgentMonitor agent = new AgentMonitor();
        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception ex) {
            Logger.getLogger(AgentMonitorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        agent.runMonitor((AgentMonitorAgent) this);
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized StandardContentContainer recieveMessage(String asID, ReplyContentContainer content) {
        StandardEnvelope sendEnvelope = new StandardEnvelope(new AgentAddress("primula.api.core.interim.monitor.StationaryAgent"), content);

        try {
//            MessageAPI.registerMessageListener(this);
            //指定されたAgentSphereにsendEnvelopeを送信
            System.out.println("メッセージを送信します");
            System.out.println(NetworkAPI.getAddressByAgentSphereId(asID));
            MessageAPI.send(NetworkAPI.getAddressByAgentSphereId(asID), sendEnvelope);
            System.out.println("送信しました");
            try {
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(AgentMonitorAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("受け取りました");
            MessageAPI.removeMessageListener(this);
        } catch (NotFoundException ex) {
            Logger.getLogger(AgentMonitorAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
        }
        System.out.println("受け取ったコンテンツを返します");
        return (StandardContentContainer) envelope.getContent();
    }

    @Override
    public synchronized void receivedMessage(AbstractEnvelope envelope) {
        System.out.println("recieved!");
        this.envelope = envelope;
        notify();
    }

    @Override
    public String getStrictName() {
        return getAgentID();
    }
}
