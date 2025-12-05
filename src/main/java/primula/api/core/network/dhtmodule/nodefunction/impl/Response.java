/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction.impl;

import java.io.Serializable;

import primula.api.core.network.dhtmodule.nodefunction.Message;

/**
 *
 * @author VENDETTA
 */
public class Response extends Message{
    private static final long serialVersionUID = -4349351320367352691L;

    public static final int REQUEST_SUCCSESSFUL = 1;
    
    public static final int REQUEST_FAILED = 0;
    
    private String failureReason;
    
    private Serializable result;
    
    private int methodIdentifier = -1;
    
    private int status = REQUEST_SUCCSESSFUL;
    
    private String inReplyTo;
    
    private Throwable throwable = null;
    
    Response(int status,int methodIdentifier, String inReplyTo){
      super();
      this.status=status;
      this.methodIdentifier=methodIdentifier;
      this.inReplyTo=inReplyTo;
      
    }
    
    int getMethodIdentifier(){
        return this.methodIdentifier;
    }
    
    int getStatus(){
        return this.status;
    }
    
    boolean isFailureResponse(){
        return (this.status==REQUEST_FAILED);
    }
    
    Throwable getThrowable(){
        return this.throwable;
    }
    
    void setThrowable(Throwable t){
        this.throwable = t;
    }
    
    void setFailureReason(String reason){
        this.status=REQUEST_FAILED;
        this.failureReason=reason;
    }
    
    String getFailureReason(){
        return this.failureReason;
    }
    
    void setResult(Serializable result){
        this.result=result;
    }
    Serializable getResult(){
        return this.result;
    }
  
    String getInReplyTo(){
        return this.inReplyTo;
    }
    
}
