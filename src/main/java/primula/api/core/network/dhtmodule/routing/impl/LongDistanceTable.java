/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import primula.api.core.network.dhtmodule.nodefunction.Node;
import primula.api.core.network.dhtmodule.utill.Logger;

/**
 *
 * @author VENDETTA
 */
public class LongDistanceTable implements Serializable{
    
    
    private final ArrayList<Node> remoteNodes;
    
    int maxLink=20;
    
    private Logger logger ;

    public LongDistanceTable() {
        this.logger = Logger.getLogger(LongDistanceTable.class);
        this.logger.debug("Logger initialized");

        this.remoteNodes=new ArrayList();
    }
    
    public void addNode(Node node){
        if(maxLink>this.remoteNodes.size()){
            this.remoteNodes.add(node);
        }else{
            this.remoteNodes.add(node);
            this.remoteNodes.remove(0);
        }
    }
   
    
    
    public Node getNodeRamdomly(){
        Random random = new Random();
        int randomNum = random.nextInt(remoteNodes.size());
        return this.remoteNodes.get(randomNum);
    }

}
