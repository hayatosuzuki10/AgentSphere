/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction.impl;

//import com.sun.org.apache.xalan.internal.lib.NodeInfo;
import static primula.api.core.network.dhtmodule.utill.Logger.LogLevel.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.CommunicationException;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.DataRange;
import primula.api.core.network.dhtmodule.data.Hub;
import primula.api.core.network.dhtmodule.data.KeyValuePair;
import primula.api.core.network.dhtmodule.nodefunction.Endpoint;
import primula.api.core.network.dhtmodule.nodefunction.EndpointStateListener;
import primula.api.core.network.dhtmodule.nodefunction.Node;
import primula.api.core.network.dhtmodule.utill.Logger;


/**
 *
 * @author VENDETTA
 */
final class RequestHandler extends Thread implements EndpointStateListener{

    private static Logger logger = Logger.getLogger(RequestHandler.class);

    private Node node;

    private Socket connection;

    private ObjectOutputStream os;

    private ObjectInputStream is;

    boolean connected = true;

    private int state;

    private SocketEndpoint endpoint;

    private Set<Thread> waitingThreads = new HashSet<Thread>();

    public RequestHandler(Node node,Socket connection , SocketEndpoint endpoint) throws IOException{
        super("RequestHandler_"+endpoint.getAddresss());

        if(RequestHandler.logger.isEnabledFor(INFO)){
            RequestHandler.logger.info("Initialising RequestHandler. Socket:"+connection+", Endpoint :"+endpoint);
        }

        this.node=node;
        this.connection=connection;
        this.os= new ObjectOutputStream(this.connection.getOutputStream());
        try{
        	this.is= new ObjectInputStream(this.connection.getInputStream());
        }catch(IOException e){
            os.close();
            throw e;
        }

        try{
            Request req = (Request) this.is.readObject();
            if(req.getRequestType()!=MethodConstants.CONNECT){
                Response res = new Response(Response.REQUEST_FAILED, req.getRequestType(), req.getReplyWith());
                try{
                    os.writeObject(res);
                }catch(IOException e){ }
                try{
                    os.close();
                }catch(IOException e){}
                try{
                    is.close();
                }catch(IOException e){}
                throw new IOException("Unexpected Message receieved!");

            }else{
                Response res = new Response(Response.REQUEST_SUCCSESSFUL, req.getRequestType(), req.getReplyWith());
                os.writeObject(res);
            }
        }catch(ClassNotFoundException e){
            throw new IOException("Unexpected class type recieved!"+e.getMessage());
        }

        this.endpoint=endpoint;
        this.state=this.endpoint.getState();
        this.endpoint.register(this);
        logger.info("RequestHandler initialized!");
   }

    SocketEndpoint getEndpoint(){
        return this.endpoint;
    }

    @Override
    public void run(){

        while(this.connected){
            Request request = null;
            try{
                logger.debug("Waiting for request....");
                request=(Request) this.is.readObject();
                if(request.getRequestType()==MethodConstants.SHUTDOWN){
                    logger.debug("Recieved shutdown request");
                    this.disconnect();
                }else{
                    logger.debug("Recieved request" +request);
                    new InvocationThread(this,request,this.os);
                }
            }catch(IOException e){
                logger.debug("Exeption occured while recieving a request. Maybe socket has been closed.");
                this.disconnect();
            }catch(ClassNotFoundException e){
                logger.error("Exception occured while recieving a request.",e);
                this.disconnect();
            }catch(Throwable t){
                logger.fatal("Unexpected throwable has been throwed while receiving message!",t);
                this.disconnect();
            }
        }
    }

    void sendFailureResponse(Throwable t,String failureReason,Request request){
        if(!this.connected){
            return;
        }
        logger.debug("Trying to send failure response.Failure reason"+failureReason);
        Response failureResponse = new Response(Response.REQUEST_FAILED, request.getRequestType(), request.getReplyWith());
        failureResponse.setFailureReason(failureReason);
        failureResponse.setThrowable(t);

        try{
            synchronized(this.os){
                this.os.writeObject(failureResponse);
                this.os.flush();
                this.os.reset();
            }
            logger.debug("Response send");
        }catch(IOException e){
            if(this.connected){
                logger.debug("Connection seems to be broken down. Could not send failure Response. Connection is closed");
                this.disconnect();
            }
        }
    }

