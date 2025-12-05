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
public class Request extends Message {
    private static final long serialVersionUID = -3679550910833751993L;
    
    private int type;
    
    private String replyWith;
    
    private Serializable[] parameters=null;
    
    
    
    protected Request(int type,String replyWith){
        super();
        this.type=type;
        this.replyWith=replyWith;
    }
    
    int getRequestType(){
        return this.type;
    }
    
    void setParameters(Serializable[] parameters){
        this.parameters=parameters;
    }
    
    Serializable[] getParameters(){
        return this.parameters;
    }
    
    String getReplyWith(){
        return this.replyWith;
    }
    
    public String toString(){
        return super.toString();
    }
}
