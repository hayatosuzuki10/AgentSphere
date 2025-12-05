/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.DataRange;
import primula.api.core.network.dhtmodule.data.Hub;
import primula.api.core.network.dhtmodule.data.KeyValuePair;
import primula.api.core.network.dhtmodule.data.histogramData;

/**
 *ノードの抽象クラス
 * @author VENDETTA
 */
public abstract class Node implements Serializable{
    
    protected Address nodeAddress;

    public final Address getNodeAddress(){
        return nodeAddress;
    }

    
    public final void setNodeAddress(Address address){
        this.nodeAddress=address;
    }
    
    public abstract Node findSuccessor(Address address) throws CommunicationException;
    
    public abstract void joinRequest(Address fromAddress )throws CommunicationException;
    
    public abstract void leavesNetwork(Node predecessor) throws CommunicationException;
    
    public abstract void disconnect();
    
    public abstract void insertEntry(String typeOfHub,DataRange range,KeyValuePair pair) throws CommunicationException;
    
    public abstract void insertEntry(String typeOfHub,DataRange range,ArrayList<KeyValuePair> pair) throws CommunicationException;
    
    public abstract List<Serializable> getEntry(String typeOfHub,DataRange range) throws CommunicationException;
    
    public abstract List<Node> notifyToPredecessor(Node potentialPredecessor) throws CommunicationException;
    
    public abstract histogramData[] getHistogramData(int TTL,String typeOfHub) throws CommunicationException;
    
    public abstract Node joinMercuryRequest(Node fromNode) throws CommunicationException;
    
    public abstract void sendHub(Address toAddress,Hub hubToSend) throws CommunicationException;
    
    public abstract void createHubNetwork(int TTL,Hub hubToSplit,ArrayList<Address> addresses) throws CommunicationException;
    
    public abstract void ping() throws CommunicationException;
    
    public abstract void acceptHub(Address fromAddress,Hub hubToAccept);
    
    
    @Override
    public final boolean equals(Object obj){
        if(obj==null||!(obj instanceof Node)){
            return false;
        }
        return ((Node)obj).nodeAddress.equals(this.nodeAddress);
    }

    @Override
    public String toString(){
        String address="null";
        if(this.nodeAddress!=null){
            address=this.nodeAddress.toString();
        }
        
        return "Node[type="+this.getClass().getSimpleName()+",Address="+address+"]";
    }
    
}
