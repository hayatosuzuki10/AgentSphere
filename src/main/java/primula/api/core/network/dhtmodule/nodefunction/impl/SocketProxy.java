/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction.impl;

//import com.sun.org.apache.xalan.internal.lib.NodeInfo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.DataRange;
import primula.api.core.network.dhtmodule.data.Hub;
import primula.api.core.network.dhtmodule.data.KeyValuePair;
import primula.api.core.network.dhtmodule.data.histogramData;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;
import primula.api.core.network.dhtmodule.nodefunction.Endpoint;
import primula.api.core.network.dhtmodule.nodefunction.Node;
import primula.api.core.network.dhtmodule.nodefunction.Proxy;
import primula.api.core.network.dhtmodule.utill.Logger;

/**
 *
 * @author VENDETTA
 */
public final class SocketProxy extends Proxy implements Runnable {

    private final static Logger logger = Logger.getLogger(SocketProxy.class);

    private static Map<String,SocketProxy> proxies = new HashMap<String, SocketProxy>();

    private Address addressOfLocalNode =null;

    private Address destinationAddress = null;

    private long requestCounter= -1;

    private transient Socket mySocket;

    private transient ObjectOutputStream os;

    private transient ObjectInputStream is;

    private transient Map<String,Response> responses;

    private transient Map<String,WaitingThread> waitingThreads;

    private volatile boolean disconnected = false;




    public static SocketProxy create(Address addressOfLocalNode,Address address)throws CommunicationException{
        synchronized(proxies){

            String proxyKey = SocketProxy.createProxyKey(addressOfLocalNode, address);
            logger.debug("Known proxies "+SocketProxy.proxies.keySet());
            if(proxies.containsKey(proxyKey)){
            logger.debug("Returning existing proxy for "+address);
            return proxies.get(proxyKey);
        }else{
                logger.debug("Creating new proxy for "+address);
                SocketProxy newProxy = new SocketProxy(address, addressOfLocalNode);
                proxies.put(proxyKey, newProxy);
                return newProxy;
            }
        }
    }

    public static SocketProxy getProxy(Address targetAddress,Address fromAddress) {
        synchronized(proxies){
            String proxyKey = SocketProxy.createProxyKey(fromAddress, targetAddress);
            if(proxies.containsKey(proxyKey)){
                return proxies.get(proxyKey);
            }else{
               RuntimeException e = new RuntimeException("Proxy has not found! something seriously wrong!");
               logger.error("Proxy has not found!", e);
               throw e;
            }

        }
    }


    public  SocketProxy(Address address,Address addressOfLocalNode) throws CommunicationException{
        super(address);
        this.destinationAddress=address;
        if(address==null||addressOfLocalNode==null){
            throw new IllegalArgumentException("Address must not be null");
        }
        this.addressOfLocalNode =addressOfLocalNode;

    }

    private synchronized void send(Request request) throws CommunicationException{
        try{
            logger.debug("SendingRequest"+request.getReplyWith());
            this.os.writeObject(request);
            this.os.flush();
            this.os.reset();
        }catch(IOException e){
            throw new CommunicationException("Could not connect to node "+this.nodeAddress, e);
        }

    }

    private synchronized  String createIdentifier(int methodIdentifier){
        StringBuilder uid = new StringBuilder();
        uid.append(System.currentTimeMillis());
        uid.append("-");
        uid.append(this.requestCounter);
        uid.append("-");
        uid.append(methodIdentifier);
        return uid.toString();
    }

    private Response waitForRensponse(Request request)throws CommunicationException{
        String responseIdentifier = request.getReplyWith();
        Response response = null;
        logger.debug("Trying to wait for response with identifier "+
                responseIdentifier+" for method "+MethodConstants.getMethodName(request.getRequestType()));

        synchronized(this.responses){
            logger.debug("Number of responses "+this.responses.size());
            if(this.disconnected){
                throw new CommunicationException("Connection  to remote host is broken down");
            }

            response=this.responses.remove(responseIdentifier);
            if(response!=null){
                return response;
            }

            WaitingThread wt = new WaitingThread(Thread.currentThread() );
            this.waitingThreads.put(responseIdentifier, wt);

            while(!wt.hasBeenWokenUp()){
                try{
                    logger.debug("waiting for response.");
                    this.responses.wait();
                }catch(InterruptedException e){

                }
            }
            logger.debug("Have been woken up form waiting for response.");
            this.waitingThreads.remove(responseIdentifier);
            response=this.responses.remove(responseIdentifier);
            logger.debug("Response for request with identifier "+responseIdentifier+
                    "for method "+MethodConstants.getMethodName(request.getRequestType())+" recieved.");

            if(response==null){
                logger.debug("No response received.");
                if(this.disconnected){
                    logger.info("Connection to remote host lost.");
                    throw new CommunicationException("Connection to remote host is broken down");
                }else{
                    logger.equals("There is no result, but we have not  been disconnected. Something went seriously wrong!/");
                    throw new CommunicationException("Did not receive a response");
                }
            }
        }

        return response;
    }


