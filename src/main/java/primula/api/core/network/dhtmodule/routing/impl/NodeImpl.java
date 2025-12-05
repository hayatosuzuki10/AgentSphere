/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.DataRange;
import primula.api.core.network.dhtmodule.data.Hub;
import primula.api.core.network.dhtmodule.data.HubContainer;
import primula.api.core.network.dhtmodule.data.KeyValuePair;
import primula.api.core.network.dhtmodule.data.histogramData;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;
import primula.api.core.network.dhtmodule.nodefunction.Endpoint;
import primula.api.core.network.dhtmodule.nodefunction.Node;
import primula.api.core.network.dhtmodule.nodefunction.impl.SocketProxy;
import primula.api.core.network.dhtmodule.utill.Logger;

/**
 *
 * @author VENDETTA
 */
public class NodeImpl extends Node {

    private Endpoint myEndpoint = null;
    
    private MercuryImpl impl;
    
    private Logger logger;
    
    private Executor asynExecutor;
    
    private Lock notifyLock;
    
    private HubContainer hubContainer;
    
    private int dividescore=2;
    
    private int callCount=1;

    
    NodeImpl(MercuryImpl impl,Address nodeAddress,HubContainer hubContainer ){
        
        if(
                //impl==null||
                nodeAddress==null){
            throw new NullPointerException("At least one parameter is null,Which is not permitted!");
        }
        
        this.impl=impl;
        this.logger=Logger.getLogger(NodeImpl.class.getName());
        this.asynExecutor=impl.getAsyncExecutor();
        this.nodeAddress=nodeAddress;
        this.hubContainer= hubContainer;
        this.notifyLock=new ReentrantLock(true);
        
        this.myEndpoint=Endpoint.createEndpoint(this, nodeAddress);
        this.myEndpoint.listen();
    }
    
    
    final void acceptEntries(){
        this.myEndpoint.acceptEntries();
    }
    
    public final void disconnect(){
        this.myEndpoint.disconnect();
    }
    
    
    @Override
    public Node findSuccessor(Address key)  {
       return this.impl.findSuccessor(key);
    }
    
    
      @Override
    public void insertEntry(String typeOfHub,DataRange range,KeyValuePair pair) throws CommunicationException {
       this.impl.insertObject(typeOfHub, range, pair);
    }
      
    @Override
    public void insertEntry(String typeOfHub,DataRange range,ArrayList<KeyValuePair> pair) throws CommunicationException {
       this.impl.insertObject(typeOfHub, range, pair);
    }
      
    @Override
      public List<Serializable> getEntry(String typeOfHub,DataRange range) throws CommunicationException{
        return   this.impl.getObject(typeOfHub, range);
      }


    
    
        @Override
    public void createHubNetwork(int TTL, Hub hubToSplit, ArrayList<Address> addresses) throws CommunicationException {
            logger.debug("Creating Hub Network"+hubToSplit.getType());
           DataRange rangeToManage = hubToSplit.getRange();
           DataRange lestOfRange = rangeToManage.divideRange(addresses.size()+1);
           Hub lestOfHub = hubToSplit;
           lestOfHub.setRange(lestOfRange);
           int counter = addresses.size()-TTL;
           logger.debug("TTL is "+TTL);
           if(counter != 0){
             hubToSplit.setDirectPredecessor(SocketProxy.create(this.getNodeAddress(), addresses.get(counter-1)));
           }else{
             hubToSplit.setDirectPredecessor(SocketProxy.create(this.getNodeAddress(), addresses.get(addresses.size()-1)));
           }
           if(counter!=TTL){
             hubToSplit.setDirectSuccessor(SocketProxy.create(this.getNodeAddress(), addresses.get(counter+1)));
             Node nextNode =hubToSplit.getDirectSuccessor();
             nextNode.createHubNetwork(TTL-1, hubToSplit, addresses);
           }else{
             hubToSplit.setDirectSuccessor(SocketProxy.create(this.getNodeAddress(),addresses.get(0) ));
           }
           
           this.hubContainer.addHub(hubToSplit);
           
           
    }
    
    @Override
    public Node joinMercuryRequest(Node fromNode) throws CommunicationException {
        if(this.impl.getAddress()==fromNode.getNodeAddress()){
            
            return fromNode.joinMercuryRequest(fromNode);
        }else{
            boolean machineCheckFlag = true;
            if(machineCheckFlag==true){
                return fromNode;
            }else{
                return null;
            }
        }
        
    }
    
    
    
    @Override
    public histogramData[] getHistogramData(int TTL, String typeOfHub) throws CommunicationException {
        
        histogramData[] datas = new histogramData[TTL];
        histogramData data  = new histogramData(TTL);
        data.setHistogramData(this.hubContainer.getDataRange(typeOfHub), this.hubContainer.getDataRangeWithInteger(typeOfHub), this.getNodeAddress());
        datas[TTL] =data;
        if(TTL!=0){
            Node nextNode = this.hubContainer.getHub(typeOfHub).getNodeFromLDT();
            datas = nextNode .getHistogramData(TTL-1, typeOfHub);
        }
        return datas;
    }
    
    @Override
    public void sendHub(Address toAddress,Hub hubToSend) throws CommunicationException {
        if(this.nodeAddress!=toAddress){
        this.logger.debug("start sending Hub");
        Node nodeToSend = SocketProxy.create(this.nodeAddress, toAddress);
        //DataRange range =hubToSend.divideRange(dividescore);
        //hubToSend.setRange(range);
        
        nodeToSend.sendHub(this.nodeAddress, hubToSend);
        }else{
            this.hubContainer.addHub(hubToSend);
        }
    }
    
    @Override
    public final void acceptHub(Address fromAddress ,Hub newHub){
        try {
            SocketProxy proxy =SocketProxy.create(this.nodeAddress, fromAddress);
            newHub.setDirectPredecessor(proxy);
            newHub.setDirectSuccessor(proxy);
            this.hubContainer.addCrossHubLink(proxy);
            
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(NodeImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(!this.nodeAddress.getHost().equals(fromAddress.getHost())){
        this.hubContainer.addHub(newHub);
        
        }
    }
        
    @Override
    public void leavesNetwork(Node predecessor) throws CommunicationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }



    @Override
    public List<Node> notifyToPredecessor(Node potentialPredecessor) throws CommunicationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
//
//    @Override
//    public RefsAndEntries notifyAndCopyEntries(Node potentialPredecessor) throws CommunicationException {
//        this.notifyLock.lock();
//        
//        try{
//            Set<Entry> copiedEntries = null;//this.entries.getEntriesInterval(this.nodeID, potentialPredecessor.getNodeID());
//            return new RefsAndEntries(this.notifyToPredecessor(potentialPredecessor), copiedEntries);
//        }finally{
//            this.notifyLock.unlock();
//        }
//    }

    @Override
    public void ping() throws CommunicationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void joinRequest(Address fromAddress){
        this.impl.joinRequest(fromAddress);
    }




}
