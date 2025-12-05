/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.data;

import java.io.Serializable;

/**
 *
 * @author kousuke
 */
public class KeyValuePair implements Serializable {
    
    private Serializable value;
    private Object key;

    public KeyValuePair(Object key,Serializable value) {
        this.value = value;
        this.key = key;
    }
    
    public Serializable getValue(){
        return this.value;
    }
    
    public Object getKey(){
        return this.key;
    }
    
    public void setValue(Serializable value){
        this.value=value;
    }
    
    public void setKey(Object key){
        this.key=key;
    }
    
    
}
