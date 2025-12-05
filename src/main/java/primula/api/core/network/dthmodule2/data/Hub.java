/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dthmodule2.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import primula.util.KeyValuePair;

/**
 * Hubに使われるエントリの抽象クラス
 * Hubを実装する際にはデータを格納するためのList等のデータフィールドを用意する事
 * なお、Mercuryの特性上比較可能データである事が望ましい。
 * Entry挿入の際には、このクラスを保持しているクラスがisRangeメソッドを使用して確認してから挿入する
 *
 * @author VENDETTA
 */
/**
 * TODOOOO
 * 現在このクラスのためにPredecessorList,SuccessorListクラスのメソッドが全部publicにしたので
 * あとで直せたら直す
 */
public abstract class Hub<T> implements Serializable{

   protected DataRange<T> range;

    private String type;

   protected LinkedList<T> listOfEntriesIndex ;

   protected LinkedList<Serializable> listOfEntriesValue;

   protected Logger logger;

   //private LongDistanceTable LDT;

   private InetAddress successor;

   private InetAddress predecessor;

    public Hub(String type,int numberOfSuccessorAndPredecessor){
        this.type=type;
        //successors=new SuccessorList(numberOfSuccessorAndPredecessor);
        //predecessors= new PredecessorList(numberOfSuccessorAndPredecessor);
        //LDT = new LongDistanceTable();
    }

    /*
    public void setNodeToLDT(Node node){
        this.LDT.addNode(node);
    }

    public Node getNodeFromLDT(){
        return this.LDT.getNodeRamdomly();
    }
     */


    public String getType(){
        return this.type;
    }

    public void setType(String type){
        this.type=type;
    }

    public InetAddress getDirectSuccessor(){
        return this.successor;
    }

    public InetAddress getDirectPredecessor(){
        return this.predecessor;
    }

    public void  setDirectSuccessor(InetAddress successor){
        this.successor=successor;
    }

    public void setDirectPredecessor(InetAddress predecessor){
        this.predecessor=predecessor;
    }


    /*
    public final int getSuccessorListSize(){
        return this.successors.size();
    }

    public final int getPredecessorListSize(){
        return this.predecessors.size();
    }



    public void addSuccessor(Node successor){
        this.successors.addSuccessor(successor);
    }
    public final void addSuccessor(Address addressOfLocalNode,Address targetAddress){
        this.successors.addSuccessor(addressOfLocalNode, targetAddress);
    }

    public void addPredecessor(Node predecessor){
        this.predecessors.addPredecessor(predecessor);
    }

    public void addPredecessor(Address destinationAddress,Address fromAddress){
        this.predecessors.addPredecessor(destinationAddress, fromAddress);
    }
    */

    public DataRange getRange(){
        return this.range;
    }

//    public abstract boolean isRange(T keyToCheck);

    public abstract void setRange(DataRange<T> range);

    public abstract DataRange<T> divideRange(int divideScore);

    public abstract void setEntry(KeyValuePair keyValuePair);

    public abstract void setEntry(KeyValuePair[] keyValuePairs );

    public abstract Serializable getEntry(T key);

    public abstract List<Serializable> getEntry(DataRange<T> key);

    @Override
    public final String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE OF HUB ="+type+"\n");
        builder.append("RANGE OF HUB ="+range.toString()+"\n");
        builder.append("Successor->"+this.getDirectSuccessor().toString()+"\n");
        builder.append("Predecessor->"+this.getDirectPredecessor().toString()+"\n");
        return builder.toString();
    }
}
