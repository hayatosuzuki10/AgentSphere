/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.testagent;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.KeyValuePair;

/**
 *
 * @author kurosaki
 */
public class CommunicationAgent extends AbstractAgent{
    private boolean migrate_flag = false;
    private boolean return_flag = false;
    @Override
    public void runAgent() {
        if(return_flag == true){
            System.out.println("戻ってきた");
        }
        else{
            if(migrate_flag == false){
                migrate_flag = true;
                try {
                    KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName("192.168.114.13"), 55878);
                    AgentAPI.migration(address, this); 
                } catch (UnknownHostException ex) {
                    Logger.getLogger(CommunicationAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else{
                return_flag = true;
                System.out.println("移動後");
                KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress, Integer>(this.getmyIP(), 55878);
                AgentAPI.migration(address, this); 
            }
        }
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
