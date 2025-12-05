/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.message;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.util.KeyValuePair;


/**
 *
 * @author kurosaki
 */
public class MoveCheckAgent extends AbstractAgent implements IMessageListener{
    private List<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>> list = new ArrayList<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>>();
    private boolean timeOut = true;
    private boolean moveCheck;
    @Override
    public synchronized void runAgent() {
        try {
            MessageAPI.registerMessageListener(this);
            KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName("192.168.114.12"),55878);
            MessageAPI.send(address, new StandardEnvelope(new AgentAddress("primula.api.core.interim.testagent.MessageQueueAgent"), new StandardContentContainer(this.getSimpleName())));
            //wait();
            for(int i=0; i < 100; i++){
                Thread.sleep(2000);
            }
        } catch (Exception ex) {
            Logger.getLogger(AgentAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(timeOut == true){
            //TODO:他のASへメッセージを送る処理を書く
        }
        else{
            /**
             * TODO
             * moveCheckの値によってエージェントを移動させるか
             * 他のASへメッセージを送る
             */
        }
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
    public synchronized void receivedMessage(AbstractEnvelope envelope) {
        timeOut = false;
        if((Boolean)((StandardContentContainer)envelope.getContent()).getContent() == true){
            moveCheck = true;
            MessageAPI.removeMessageListener(this);
            System.out.println("true");
        }
        else{
            moveCheck = false;
            System.out.println("false");
        }
        notify();
    }

    //デバッグ用
    public void print(){
        StringBuilder buffer = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        for(int i=0;i<list.size();i++){
            buffer.append("[AgentSphereID:");
            buffer.append(list.get(i).getKey());
            buffer.append(" IPAddress:");
            buffer.append(list.get(i).getValue().getKey());
            buffer.append("]");
            if(i != list.size()-1){
                buffer.append(lineSeparator);
            }
        }
        String text = new String(buffer);
        System.out.println(text);
    }
}
