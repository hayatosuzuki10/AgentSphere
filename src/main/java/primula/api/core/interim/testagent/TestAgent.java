/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.testagent;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.util.KeyValuePair;


/**
 *
 * @author kurosaki
 */
public class TestAgent extends AbstractAgent implements IMessageListener{
    private List<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>> list = new ArrayList<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>>();
    private boolean moveCheck;
    
    @Override
    public synchronized void runAgent(){  
        AgentTest agent = new AgentTest();
        AgentAPI.runAgent(agent);
        AgentAPI.messageSendConf(this, this.getSimpleName(), agent.getAgentID());
        try {
            wait();
            if(moveCheck == true){
                KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName("192.168.114.12"), 55878);
                AgentAPI.migration(address, agent); 
            }
            wait();
            System.out.println("送信完了");
        } catch (UnknownHostException ex) {
            Logger.getLogger(TestAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(TestAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        MessageAPI.removeMessageListener(this);
    }
    
    @Override
    public void requestStop(){
        
    }

    
    private void print(){
        StringBuilder buffer = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        for(int i=0;i<list.size();i++){
            buffer.append("[AgentSphereID:");
            buffer.append(list.get(i).getKey());
            buffer.append("   IPaddress:");
            buffer.append(list.get(i).getValue().getKey());
            buffer.append("]");
            if(i != list.size()-1){
                buffer.append(lineSeparator);
            }
        }
        String text = new String(buffer);
        System.out.println(text);
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
    public synchronized void receivedMessage(AbstractEnvelope envelope) {
        if((Boolean)((StandardContentContainer)envelope.getContent()).getContent() == true){
            moveCheck = true;            
            System.out.println("true");
        }
        else{
            moveCheck = false;
            System.out.println("false");
        }
        notify();
    }
}
