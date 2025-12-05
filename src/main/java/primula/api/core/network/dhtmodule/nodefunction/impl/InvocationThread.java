/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction.impl;

import static primula.api.core.network.dhtmodule.utill.Logger.LogLevel.DEBUG;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import primula.api.core.network.dhtmodule.utill.Logger;

/**
 *
 * @author VENDETTA
 */
class InvocationThread implements Runnable{

    protected static final String CORE_POOL_SIZE_PROPERTY_NAME=InvocationThread.class.getName()+".corepoolsize";
    
    protected static final String MAX_POOL_SIZE_PROPERTY_NAME=InvocationThread.class.getName()+".maxpoolsize";
    
    protected static final String KEEP_ALIVE_TIME_PROPERTY_NAME= InvocationThread.class.getName()+".keepalivetime";
    
    private static final int CORE_POOL_SIZE = Integer.parseInt(System.getProperty(MAX_POOL_SIZE_PROPERTY_NAME));
    
    private static final int MAX_POOL_SIZE = Integer.parseInt(System.getProperty(MAX_POOL_SIZE_PROPERTY_NAME));
    
    private static final int KEEP_ALIVE_TIME = Integer.parseInt(System.getProperty(KEEP_ALIVE_TIME_PROPERTY_NAME));
    
    private static final Logger logger = Logger.getLogger(InvocationThread.class);
    
    private static final boolean debug = logger.isEnabledFor(DEBUG);
    
    private Request request;
    
    private RequestHandler handler;
    
    private ObjectOutputStream os;
    
    public InvocationThread(RequestHandler aThis, Request request, ObjectOutputStream os) {
        this.handler=aThis;
        this.request=request;
        this.os=os;
        
        this.handler.getEndpoint().scheduleInvocation(this);
        if(debug){
            logger.debug("InvocationThread scheduled for request "+request);
        }
    }

    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("[Invocation of ");
        sb.append(MethodConstants.getMethodName(this.request.getRequestType()));
        sb.append("]Request:");
        sb.append(this.request);
        return sb.toString();
    }
    
    @Override
    public void run() {
       if(debug){
           logger.debug(this+" started");
       }
       int requestType = this.request.getRequestType();
       String methodName = MethodConstants.getMethodName(requestType);
       if(debug){
           logger.debug("Request recieved. Requested method:" +methodName);
       }
       
       try{
           if(debug){
               logger.debug("trying to invoke method "+methodName);
           }
           Serializable result = this.handler.invokeMethod(requestType,this.request.getParameters());
           
           Response response = new Response(Response.REQUEST_SUCCSESSFUL, requestType, this.request.getReplyWith());
           response.setResult(result);
           
           synchronized(this.os){
               this.os.writeObject(response);
               this.os.flush();
               this.os.reset();
           }
           logger.debug("Method invoked and result has been sent.");
       }catch(IOException e){
           if(this.handler.connected){
               logger.warn("could not send response. Disconecting!",e);
               this.handler.disconnect();
           }
       }catch(Exception t){
           if(debug){
               logger.debug("Throwable occured during execution of request "+
                       MethodConstants.getMethodName(requestType)+"!");
           }
           this.handler.sendFailureResponse(t,"Could not execute Request !"
                   + "Reason unkown! maybe this helps;"+t.getMessage(),this.request);
       }
       this.handler=null;
       this.os=null;
       if(debug){
           logger.debug(this+"finished");
       }
    }
    
    static ThreadPoolExecutor createInvocationThreadPool(){
        return new ThreadPoolExecutor(CORE_POOL_SIZE,MAX_POOL_SIZE,KEEP_ALIVE_TIME,TimeUnit.SECONDS
                ,new LinkedBlockingQueue<Runnable>(),new ThreadFactory() {
                    
                    private static final String name = "InvocationExecution-";
            @Override
            public Thread newThread(Runnable r) {
                Thread newThread = new Thread(r);
                newThread.setName(name+r.toString());
                return newThread;
            }
        });
    }
    
}
