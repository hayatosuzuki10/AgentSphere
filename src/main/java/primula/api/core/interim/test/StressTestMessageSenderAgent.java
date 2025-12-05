/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.test;

import java.util.Calendar;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;

/**
 * 送信用負荷テストAgent
 * @author yamamoto
 */
public class StressTestMessageSenderAgent extends AbstractAgent {

    @Override
    public String getAgentName() {
        return this.getClass().getName();
    }

    @Override
    public void runAgent() {
        System.out.println("Start" + Calendar.getInstance().getTime().toString());
        for (int i = 0; i < 10000; i++) {
            StandardEnvelope envelope = new StandardEnvelope(new AgentAddress("StressTestMessageReceiverAgent"), new StandardContentContainer("hoge"));
            MessageAPI.send(envelope);
        }

    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
