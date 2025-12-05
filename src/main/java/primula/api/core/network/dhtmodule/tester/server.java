/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.tester;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.hubimpl.IntegerHub;
import primula.api.core.network.dhtmodule.data.hubimpl.IntegerRange;
import primula.api.core.network.dhtmodule.routing.PropertiesLoader;
import primula.api.core.network.dhtmodule.routing.ServiceException;
import primula.api.core.network.dhtmodule.routing.impl.MercuryImpl;

/**
 *
 * @author VENDETTA
 */
public class server {
    static int port = 5122;
    static String homeDeskTop = "192.168.11.4";
    static String macPro_Home ="192.168.11.5";
    static String univMainPC = "133.220.114.125";
    static String univSubPC = "133.220.114.124";
    static String univMacPrp = "133.220.114.99";
    
    public static void main(String args[]){
    
        PropertiesLoader.loadPropertyFile();
        
        System.err.println("Property where to find propeties file::"+PropertiesLoader.PROPERTY_WHERE_TO_FIND_PROPERTY_FILE);
        
        MercuryImpl node = new MercuryImpl();
        Address address = new Address(macPro_Home, port);
        Address addressSub = new Address(homeDeskTop,port);
        Address addressPro = new Address(univMacPrp, port);
        try {
            node.create(address);
            IntegerHub intHub = new IntegerHub("INTEGER", 5, 1,100);
            IntegerRange range = new IntegerRange("INTEGER",0, 2000,2000);
            intHub.setRange(range);
         //   node.createHub(intHub);
            //Node sentNode = node.joinMercuryRequest();
            ArrayList<Address> addresess = new ArrayList<Address>();
            
            
//            addresess.add(address);
//            addresess.add(addressSub);
//            addresess.add(addressPro);
//
//            
//
//            node.createHubNetwork(3, intHub,addresess);
  //          node.sendHub(addressSub, intHub);
            
        }
//        catch (CommunicationException ex) {
//            Logger.getLogger(server.class.getName()).log(Level.SEVERE, null, ex);
//        }
        catch (ServiceException ex) {
            Logger.getLogger(server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