    private void responseReceived(Response response){
        synchronized(this.responses){
            logger.debug("No of waiting threads "+ this.waitingThreads);
            WaitingThread waitingThread = this.waitingThreads.get(response.getInReplyTo());
            logger.debug("Response with id "+response.getInReplyTo()+"recieved.");
            this.responses.put(response.getInReplyTo(), response);

            if(waitingThread!=null){
                logger.debug("Waiking up Thread!");
                waitingThread.wakeUp();
            }
        }
    }

    private void connectionBrokenDown(){
        if(this.responses==null){
            return;
        }
        synchronized(this.responses){
            logger.info("Connection broken down!");
            this.disconnected =true;

            for(WaitingThread thread : this.waitingThreads.values()){
                logger.debug("Interrupting waiting thread"+thread);
                thread.wakeUp();
            }
        }

    }

    private Request createRequest(int methodIdentifier,Serializable[] parameters){
        if(logger.isEnabledFor(Logger.LogLevel.DEBUG)){
            logger.debug("Creating request for method "+
                    MethodConstants.getMethodName(methodIdentifier)
                    +"with parameters "+java.util.Arrays.deepToString(parameters));
        }
        String responseIdentifier = this.createIdentifier(methodIdentifier);
        Request request = new Request(methodIdentifier,responseIdentifier);
        request.setParameters(parameters);
        logger.debug("Request "+request+" created.");
        return request;
    }





    private void makeSocketAvailable() throws CommunicationException{
        if(this.disconnected){
            throw new CommunicationException("Connection form "+this.addressOfLocalNode+" to remote host "+this.nodeAddress
                    +" is broken down.");
        }
        logger.debug("makeSocketAvailable() called. Testing for Socket availavility" );

        if(this.responses==null){
            this.responses = new HashMap<String, Response>();
        }
        if(this.waitingThreads==null){
            this.waitingThreads = new HashMap<String, WaitingThread>();
        }
        if(this.mySocket==null){
            try{
                logger.info("Opening new Socket to "+this.nodeAddress);
                this.mySocket = new Socket(this.nodeAddress.getHost(),this.nodeAddress.getPort());
                logger.debug("Socket created:"+this.mySocket);
                this.mySocket.setSoTimeout(5000);
                this.os= new ObjectOutputStream(this.mySocket.getOutputStream());
                this.os.flush();
                this.is = new ObjectInputStream(this.mySocket.getInputStream());
                logger.debug("Sending connection request!");
                os.writeObject(new Request(MethodConstants.CONNECT,"Initial Connection"));

                try{
                    Response res = null;
                    boolean timedOut = false;
                    try{
                        logger.debug("Waiting for connection response.");
                        res = (Response )is.readObject();
                    }catch(SocketTimeoutException e){
                        logger.info("Connection timed out!");
                        timedOut =true;
                    }
                    this.mySocket.setSoTimeout(0);
                    if(timedOut){
                        throw new CommunicationException("Connection to remote host timed out!");
                    }

                    if(res!=null&&res.getStatus()== Response.REQUEST_SUCCSESSFUL){
                        Thread t = new Thread(this,"SocketProxy_Thread_"+this.nodeAddress);
                        t.start();
                    }else{
                        throw new CommunicationException("Establising connection failed.");
                    }
                }catch(UnknownHostException e){
                    throw new CommunicationException("Unknown host:"+this.nodeAddress.getHost());
                }
                catch(ClassNotFoundException e){
                    throw new CommunicationException("Unexpected result Recieved!");
                }
            }catch(IOException e){
                throw new CommunicationException("Could not set up IO channel to host "+this.nodeAddress.getHost(), e);
            }
        }
        logger.debug("makeSocketAvailable() finished. Socket "+this.mySocket);
    }

    protected void finalize() throws Throwable{
        logger.debug("Finalization running");
    }

