/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;

/**
 * 送信用受信テストAgent
 * @author migiside
 */
public class StressTestMessageReceiverAgent extends AbstractAgent implements IMessageListener {

    private ArrayList<AbstractEnvelope> envelopes = new ArrayList<AbstractEnvelope>();

    @Override
    public String getSimpleName() {
        return "StressTestMessageReceiverAgent";
    }

    @Override
    public void runAgent() {
        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception ex) {
            Logger.getLogger(StressTestMessageReceiverAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (envelopes.size() != 10000) {
        }
        MessageAPI.removeMessageListener(this);
        System.out.println("End" + Calendar.getInstance().getTime().toString());
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void receivedMessage(AbstractEnvelope envelope) {
        envelopes.add(envelope);
    }

    @Override
    public String getStrictName() {
        return getAgentID();
    }
}
