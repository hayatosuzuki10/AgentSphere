/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.DataRange;
import primula.api.core.network.dhtmodule.data.Hub;
import primula.api.core.network.dhtmodule.data.HubContainer;
import primula.api.core.network.dhtmodule.data.KeyValuePair;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;
import primula.api.core.network.dhtmodule.nodefunction.Node;
import primula.api.core.network.dhtmodule.nodefunction.Proxy;
import primula.api.core.network.dhtmodule.nodefunction.impl.SocketProxy;
import primula.api.core.network.dhtmodule.routing.AsynMercury;
import primula.api.core.network.dhtmodule.routing.Mercury;
import primula.api.core.network.dhtmodule.routing.Report;
import primula.api.core.network.dhtmodule.routing.ServiceException;
import primula.api.core.network.dhtmodule.utill.Logger;

/**
 *
 * @author VENDETTA
 */
public final class MercuryImpl implements Mercury,Report,AsynMercury {

    private static final int ASYNC_CALL_THREADS = Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".AsyncThread.no"));
    
    private static final int STABILIZE_TASK_START=Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".StabilizeTask.start"));
    
    private static final int STABILIZE_TASK_INTERVAL=Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".StabilizeTask.interval"));
    
    private static final int FIX_LONG_TABLE_START =Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".FixLongTable.start"));
    
    private static final int FIX_LONG_TABLE_INTERVAL = Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".FixLongTable.interval"));
    
    private static final int CHECK_PREDECESSOR_TASK_START = Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".CheckPredecessorTask.start"));
    
    private static final int CHECK_PREDECESSOR_TASK_INTERVAL = Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".CheckPredecessorTask.interval"));
    
    private static final int NUMBER_OF_SUCCESSORS=(Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".successors"))<1)?1:Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".successors"));
    
    private static final int NUMBER_OF_PREDECESSORS = (Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".predecessors"))<1)?1:Integer.parseInt(System.getProperty(MercuryImpl.class.getName()+".predecessors"));
    
    protected Logger logger;
    
    private NodeImpl localNode;
    
    private ScheduledExecutorService maintenanceTasks;
    
    private ExecutorService asynExecutor;
    
    private Address localAddress;
    
    protected HubContainer hubContainer;

    @Override
    public String printEntries() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String printLongDistanceTable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String printSuccessorList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String printReferences() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String printPredecessor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void createHub(Hub hubToAdd) {
        //自分にHubを追加
        this.hubContainer.addHub(hubToAdd);
        //レプリケートするノードにHubを追加
    //    hubToAdd.setType(hubToAdd.getType()+"_REPLICA");
        
       // this.sendHub(hubToAdd.getDirectSuccessor().getNodeAddress(), hubToAdd);
     //   this.sendHub(hubToAdd.getDirectPredecessor().getNodeAddress(), hubToAdd);
        
    }
    
    @Override
    public void sendHub(Address toAddress,Hub hubToSend){
        try {
            this.localNode.sendHub(toAddress, hubToSend);
            hubToSend.setType(hubToSend.getType()+"_REPLICA");
            this.hubContainer.addHub(hubToSend);
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(MercuryImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

   @Override
    public void insertObject(String typeOfHub,DataRange range, KeyValuePair pair) throws CommunicationException{
        if(this.hubContainer.isMainNetwork()){
            if(this.hubContainer.hasHub(typeOfHub)){
                
                int indexOfHub = this.hubContainer.isRange(range);
                if(indexOfHub==0){
                    this.hubContainer.setEntry(typeOfHub, indexOfHub,pair);
                    
                    //バックアップHubにデータ送信
                    if(typeOfHub.length()>8){
                    if(!typeOfHub.substring(typeOfHub.length()-8).equals("_REPLICA")){
                             this.replicateObject(typeOfHub, range, pair);
                    }
                }else{
                        this.replicateObject(typeOfHub, range, pair);
                    }
                }
                else if(indexOfHub==-1){
                    //Hubの担当範囲外のデータの場合
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, range, pair);
                }
                  else if(indexOfHub==1){
                    //Hubの担当範囲外のデータの場合、範囲内＆範囲以上
                    range.setLower(hubContainer.getDataRange(typeOfHub).getUpperBound());
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, range, pair);
                }
                 else if(indexOfHub==2){
                    //Hubの担当範囲外のデータの場合、範囲内＆範囲以下≒２
                     range.setUpper(hubContainer.getDataRange(typeOfHub).getLowerBound());
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, range, pair);
                }                 
                 else if(indexOfHub==3){
                    //Hubの担当範囲外のデータの場合、
                     DataRange newRangeUpper = range;
                     DataRange newRangeLower = range;
                     newRangeLower.setUpper(hubContainer.getDataRange(typeOfHub).getLowerBound());
                     newRangeUpper.setLower(hubContainer.getDataRange(typeOfHub).getUpperBound());
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, newRangeLower, pair);
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, newRangeUpper, pair);
                }
                
            }else{
               //該当するタイプのHubが存在しない場合 
                 this.hubContainer.getAccessLinkRandomly().insertEntry(typeOfHub, range, pair);
            }      
        }else{
          //自分自身がサブノードの場合
           Node parentNode = this.hubContainer.getAccessLinkRandomly();
           parentNode.insertEntry(typeOfHub, range, pair);
        
        }
        
        
    }
   
      @Override
    public void insertObject(String typeOfHub,DataRange range, ArrayList<KeyValuePair> pairs) throws CommunicationException{
        if(this.hubContainer.isMainNetwork()){
            if(this.hubContainer.hasHub(typeOfHub)){
                
                int indexOfHub = this.hubContainer.isRange(range);
                if(indexOfHub==0){
                    this.hubContainer.setEntry(typeOfHub, indexOfHub,pairs);
                    
                    //バックアップHubにデータ送信
                    if(typeOfHub.length()>8){
                    if(!typeOfHub.substring(typeOfHub.length()-8).equals("_REPLICA")){
                             this.replicateObject(typeOfHub, range, pairs);
                    }
                }else{
                        this.replicateObject(typeOfHub, range, pairs);
                    }
                }
                else if(indexOfHub==-1){
                    //Hubの担当範囲外のデータの場合
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, range, pairs);
                }
                  else if(indexOfHub==1){
                    //Hubの担当範囲外のデータの場合、範囲内＆範囲以上
                    range.setLower(hubContainer.getDataRange(typeOfHub).getUpperBound());
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, range, pairs);
                }
                 else if(indexOfHub==2){
                    //Hubの担当範囲外のデータの場合、範囲内＆範囲以下≒２
                     range.setUpper(hubContainer.getDataRange(typeOfHub).getLowerBound());
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, range, pairs);
                }                 
                 else if(indexOfHub==3){
                    //Hubの担当範囲外のデータの場合、
                     DataRange newRangeUpper = range;
                     DataRange newRangeLower = range;
                     newRangeLower.setUpper(hubContainer.getDataRange(typeOfHub).getLowerBound());
                     newRangeUpper.setLower(hubContainer.getDataRange(typeOfHub).getUpperBound());
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, newRangeLower, pairs);
                    this.hubContainer.getHub(typeOfHub).getDirectSuccessor().insertEntry(typeOfHub, newRangeUpper, pairs);
                }
                
            }else{
               //該当するタイプのHubが存在しない場合 
                 this.hubContainer.getAccessLinkRandomly().insertEntry(typeOfHub, range, pairs);
            }    
        }else{
          //自分自身がサブノードの場合
           Node parentNode = this.hubContainer.getAccessLinkRandomly();
           parentNode.insertEntry(typeOfHub, range, pairs);
        
        }
        
        
    }
   
   
   
   
    private void replicateObject(String typeOfHub,DataRange range, KeyValuePair pair){
        typeOfHub=typeOfHub+"_REPLICA";
        DataRange newRange = range;
        newRange.setType(typeOfHub);
        try {
            this.insertObject(typeOfHub, range, pair);
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(MercuryImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
        private void replicateObject(String typeOfHub,DataRange range, ArrayList<KeyValuePair> pairs){
        typeOfHub=typeOfHub+"_REPLICA";
        DataRange newRange = range;
        newRange.setType(typeOfHub);
        try {
            this.insertObject(typeOfHub, range, pairs);
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(MercuryImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    @Override
    public List<Serializable> getObject(String typeOfHub, DataRange range) throws CommunicationException {
        //このノードがHubを持つメインノードか調べる
        if(this.hubContainer.isMainNetwork()){
            //該当するタイプのハブが存在しているか調べる
            if(this.hubContainer.hasHub(typeOfHub)){
                //探している範囲がこのノードの担当範囲か調べる
                int indexOfHub = this.hubContainer.isRange(range);
                if(indexOfHub==0){
                    //このノードが担当範囲であった場合
                    return this.hubContainer.getEntries(range);
                    
                }else if(indexOfHub==-1){
                    try {
                        //完全にこのノードの担当範囲内であった場合        
                        return this.hubContainer.getHub(typeOfHub).getDirectSuccessor().getEntry(typeOfHub, range);
                    } catch (CommunicationException ex) {
                        this.logger.debug("Failing get object :typeOfHub:"+typeOfHub+"");
                        this.logger.debug("Starting get backup");
                        String backUpName = typeOfHub+"_REPLICA";
                        range.setType(backUpName);
                        return this.getObject(backUpName, range);
                    }
                }else if(indexOfHub==1){
                    //このノードの担当範囲内＆範囲以上
                    List<Serializable> result=null;
                    List<Serializable> formerResult=null;
                    result=this.hubContainer.getEntries(range);
                    formerResult = this.hubContainer.getHub(typeOfHub).getDirectSuccessor().getEntry(typeOfHub, range);
                    
                    if(result!=null&&formerResult!=null){
                        for(int i=0;i<formerResult.size();i++){
                            result.add(formerResult.get(i));
                        }
                    }
                    
                    return result;
                }
                else if(indexOfHub==2){
                    //このノードの担当範囲内＆範囲以下
                    List<Serializable> result=null;
                    List<Serializable> formerResult=null;
                        if(result!=null&&formerResult!=null){
                        for(int i=0;i<formerResult.size();i++){
                            result.add(formerResult.get(i));
                        }
                    }
                    return result;
                }
                
                    
            }else{
                try {
                    //このノードに該当するタイプのHUｂが存在しない場合
                     return this.hubContainer.getHub(typeOfHub).getDirectSuccessor().getEntry(typeOfHub, range);
                } catch (CommunicationException ex) {
                    java.util.logging.Logger.getLogger(MercuryImpl.class.getName()).log(Level.SEVERE, null, ex);
                        this.logger.debug("Failing get object :typeOfHub:"+typeOfHub+"");
                        this.logger.debug("Starting get backup");
                        String backUpName = typeOfHub+"_REPLICA";
                        return this.getObject(backUpName, range);
                }
            }
        }else{
            //このノードがHubを持たないサブノードの場合
           Node parentNode = this.hubContainer.getAccessLinkRandomly();
            try {
                return parentNode.getEntry(typeOfHub, range);
            } catch (CommunicationException ex) {
                        this.logger.debug("Failing get object :typeOfHub:"+typeOfHub+" ");
                        this.logger.debug("Starting get backup");
                        String backUpName = typeOfHub+"_REPLICA";
                        return this.getObject(backUpName, range);
            }
            
        }
        //ここに来るのはおかしい。
        return null;
    }

    @Override
    public void createHubNetwork(int TTL, Hub hubToSplit, ArrayList<Address> addresess) throws CommunicationException {
        this.localNode.createHubNetwork(TTL, hubToSplit, addresess);
    }


    private static class MercuryThreadFactory implements ThreadFactory{

        private String executorName;

        public MercuryThreadFactory(String executorName) {
            this.executorName = executorName;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread newThread = new Thread(r);
            newThread.setName(this.executorName+"-"+newThread.getName());
            return newThread;
        }
        
    }
    

    
    public MercuryImpl(){
        this.logger = Logger.getLogger(MercuryImpl.class.getName()+".unidentified");
        this.logger.debug("Logger initialized.");
        
        this.maintenanceTasks = new ScheduledThreadPoolExecutor(3, new MercuryThreadFactory("MaintenanceTaskExecution"));
        this.asynExecutor=Executors.newFixedThreadPool(MercuryImpl.ASYNC_CALL_THREADS,new MercuryThreadFactory("AsynchronousExecution"));
        ///ここいるかどうか確認（HubContainerの初期化位置）
        this.hubContainer = new HubContainer(2);
        logger.info("MercuryImpl initialized!");
    }
    
    final Executor getAsyncExecutor(){
        if(this.asynExecutor==null){
            throw new NullPointerException("MercuryImpl.asynExecutor is null!");
        }
        return this.asynExecutor;
    }
    
    
    
    @Override
    public Address getAddress() {
        return this.localAddress;
    }

    @Override
    public void setAddress(Address nodeAddress) throws IllegalArgumentException {
        if(nodeAddress==null){
            NullPointerException e = new NullPointerException("Cant set Address to null");
            this.logger.error("Nullpointer",e);
            throw e;
        }
        if(this.localNode!=null){
            IllegalStateException e = new IllegalStateException("Addres cant be set after creation");
            this.logger.error("Illegal state",e);
            throw e;
        }
        this.localAddress=nodeAddress;
    }



    
    @Override
    public void create() throws ServiceException {
        if(this.localNode!=null){
            throw new ServiceException("Cant create network; node is already connected");
        }
        if(this.localAddress==null){
            throw new ServiceException("Node Address is not set yet");
        }
        this.createHelp();
    }

    
    
    @Override
    public void create(Address localAddress) throws ServiceException {
        if(localAddress==null){ 
            throw new NullPointerException("parameter must not be null");
        }
        if(this.localNode!=null){
            throw new ServiceException("Cant create network, node is already exist");
        }
        
        this.localAddress=localAddress;
        
        this.hubContainer.setAddress(localAddress);
        this.createHelp();
    }
    
    private final void createHelp(){
        
        this.logger.debug("Help method for createing a new Mercury ring invoked");
        //this.entries = new Entries();
        
        this.hubContainer = new HubContainer(2);
        this.localNode= new NodeImpl(this,this.localAddress,this.hubContainer);
        this.createTasks();
        this.localNode.acceptEntries();
    }
    

    private final void createTasks(){
//        this.maintenanceTasks.scheduleWithFixedDelay(new StabilizeTask(this.localNode, this.references, this.entries), MercuryImpl.STABILIZE_TASK_START, MercuryImpl.STABILIZE_TASK_INTERVAL, TimeUnit.SECONDS);
//        this.maintenanceTasks.scheduleWithFixedDelay(new FixLDT(this.localNode,this.references), MercuryImpl.FIX_LONG_TABLE_START, MercuryImpl.FIX_LONG_TABLE_INTERVAL, TimeUnit.SECONDS);
//        this.maintenanceTasks.scheduleWithFixedDelay(new CheckPredecessorTask(this.references), MercuryImpl.CHECK_PREDECESSOR_TASK_START, MercuryImpl.CHECK_PREDECESSOR_TASK_INTERVAL,TimeUnit.SECONDS);
        this.maintenanceTasks.scheduleWithFixedDelay(new MaintenanceLayer(null, localNode, hubContainer), MercuryImpl.STABILIZE_TASK_START, MercuryImpl.STABILIZE_TASK_INTERVAL, TimeUnit.SECONDS);
    }
    
    @Override
    public void join(Address bootstrapAddress) throws ServiceException {
        if(bootstrapAddress==null){
            throw new NullPointerException("at least one parameter is null, which is not permitted");
        }
        if(this.localNode!=null){
            throw new ServiceException("Cant join network, node is alerady connected");
        }
        if(this.localAddress==null){
            throw new ServiceException("Node address is not set yet, Please set address with help of setAddresss");
        }

        this.joinHelp(bootstrapAddress);
    }

    @Override
    public void join(Address localAddress, Address bootstrapAddress) throws ServiceException {
        if(localAddress==null||bootstrapAddress==null){
            throw new NullPointerException("at least one parameter is null, which is not permitted");
        }
        if(this.localNode!=null){
            throw new ServiceException("Cant join network, node is alerady connected");
        }
        
        this.localAddress=localAddress;
        this.hubContainer.setAddress(localAddress);
        
        this.joinHelp(bootstrapAddress);
    }

    
    private final void joinHelp (Address bootstrapAddress)throws ServiceException{
        this.localNode = new NodeImpl(this, this.getAddress(), hubContainer =new HubContainer(2));
        
        Node bootstrapNode;
        try {
            bootstrapNode = Proxy.createConnection(this.localAddress, bootstrapAddress);
        } catch (CommunicationException ex) {
           throw new ServiceException("An error occouerd when creating a proxy for outgoing connection to bootstrap node!"
                   + "Join Operation Failed!", ex);
        }

       // this.references.addSuccessor(bootstrapNode);
        
        Node myPredecessor = null;
        try {
            bootstrapNode.joinRequest(localAddress);
            this.hubContainer.setAccessLink(bootstrapNode);
            
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(MercuryImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
//        if(this.references.containsSuccessor(this.localNode)||this.references.){
//            
//        }
        
          //  this.references.addPredecessor(myPredecessor);

        
        this.localNode.acceptEntries();
        this.createTasks();
        }
    
    
  
    public Node joinMercury(Address targetAddresss) throws CommunicationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    

    public Hub acceptMercuryLink() throws CommunicationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
        
        
    @Override
    public void leave() throws ServiceException {
        if(this.localNode==null){
            return;
        }
        
        this.maintenanceTasks.shutdown();
//        try{
//            Node successor = this.references.getSuccessor();
//            if(successor!=null&&this.references.getPredecessors()!=null){
//                successor.leavesNetwork(this.references.getPredecessors());
//            }
//        }catch(CommunicationException e){
//            
//        }
        
        this.localNode.disconnect();
        this.asynExecutor.shutdownNow();
        this.localNode=null;
    }
    
        Node findSuccessor(Address key) {
       if(key == null){
           NullPointerException e = new NullPointerException("ID to find successor may not be null");
           this.logger.error("null pointer", e);
           throw e;
       }
       boolean debug = this.logger.isEnabledFor(Logger.LogLevel.DEBUG);
      // Node successor = this.references.getSuccessor();
       
//       if(successor==null){
//           if(this.logger.isEnabledFor(Logger.LogLevel.INFO)){
//               this.logger.info("this node is only one node in the network ,so return reference on own node");
//           }
//           return this.localNode;
//       }else{
//           return successor;
//       }
       Node node = null;
       return node;
       
    }
        /**
         * AddressのノードをSuccessorとして設定した後、Addressのノードが持つPredecessorを
         * 返り値とする。このメソッドはリクエストを受ける側が実行するため、
         * Addressは自分自身のアドレスとなる。
         * 
         * @param address　Predecessorを取得するノードのアドレス
         * @return predecessor of address
         */
    @Override
     public void joinRequest(Address fromAddress) {
           if(fromAddress==null){
               NullPointerException e = new NullPointerException("Address to get as successor may not be null!");
               this.logger.error("nullpointer",e);
               throw e;
           }
           boolean debug = this.logger.isEnabledFor(Logger.LogLevel.DEBUG);
          
           
           Node predecessor=null;
        try {
            predecessor = SocketProxy.create(localAddress, fromAddress);
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(MercuryImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
           if(predecessor==null){
               if(this.logger.isEnabledFor(Logger.LogLevel.INFO)){
                   this.logger.info("Target node:"+fromAddress.toString()+": has no Predecessor! perhaps something seriously wrong");
               }
           }
           
           this.hubContainer.setAccessLink(predecessor);
    }
    
    
    public Node joinMercuryRequest(){
        try {
            return this.localNode.joinMercuryRequest(this.localNode);
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(MercuryImpl.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    
}
        