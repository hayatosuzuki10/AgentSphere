/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.monitor;

import java.io.Serializable;

/**
 *
 * @author onda
 */
public class ImageMachineInfo implements Serializable{
    private StringBuffer string;
    private int countAgent;
    ImageMachineInfo(StringBuffer string, int countAgent){
        this.string=string;
        this.countAgent=countAgent;
    }    

    public StringBuffer getInfo(){
       return string;
    }

    public int getCountAgent(){
        return countAgent;
    }


}
