/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.monitor;

import primula.api.core.network.message.AbstractContentContainer;

/**
 *
 * @author owner
 */
public class OtherDispContent extends AbstractContentContainer{
    private StringBuffer string;
    private int size;

    OtherDispContent(StringBuffer string, int size){
        this.string = string;
        this.size = size;
    }

    public StringBuffer getString(){
        return string;
    }

    public int getAgentSize(){
        return size;
    }

}
