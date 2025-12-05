/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.message;

import primula.util.KeyValuePair;

/**
 *
 * @author kurosaki
 */
public class SendContentContainer extends AbstractContentContainer{
    private KeyValuePair<String, String> content;
    
    public SendContentContainer(KeyValuePair<String, String> content){
        this.content = content;
    }
    
    public KeyValuePair<String, String> getContent(){
        return content;
    }
}
