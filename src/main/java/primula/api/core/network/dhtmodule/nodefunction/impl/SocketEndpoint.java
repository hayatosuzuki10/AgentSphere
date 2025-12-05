/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction.impl;


import static primula.api.core.network.dhtmodule.utill.Logger.LogLevel.DEBUG;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.nodefunction.Endpoint;
import primula.api.core.network.dhtmodule.nodefunction.Node;
import primula.api.core.network.dhtmodule.utill.Logger;

/**
 *
 * @author VENDETTA
 */
public final class SocketEndpoint extends Endpoint implements Runnable{

    private final static Logger logger = Logger.getLogger(SocketEndpoint.class);
    
    private final static boolean debug = logger.isEnabledFor(DEBUG);
    
    private Set<RequestHandler> handlers = new HashSet<RequestHandler>();
    
    private ServerSocket mySocket = null;
    
    private final ThreadPoolExecutor invocationExecutor  = InvocationThread.createInvocationThreadPool();
    
    public SocketEndpoint(Node node,Address address){
        super(node, address);
        SocketEndpoint.logger.info("Initialization finished");
    }
    
    @Override
    protected void openConnections() {
        try{
            if(debug){
                SocketEndpoint.logger.debug("Trying to open server socket on port "+this.address.getPort());
            }
            this.mySocket = new ServerSocket(this.address.getPort());
            this.setState(LISTENING);
            if(debug){
                SocketEndpoint.logger.debug("ServerSocket opened on port:"+this.address.getPort()+ ". Starting listener thread");
            }
            
            Thread listenerThread = new Thread(this,"SocketEndpoint_"+this.address+"_Thread");
            listenerThread.start();
            
            if(debug){
                SocketEndpoint.logger.debug("ListenerThread "+listenerThread+ " started.");
            }
        }catch(IOException e){
            throw new RuntimeException("SocketEndpoint cold not listen on port "+this.address.getPort()+" " +e.getMessage());
        }
    }

    @Override
    protected void entriesAcceptable() {
       if(debug){
           SocketEndpoint.logger.debug("entriesAcceptable() called" );
       }
       this.setState(ACCEPT_ENTRIES);
    }

    @Override
    protected void closeConnections() {
       this.setState(STARTED);
       try{
           this.mySocket.close();
       }catch(IOException e){
           if(debug){
               SocketEndpoint.logger.debug("could not close socket  "+this.mySocket,e);
           }
       }
       this.invocationExecutor.shutdownNow();
       
       SocketProxy.shutDownAll();
    }

    @Override
    public void run() {
        while(this.getState()>STARTED){
            if(debug){
                SocketEndpoint.logger.debug("Waiting for incoming connection");
            }
            Socket incomingConnection = null;
            
            try{
                incomingConnection =this.mySocket.accept();
                if(debug){
                    SocketEndpoint.logger.debug("Incoming connection "+incomingConnection );
                    SocketEndpoint.logger.debug("Creating request handler for incoming connection");
                }
                
                RequestHandler handler = new RequestHandler(this.node, incomingConnection, this);
                this.handlers.add(handler);
                if(debug){
                    SocketEndpoint.logger.debug("RequestHandler created. Starting Thread .");
                }
                handler.start();
                if(debug){
                    SocketEndpoint.logger.debug("RequestHandler thread started");
                }
            }catch(IOException e){
                if((this.getState())>STARTED){
                    if(debug){
                        SocketEndpoint.logger.debug("could not accept connection form other node!",e);
                    }
                    if(incomingConnection!=null){
                        try{
                            incomingConnection.close();
                        }catch(IOException ioe){}
                        incomingConnection= null;
                        }
                    }
                    else{
                        //ソケットクローズ完了済
                    }
                }
            }
            SocketEndpoint.logger.info("ListenerThread stopped");
            for(RequestHandler handler:this.handlers){
                handler.disconnect();
            }
            this.handlers.clear();
        }
    
    void scheduleInvocation(InvocationThread invocationThread){
        if(debug){
            logger.debug("Scheduling invocation:"+invocationThread);
        }
        this.invocationExecutor.execute(invocationThread);
        if(debug){
            logger.debug("Current jobs:"+this.invocationExecutor.getQueue().size());
            logger.debug("Active jobs:"+this.invocationExecutor.getActiveCount());
            logger.debug("Completed jobs :"+this.invocationExecutor.getCompletedTaskCount());
        }
    }
    
    }
 
