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
import primula.util.KeyValuePair;
/**
 *
 * @author kurosaki
 */
public class AgentTest extends AbstractAgent{
    @Override
    public void runAgent(){
        for(int i=0;i<100;i++){
            System.out.println("i:"+ i);
            /*
            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(AgentTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            * 
            */
        }
        //requestStop();       
    }
    
    @Override
    public void requestStop() {
        System.out.println("finish"); 
        try {
            KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName("192.168.114.12"),55878);  //現在IP直打ち。ASIDとかから指定できるようにしたい
            try{               
                
            } catch (Exception ex) {
                Logger.getLogger(AgentTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(TestAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
   