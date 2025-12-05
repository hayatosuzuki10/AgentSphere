/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.receiver;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.api.core.network.receiver.testsocket.SenderThread;
import primula.util.KeyValuePair;

/**
 *
 * @author kurosaki
 */
/*
 * 現在使用されてない。そのうち消すと思う
 */
public class AgentReceiveThread extends Thread implements IAgentListener{
    private static AgentRegistrator agentServer = AgentRegistrator.getInstance();
    private String ID;
    private boolean requestStop = false;
    private String pair;
    public static BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    
    AgentReceiveThread(){
        setID();
    }
    
    @Override
    public void run(){
        while(requestStop == false){
            try {
                pair = queue.take();
                //agentServer.registerAgentListener(this, pair);
                System.out.println("送られてくるエージェントを登録しました-->"+pair);
            } catch (Exception ex) {
                Logger.getLogger(AgentReceiveThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("agentreceived thread finish");
    }

    @Override
    public String getStrictName() {
        return this.ID;
    }

    @Override
    public void receivedAgent(String ID) {
        System.out.println("エージェント受信完了");
        //agentServer.removeAgentListener(this);
        try {
            SenderThread.task.put(new KeyValuePair<String, String>("finish", ID));
        } catch (InterruptedException ex) {
            Logger.getLogger(AgentReceiveThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        //notify();
    }
    
    private void setID(){
        this.ID = UUID.randomUUID().toString();
    }
    
    public void requestStop(){
        this.requestStop = true;
        try {
            queue.put("dummy");
        } catch (InterruptedException ex) {
            Logger.getLogger(AgentReceiveThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
