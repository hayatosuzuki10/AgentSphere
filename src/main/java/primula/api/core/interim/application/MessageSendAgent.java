/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.application;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

/**
 *
 * @author kurosaki
 */
public class MessageSendAgent extends AbstractAgent{
    private String text;
    
    MessageSendAgent(String text){
        this.text = text;
    }
    @Override
    public void runAgent() {
        try {
            KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName(IPAddress.IPAddress),55878);
            MessageAPI.send(address, new StandardEnvelope(new AgentAddress("primula.api.core.interim.application.DesktopAgent"), new StandardContentContainer(text)));
        } catch (UnknownHostException ex) {
            Logger.getLogger(MessageSendAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
