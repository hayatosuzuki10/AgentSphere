/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.DataRange;
import primula.api.core.network.dhtmodule.data.Hub;
import primula.api.core.network.dhtmodule.data.KeyValuePair;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;

/**
 *
 * @author VENDETTA
 */
public interface Mercury {
    
    public abstract Address getAddress();
    
    public abstract void setAddress(Address nodeAddress) throws IllegalArgumentException;

    
    public abstract void create() throws ServiceException;
    
    public abstract void create(Address localAddress) throws ServiceException;
    
    public abstract void join(Address bootstrapAddress) throws ServiceException;
    
    public abstract void join(Address localAddress,Address bootstrapAddress) throws ServiceException;
    
    public abstract void joinRequest(Address address) throws ServiceException;
    
    public abstract void leave() throws ServiceException;
    
    public abstract void createHub(Hub hubToAdd);
    
    public abstract void insertObject(String typeOfHub,DataRange range,KeyValuePair pair) throws CommunicationException;
    
    public abstract void insertObject(String typeOfHub,DataRange range,ArrayList<KeyValuePair> pairs) throws CommunicationException;
    
    public abstract List<Serializable> getObject(String typeOfHub,DataRange range) throws CommunicationException;
    
   public abstract void sendHub(Address toAddress,Hub hubToSend)throws CommunicationException;
   
   public abstract void createHubNetwork(int TTL,Hub hubToSplit,ArrayList<Address> addresess)throws CommunicationException;
}
