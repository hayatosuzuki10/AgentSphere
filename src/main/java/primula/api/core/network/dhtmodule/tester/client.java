/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.tester;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.KeyValuePair;
import primula.api.core.network.dhtmodule.data.hubimpl.IntegerHub;
import primula.api.core.network.dhtmodule.data.hubimpl.IntegerRange;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;
import primula.api.core.network.dhtmodule.nodefunction.impl.SocketProxy;
import primula.api.core.network.dhtmodule.routing.PropertiesLoader;
import primula.api.core.network.dhtmodule.routing.ServiceException;
import primula.api.core.network.dhtmodule.routing.impl.MercuryImpl;
import primula.api.core.network.dhtmodule.tester.datasample.TestEntry;

/**
 *
 * @author VENDETTA
 */
public class client {
    
    static int port = 5122;
    static String homeDeskTop = "192.168.11.4";
    static String macPro_Home ="192.168.11.5";
    static String univMainPC = "133.220.114.125";
    static String univSubPC = "133.220.114.124";
    static String univMacPro = "133.220.114.126";
    
   public static void main(String args[]){
 
        PropertiesLoader.loadPropertyFile();
       Address bootstrapAddress = new Address(macPro_Home, port);
       Address acceptIncomingConnections = new Address(homeDeskTop, port);
       MercuryImpl impl = new MercuryImpl();
       impl.setAddress(acceptIncomingConnections);
  
            //impl.join(bootstrapAddress);
       IntegerRange intRange = new  IntegerRange("INTEGER", 50, 400, 1000);
            IntegerHub hub = new IntegerHub("INTEGER", 3, 1000, 0);
            hub.setRange(intRange);
            IntegerHub hub2 = new IntegerHub("INTEGER",3,1000,0);
            intRange = new IntegerRange("INTEGER", 0, 50, 1000);
            hub2.setRange(intRange);
            try {
                SocketProxy proxy =SocketProxy.create(acceptIncomingConnections, bootstrapAddress);
                hub2.setDirectPredecessor(proxy);
                hub2.setDirectSuccessor(proxy);
                
                proxy = SocketProxy.create(bootstrapAddress, acceptIncomingConnections);
                hub.setDirectPredecessor(proxy);
                hub.setDirectSuccessor(proxy);
                
            } catch (CommunicationException ex) {
                Logger.getLogger(client.class.getName()).log(Level.SEVERE, null, ex);
            }
        try {
            impl.join(bootstrapAddress);
        } catch (ServiceException ex) {
            Logger.getLogger(client.class.getName()).log(Level.SEVERE, null, ex);
        }
            
            impl.sendHub(bootstrapAddress, hub);
            impl.createHub(hub2);
            IntegerRange range = new IntegerRange("INTEGER",100, 100,100);
            TestEntry entry = new TestEntry(100, "OK!", 20.2);
            KeyValuePair pair = new KeyValuePair(entry.getIntegerKey(),entry);
            try {
                impl.insertObject("INTEGER", range, pair);
               List<Serializable> result= impl.getObject("INTEGER", range);
               TestEntry tester =(TestEntry)result.get(0);
               System.err.println("ENTRY ARRIVED! integer key is" +tester.getIntegerKey());
            } catch (CommunicationException ex) {
                Logger.getLogger(client.class.getName()).log(Level.SEVERE, null, ex);
            }

            
            System.err.println("E_N_D");
        }
   
    
    }

