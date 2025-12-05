/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.nodefunction.impl.SocketEndpoint;
import primula.api.core.network.dhtmodule.utill.Logger;


/**
 *
 * @author VENDETTA
 */
public abstract class Endpoint {
    
    private static final Logger loggger = Logger.getLogger(Endpoint.class);
    
    protected static final Map<Address,Endpoint> endpoints = new HashMap<Address, Endpoint>();
    
    public static final int STARTED=0;
    
    public static final int LISTENING = 1;
    
    public static final int ACCEPT_ENTRIES= 2;
    
    public static final int DISCONNECTED = 3;
    
    public static final List<String> METHODS_ALLOWED_IN_ACCEPT_ENTRIES;
    
    static{
        String[] temp = new String[] {
            "inertEntry","removeEntry","retrieveEntires"
        };
        Arrays.sort(temp);
        List<String> list = new ArrayList<String>(Arrays.asList(temp));
        METHODS_ALLOWED_IN_ACCEPT_ENTRIES=Collections.unmodifiableList(list);
    }
    
    private int state=-1;
    
    protected Address address;
    
    protected Node node;
    
    private Set<EndpointStateListener> listeners = new HashSet<EndpointStateListener>();
    
    protected Endpoint(Node node, Address address){
        loggger.info("Endpoint for "+node+" with address "+address+" created.");
        this.node=node;
        this.address=address;
        this.state=STARTED;
    }
    
    public final Node getNode(){
        return this.node;
    }
    
    public final void register(EndpointStateListener listener){
        this.listeners.add(listener);
    }
    
    public final void deregister(EndpointStateListener listener){
        this.listeners.remove(listener);
    }
    
    protected void notify(int s){
        loggger.debug("notifying state change");
        
        synchronized(this.listeners){
            loggger.debug("Size of Listeners = "+this.listeners.size());
            for(EndpointStateListener listener:this.listeners){
                listener.notify(s);
            }
        }
    }
    
    public Address getAddresss(){
        return this.address;
    }
    
    public final int getState(){
        return this.state;
    }
    
    protected void setState(int state){
        this.state=state;
        this.notify(state);
    }
    
    public final void listen(){
        this.state=LISTENING;
        this.notify(this.state);
        this.openConnections();
    }
    
    protected abstract void openConnections();
    
    public final void acceptEntries(){
        loggger.info("AcceptEntries() called");
        this.state=ACCEPT_ENTRIES;
        this.notify(this.state);
        this.entriesAcceptable();
    }
  
    protected abstract void entriesAcceptable();
    
    public final void disconnect(){
        this.state=STARTED;
        loggger.info("Disconnecting");
        this.notify(this.state);
        this.closeConnections();
        synchronized(endpoints){
            endpoints.remove(this.node.nodeAddress);
        }
    }
    
    protected abstract void closeConnections();
    
    public static Endpoint createEndpoint(Node node,Address address){
        synchronized(endpoints){
            if(endpoints.containsKey(address)){
                throw new RuntimeException("Endpoint already created!");
            }
            Endpoint endpoint=null;
            
            if(address==null){
                throw new IllegalArgumentException("address must not be null!");
            }
            endpoint =new SocketEndpoint(node,address);
            
            endpoints.put(address, endpoint);
            return endpoint;
        }
    }
    
    public static Endpoint getEndpoint(Address address){
           synchronized(endpoints){
               Endpoint ep = endpoints.get(address);
               loggger.debug("Endpoint for Address "+address+": "+ep);
               return ep;
           }
    }
    
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("[Endpoint for ");
        builder.append(this.node);
        builder.append(" with address ");
        builder.append(this.address);
        builder.append("]");
        return builder.toString();
    }
}