    @Override
    public void disconnect(){
      logger.info("Destroying connection form "+this.addressOfLocalNode+" to "+this.nodeAddress);

      synchronized(proxies){
          String proxyKey = SocketProxy.createProxyKey(this.addressOfLocalNode, this.nodeAddress);
          Object o = proxies.remove(proxyKey);
      }
      this.disconnected = true;

      try{

          if(this.os!=null){
              try{
                  logger.debug("Socket shutdown notification to endpoint");
                  Request request = this.createRequest(MethodConstants.SHUTDOWN, new Serializable[0]);
                  logger.debug("Notification send");

                  this.os.writeObject(request);
                  this.os.close();
                  this.os=null;
                  logger.debug("OutputStream "+this.os+" closed.");
              }catch(IOException e){
                  logger.debug(this+": Exception during closing of outputStream "+this.os,e);
              }
          }

          if(this.is!=null){
              try{
                  this.is.close();
                  logger.debug("InputStream "+this.is+" closed.");
                  this.is=null;
              }catch(IOException e){
                  logger.debug("Exception during closing of input stream"+this.is);
              }
          }

          if(this.mySocket!=null){
              try{
                  logger.info("Closing socket "+this.mySocket+".");
                  this.mySocket.close();
              }catch(IOException e){
                  logger.debug("Exception during closing of socket"+this.mySocket);
              }
              this.mySocket=null;
          }
      }catch(Throwable t){
          logger.warn("Unexpected exception during disconnection of SocketProxy",t);
      }
      this.connectionBrokenDown();
    }

    private static String createProxyKey(Address localAddress,Address remoteAddress){
        return localAddress.toString()+"->"+remoteAddress.toString();
    }

    @Override
    public void run() {
        while(!this.disconnected){
            try{
                Response response =(Response) this.is.readObject();
                logger.debug("Response "+response+" recieved!");
                this.responseReceived(response);
            }catch(ClassNotFoundException e){
                logger.fatal("ClassNotFoundException has occured during deserialization "
                        + "of response. Threre is something seriously worng here!",e);
            }catch(IOException e){
                if(!this.disconnected){
                    logger.warn("Could not read response from stream!",e);
                }else{
                    logger.debug(this+": Connection has been closed");
                }
                this.connectionBrokenDown();
            }
        }
    }



    @Override
    public void ping() throws CommunicationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void joinRequest(Address fromAddress) throws CommunicationException {
        this.makeSocketAvailable();

        logger.debug("Trying to get predecessor which address:"+fromAddress.getHost());

        Request request = this.createRequest(MethodConstants.JOIN_REQUEST, new Serializable[]{fromAddress});

        logger.debug("Trying to send request "+request);
        this.send(request);

        logger.debug("Waiting for response for request "+request);
        Response response = this.waitForRensponse(request);
        logger.debug("Response " +response+" has arrived!");

        if(response.isFailureResponse()){
            throw new CommunicationException(response.getFailureReason());
        }
//        else{
//            try{
//            Hub newHub = (Hub) response.getResult();
//            }catch(ClassCastException e){
//                String message = "Could not understand messaqge";
//                logger.fatal(message);
//                throw new CommunicationException(message, e);
//            }
//        }
    }



    @Override
    public Node joinMercuryRequest(Node fromNode) throws CommunicationException {
        this.makeSocketAvailable();
        logger.debug("Trying to join mercury");
       Request request = this.createRequest(MethodConstants.JOIN_MERCURY_REQUEST, new Serializable[]{fromNode});

       try{
           this.send(request);
       }catch(CommunicationException e){
           logger.debug("Connection failed!");
           throw e;
       }

       logger.debug("Waiting for respose for request");
       Response response = this.waitForRensponse(request);
       logger.debug("Response"+response+" has arrived!");

       if(response.isFailureResponse()){
           throw new CommunicationException(response.getFailureReason());
       }else{
           return (Node)response.getResult();
       }
    }


    @Override
    public histogramData[] getHistogramData(int TTL, String typeOfHub) throws CommunicationException {
       this.makeSocketAvailable();
       logger.debug("Trying to get HistogramData");
       Request request = this.createRequest(MethodConstants.GET_HISTOGRAM_DATA,new Serializable[]{TTL,typeOfHub} );
       try{
           this.send(request);
       }catch(CommunicationException e){
           logger.debug("Connection Failed");
           throw e;
       }

       logger.debug("Waiting response for request");
       Response response = this.waitForRensponse(request);
       logger.debug("Response "+response+" fas arrived!");

       if(response.isFailureResponse()){
           throw new CommunicationException(response.getFailureReason());
       }else{
        return (histogramData[])response.getResult();
    }
    }

