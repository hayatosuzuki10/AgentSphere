/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.monitor;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.NotFoundException;
import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.NetworkAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;

/**
 *
 * @author selab
 */
public class MonitorTestAgent extends AbstractAgent {

    @Override
    public String getAgentName() {
        return "MonitorTestAgent";
    }

    @Override
    public synchronized void runAgent() {
        KeyValuePair<InetAddress, Integer> address;
        for (KeyValuePair<String, KeyValuePair<InetAddress, Integer>> x : NetworkAPI.getAddresses()) {
            try {
                address = NetworkAPI.getAddressByAgentSphereId(x.getKey());
                String hello="hello";
                StandardEnvelope envelope = new StandardEnvelope(new AgentAddress("StationaryAgent"),(AbstractContentContainer) (Serializable) hello);
                MessageAPI.send( address, envelope);
            } catch (NotFoundException ex) {
                Logger.getLogger(MonitorFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
