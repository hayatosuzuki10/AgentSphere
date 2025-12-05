/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction;


import static primula.api.core.network.dhtmodule.utill.Logger.LogLevel.DEBUG;
import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.nodefunction.impl.SocketProxy;
import primula.api.core.network.dhtmodule.utill.Logger;


/**
 *
 * @author VENDETTA
 */
public abstract class Proxy extends Node {
    
    private final static Logger logger = Logger.getLogger(Proxy.class.getName());
    
    protected Proxy(Address address){
        if(address==null){
            throw new IllegalArgumentException("Address must not be null");
        }
        this.nodeAddress=address;
        logger.info("Proxy with Address"+ address +"initialized");
        
    }
    
    public static Node createConnection(Address sourceAddress,Address destinationAddress) throws CommunicationException{
        if(sourceAddress==null||destinationAddress==null){
            throw new NullPointerException("Address must not be null");
        }
        if(sourceAddress.equals(destinationAddress)){
           logger.fatal("Address are equal : this address ="+sourceAddress.toString());
            throw new IllegalArgumentException("Address must not be null");
        }
        
        boolean debug = logger.isEnabledFor(DEBUG);
        if(debug){
            logger.debug("Trying to create Proxy for connection to"+destinationAddress);
        }
        
        Node node = null;

        node =SocketProxy.create(sourceAddress, destinationAddress);
                
        if(debug){
            logger.debug("SocketProxy careated.");
        }
        
        return node;
    }
    
    
}