    @Override
    public void sendHub(Address toAddress,Hub hubToSend) throws CommunicationException {
        this.makeSocketAvailable();
        Request request =this.createRequest(MethodConstants.SEND_HUB, new Serializable[]{toAddress,hubToSend});

        logger.debug("Trying to get Hubs");
        try{
            this.send(request);
        }catch(CommunicationException e ){
            logger.debug("Connection failed");
            throw e;
        }

        logger.debug("Waiting response for request");
        Response response = this.waitForRensponse(request);
        logger.debug("Response "+response+" has arrived!");

        if(response.isFailureResponse()){
            throw new CommunicationException(response.getFailureReason());
        }else{

        }
    }

        @Override
    public void createHubNetwork(int TTL, Hub hubToSplit, ArrayList<Address> addresses) throws CommunicationException {
               this.makeSocketAvailable();
               logger.debug("Trying to create HubNetwork");

               Request request = this.createRequest(MethodConstants.CREATE_HUB_NETWORK, new Serializable[]{TTL,hubToSplit,addresses});

               try{
                   this.send(request);
               }catch(CommunicationException e){
                   logger.debug("ConnectionFailed!");
                   throw e;
               }

               logger.debug("Waiting response for Request");
               Response response = this.waitForRensponse(request);

               if(response.isFailureResponse()){
                   throw new CommunicationException(response.getFailureReason());
               }else{

               }
    }


        @Override
    public void insertEntry  (String typeOfHub, DataRange range, KeyValuePair pair) throws CommunicationException {
       this.makeSocketAvailable();

       logger.debug("Trying to insert Entry "+pair+".");

       Request request = this.createRequest(MethodConstants.INSERT_ENTRY, new Serializable[]{typeOfHub,range,pair});

       try{
           logger.debug("Trying to send request "+request);
           this.send(request);
       }catch(CommunicationException ce){
           logger.debug("Connection failed!");
           throw ce;
       }

       logger.debug("Waiting for response for request "+request);
       Response response = this.waitForRensponse(request);
       logger.debug("Response "+response+" has arrived!");

       if(response.isFailureResponse()){
           throw new CommunicationException(response.getFailureReason());
       }else{
           return;
       }

      }

                @Override
    public void insertEntry  (String typeOfHub, DataRange range, ArrayList<KeyValuePair> pair) throws CommunicationException {
       this.makeSocketAvailable();

       logger.debug("Trying to insert Entry "+pair+".");

       Request request = this.createRequest(MethodConstants.INSERT_MULTI_ENTRY, new Serializable[]{typeOfHub,range,pair});

       try{
           logger.debug("Trying to send request "+request);
           this.send(request);
       }catch(CommunicationException ce){
           logger.debug("Connection failed!");
           throw ce;
       }

       logger.debug("Waiting for response for request "+request);
       Response response = this.waitForRensponse(request);
       logger.debug("Response "+response+" has arrived!");

       if(response.isFailureResponse()){
           throw new CommunicationException(response.getFailureReason());
       }else{
           return;
       }

      }



        @Override
    public List<Serializable> getEntry(String typeOfHub, DataRange range) throws CommunicationException {
      this.makeSocketAvailable();

      logger.debug("Trying to get Entry "+range);
      Request request = this.createRequest(MethodConstants.GET_ENTRY, new Serializable[]{typeOfHub,range});
      try{
         logger.debug("Trying to send request " +request);
          this.send(request);
      }catch(CommunicationException e){
          logger.debug("Connection failed!");
          throw e;
      }

      logger.debug("Waiting for response for request "+request);
      Response response = this.waitForRensponse(request);
      logger.debug("Response "+response+ " has arrived!");

      if(response.isFailureResponse()){
          throw new CommunicationException(response.getFailureReason());
      }else{
          return (List<Serializable>)response.getResult();
      }
    }


