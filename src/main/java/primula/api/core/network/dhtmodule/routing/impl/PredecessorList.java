/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing.impl;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.logging.Level;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;
import primula.api.core.network.dhtmodule.nodefunction.Node;
import primula.api.core.network.dhtmodule.nodefunction.impl.SocketProxy;
import primula.api.core.network.dhtmodule.utill.Logger;

/**
 *
 * @author kousuke
 */
public final class PredecessorList implements Serializable{

    
    private  LinkedList<Node> predecessorList =null;
    
    private Node directPredecessor=null;
    
    private int capacity;
     
     private Logger logger;
     
     //private Reference reference;

    public PredecessorList( int capacity) {
       this.logger=Logger.getLogger(PredecessorList.class);
       this.logger.debug("Logger.initialized");
       
        this.capacity = capacity;
        predecessorList = new LinkedList<Node>();
    }
    
    public void initializePredecessor(Node localNode){
        this.predecessorList.add(localNode);
    }
     
   public synchronized final void addPredecessor(Node predecessor){
        if(predecessor==null){
            NullPointerException e = new NullPointerException("Parameter which To add as predecessor is null, this isnt permitted!");
            this.logger.error("Nullpointer", e);
            throw e;
        }

            predecessorList.push(predecessor);
        
    }
   
   public void setDirectPredecessor(Node predecessor){
       this.directPredecessor=predecessor;
       this.addPredecessor(predecessor);
   }
    
   public synchronized  final void addPredecessor(Address destinationAddress,Address fromAddress){
        if(destinationAddress==null||fromAddress==null){
            NullPointerException e = new NullPointerException("Parameter which To Add as predecessor is null,this isnt permitted!");
            this.logger.error("Nullpointer",e);
            throw e;
        }
        SocketProxy predecessor=null;
        try {
            predecessor = new SocketProxy(destinationAddress, fromAddress);
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(PredecessorList.class.getName()).log(Level.SEVERE, null, ex);
        }
        predecessorList.push(predecessor);
    }
    
  public  final boolean containsPredecessor(Node predecessor){
        if(predecessor==null){
           NullPointerException e = new NullPointerException("Predecessor to delete must not be null!");
           this.logger.error("Null pointer", e);
           throw e;
       }
        boolean isContains=false;
        isContains=this.predecessorList.contains(predecessor);
        return isContains;
    }
    
  public  synchronized final void removePredecessor(Node predecessorToDelete){
       if(predecessorToDelete==null){
           NullPointerException e = new NullPointerException("Predecessor to delete must not be null!");
           this.logger.error("Null pointer", e);
           throw e;
       }
       boolean removeSuccessful = true;
       removeSuccessful = predecessorList.remove(predecessorToDelete);
       if(removeSuccessful==false){
           this.logger.error("There is no reference on predecessor list :"+predecessorToDelete.toString());
       }
    }
    
  public  final Node getDirectPredecessor(){
       return predecessorList.get(0);
    }
  
    
    public String toString(){
        
        StringBuilder builder = new StringBuilder();
        
        for(Node predecessor : predecessorList){
            builder.append("Predecessor;").append(predecessor==null ? "null":predecessor.getNodeAddress()+"\n");
        }
        
        return builder.toString();
    }
    
    public int size(){
        return this.predecessorList.size();
    }
    
}