    Serializable invokeMethod(int methodType,Serializable[] parameters)throws Exception{
        String method = MethodConstants.getMethodName(methodType);
        this.waitForMethod(method);

        if(!this.connected){
            throw new CommunicationException("Connection closed.");
        }
        Serializable result = null;
        logger.debug("Trying to invoke method "+methodType+" with parametes:");
        for(Serializable parameter:parameters){
            logger.debug(parameter);
        }

        switch(methodType){
            case MethodConstants.FIND_SUCCESSOR:{
                Node mercuryNode = this.node.findSuccessor((Address)parameters[0]);
                result = new RemoteNodeInfo(mercuryNode.getNodeAddress());
                break;
            }
            case MethodConstants.JOIN_REQUEST:{
                this.node.joinRequest((Address)parameters[0]);

                break;
            }
            case MethodConstants.LEAVES_NETWORK:{
                RemoteNodeInfo nodeInfo = (RemoteNodeInfo)parameters[0];
                this.node.leavesNetwork(SocketProxy.create(nodeInfo.getNodeAddress(), this.node.getNodeAddress()));
                break;
            }
            case MethodConstants.NOTIFY:{
                RemoteNodeInfo nodeInfo = (RemoteNodeInfo)parameters[0];
                List<Node> list = this.node.notifyToPredecessor(SocketProxy.create(nodeInfo.getNodeAddress(), this.node.getNodeAddress()));
                List<RemoteNodeInfo> nodeInfos = new LinkedList<RemoteNodeInfo>();
                for(Node current:list){
                    nodeInfos.add(new RemoteNodeInfo(current.getNodeAddress()));
                }
                result=(Serializable) nodeInfos;
                break;
            }
            case MethodConstants.INSERT_ENTRY:{
                String typeOfHub = (String) parameters[0];
                DataRange range = (DataRange)parameters[1];
                KeyValuePair pair = (KeyValuePair)parameters[2];

                this.node.insertEntry(typeOfHub,range,pair);
                break;
            }
            case MethodConstants.GET_ENTRY:{
                String typeOfHub =(String )parameters[0];
                DataRange range = (DataRange) parameters[1];
                result = (Serializable) this.node.getEntry(typeOfHub, range);
                break;
            }
            case MethodConstants.JOIN_MERCURY_REQUEST:{
               result = this.node.joinMercuryRequest((Node)parameters[0]);
                break;
            }
            case MethodConstants.SEND_HUB:{
                Address fromAddress = (Address) parameters[0];
                Hub hub =(Hub) parameters[1];
                this.node.acceptHub(fromAddress,hub);
                break;
            }
            case MethodConstants.CREATE_HUB_NETWORK:{
                int TTL = (Integer)parameters[0];
                Hub hubToSplit = (Hub)parameters[1];
                ArrayList addresses =(ArrayList)parameters[2];
                this.node.createHubNetwork(TTL, hubToSplit, addresses);
                break;
            }
            case MethodConstants.INSERT_MULTI_ENTRY:{
                String typeOfHub = (String) parameters[0];
                DataRange range = (DataRange)parameters[1];
                ArrayList<KeyValuePair> pair = (ArrayList<KeyValuePair>)parameters[2];

                this.node.insertEntry(typeOfHub,range,pair);
                break;
            }
            default :{
                logger.warn("Unknown method requested "+method);
                throw new Exception("Unknown method requested "+method);
            }
        }
        logger.debug("Returning result");
        return result;
    }

    private void waitForMethod(String method){
        logger.debug(method+" allowed?"+!(Collections.binarySearch(Endpoint.METHODS_ALLOWED_IN_ACCEPT_ENTRIES, method)>=0));

        synchronized(this.waitingThreads){
            while((!(this.state==Endpoint.ACCEPT_ENTRIES))
                    &&(this.connected)&&
                    ((Collections.binarySearch(Endpoint.METHODS_ALLOWED_IN_ACCEPT_ENTRIES, method)>=0))){
                Thread currentThread = Thread.currentThread();
                boolean debug = logger.isEnabledFor(DEBUG);
                if(debug){
                    logger.debug(currentThread+" waiting for permission to execute "+method);
                }
                this.waitingThreads.add(currentThread);
                try{
                    this.waitingThreads.wait();
                }catch(InterruptedException e){}

                if(debug){
                    logger.debug(currentThread+" has been notified");
                }
                this.waitingThreads.remove(currentThread);
            }
        }
        logger.debug("waitForMethod("+method+") returns!");
    }

    public void disconnect(){
        logger.info("Disconnecting.");

        if(this.connected){
            synchronized(this.waitingThreads){
                this.connected = false;
                this.waitingThreads.notifyAll();
            }
            this.node=null;
            try{
                synchronized(this.os){
                    this.os.close();
                    this.os = null;
                    }
                }catch(IOException e){
                    logger.debug("Exception while closing output stream "+this.os);
                }
            try{
                this.is.close();
                this.is = null;
            }catch(IOException e){
                logger.debug("Exception while closing input stream "+this.is);
            }
            try{
                logger.info("Closing socket "+this.connection);
                this.connection.close();
                this.connection = null;
                logger.info("Socket Closed");
            }catch(IOException e){
                logger.debug("Exeption while closing socket "+this.connection  );
            }
            this.endpoint.deregister(this);
        }
        logger.debug("Disconnected.");
    }

    public boolean isConnected(){
        return this.connected;
    }

    @Override
    public void notify(int newState) {
        logger.debug("notify("+newState+") called");
        this.state=newState;

        synchronized(this.waitingThreads){
            logger.debug("Notifying waiting threads."+this.waitingThreads);
            this.waitingThreads.notifyAll();
        }
    }

}
