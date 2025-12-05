/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.application;

import java.util.logging.Level;
import java.util.logging.Logger;
import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;

/**
 *
 * @author kurosaki
 */
public class DesktopAgent extends AbstractAgent implements IMessageListener{
    private String receive_text;
    String lineSeparator = System.getProperty("line.separator");
    @Override
    public void runAgent() {
        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception ex) {
            Logger.getLogger(DesktopAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        TestApplication test = new TestApplication();
        test.runApplication(this);
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getStrictName() {
        return getAgentID();
    }

    @Override
    public String getSimpleName() {
        return getAgentName();
    }

    @Override
    public void receivedMessage(AbstractEnvelope envelope) {
        StandardContentContainer content = (StandardContentContainer)envelope.getContent();
        //System.out.println("scc.getContent():"+content.getContent());デバッグ用
        this.receive_text = (String) content.getContent();
        ApplicationFrame.receive_area.append(receive_text);
        ApplicationFrame.receive_area.append(lineSeparator);
    }
    
}