    @Override
    public Node findSuccessor(Address address) throws CommunicationException {
          this.makeSocketAvailable();

          logger.debug("Trying to find successor for Address "+ address);

          Request request = this.createRequest(MethodConstants.FIND_SUCCESSOR, new Serializable[]{address});

          logger.debug("Trying to send request "+ request);
          this.send(request);

          logger.debug("Waiting for response for request "+request);
          Response response =this.waitForRensponse(request);
          logger.debug("Response "+response+" has arrived");

          if(response.isFailureResponse()){
              throw new CommunicationException(response.getFailureReason());
          }else{
              try{
                  RemoteNodeInfo nodeInfo = (RemoteNodeInfo)response.getResult();
                  if(nodeInfo.getNodeAddress().equals(this.addressOfLocalNode)){
                      return Endpoint.getEndpoint(this.addressOfLocalNode).getNode();
                  }else{
                      return create(nodeInfo.getNodeAddress(),this.addressOfLocalNode);
                  }
              }catch(ClassCastException e){
                  String message = "Could not understand result!"+response.getResult();
                  logger.fatal(message);
                  throw new CommunicationException(message, e);
              }

          }
    }

    @Override
    public List<Node> notifyToPredecessor(Node potentialPredecessor) throws CommunicationException{
        this.makeSocketAvailable();

        RemoteNodeInfo nodeInfoToSend = new RemoteNodeInfo(potentialPredecessor.getNodeAddress());
        Request request = this.createRequest(MethodConstants.NOTIFY, new Serializable[]{nodeInfoToSend});

        try{
            this.send(request);
        }catch(CommunicationException e){
            throw e;
        }

        Response response = this.waitForRensponse(request);
        if(response.isFailureResponse()){
            throw new CommunicationException(response.getFailureReason());
        }else{
            try{
                List<RemoteNodeInfo> references = (List<RemoteNodeInfo>) response.getResult();
                List<Node> nodes = new LinkedList<Node>();

                for(RemoteNodeInfo nodeInfo:references){
                    if(nodeInfo.getNodeAddress().equals(this.addressOfLocalNode)){
                        nodes.add(Endpoint.getEndpoint(this.addressOfLocalNode).getNode());
                    }else{
                        nodes.add(create(nodeInfo.getNodeAddress(), this.addressOfLocalNode));
                    }
                }
                return nodes;

            }catch(ClassCastException e){
                throw new CommunicationException("Could not understand result! "+ response.getResult(),e);
            }
        }
    }

    @Override
    public void leavesNetwork(Node predecessor) throws CommunicationException {
        this.makeSocketAvailable();
        logger.debug("Trying to insert notify node that "+predecessor
                +"leaves network");

        RemoteNodeInfo nodeInfo = new RemoteNodeInfo(predecessor.getNodeAddress());
        Request request = this.createRequest(MethodConstants.LEAVES_NETWORK, new Serializable[]{nodeInfo});

        try{
            logger.debug("Trying to send request "+request);
           this.send(request);
        }catch(CommunicationException e){
            logger.debug("Connection failed!");
            throw e;
        }
        logger.debug("WAiting forresponse for request "+request);
        Response response = this.waitForRensponse(request);
        logger.debug("Response " +response+" arrived.");
        if(response.isFailureResponse()){
            throw new CommunicationException(response.getFailureReason());
        }else{
           return ;
        }
    }

    static void shutDownAll(){
        Set<String> keys=proxies.keySet();
        for(String key:keys){
            proxies.get(key).disconnect();
        }

        proxies.clear();
    }





    private String stringRepresentation = null;

    public String toString(){
        if(this.mySocket==null){
            return "Unconnected SocketProxy form "+ this.addressOfLocalNode+"to "+this.nodeAddress;
        }
        if(this.stringRepresentation==null){
            StringBuilder builder = new StringBuilder();
            builder.append("Connection form Node[Address= ");
            builder.append(this.addressOfLocalNode);
            builder.append(", socket=");
            builder.append(this.mySocket);
            builder.append("]");
            builder.append(", Address= ");
            builder.append("]");
            this.stringRepresentation=builder.toString();
        }

        return  stringRepresentation;
    }

    @Override
    public void acceptHub(Address address,Hub hubToAccept) {
        throw new RuntimeException("SocketProxy:acceptHub--- method should not invoke! Something seriously wrong!");
    }









    private static class WaitingThread{
        private boolean hasBeenWokenUp = false;

        private Thread thread;

        private WaitingThread(Thread thread){
            this.thread=thread;
        }

        boolean hasBeenWokenUp(){
            return this.hasBeenWokenUp;
        }

        void wakeUp(){
            this.hasBeenWokenUp=true;
            this.thread.interrupt();
        }

        public String toString(){
            return this.thread.toString()+": Waiting?"+!this.hasBeenWokenUp();
        }
    }
}
