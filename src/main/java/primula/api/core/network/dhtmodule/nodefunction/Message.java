/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction;

import java.io.Serializable;

/**
 *
 * @author VENDETTA
 */
public abstract class Message implements Serializable {

    private final long timeStamp;

    protected Message(){
        this.timeStamp = System.currentTimeMillis();
    }
    
    public final long getTimeStamp(){
        return this.timeStamp;
    }
    
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("[Message@");
        builder.append(this.hashCode());
        builder.append(" from time ");
        builder.append(this.timeStamp);
        builder.append("]");
        return builder.toString();
    }
}
