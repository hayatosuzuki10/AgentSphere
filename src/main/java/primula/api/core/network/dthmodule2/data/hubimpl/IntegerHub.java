/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dthmodule2.data.hubimpl;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import primula.api.core.network.dthmodule2.data.DataRange;
import primula.api.core.network.dthmodule2.data.Hub;
import primula.util.KeyValuePair;
/**
 *テスト用のIntegerをKeyとするHub
 * @author VENDETTA
 */
public class IntegerHub extends Hub<Integer> {

    private static final long serialVersionUID = -3174403459101781115L;

    private int lowerBound =0;

    private int upperBound = 200;

    private static final int DEVIDE_SCORE=2;


    public IntegerHub(String type,int numberOfSuccessorAndPredecessor) {
        super(type,numberOfSuccessorAndPredecessor);
        this.listOfEntriesIndex  = new LinkedList<Integer>();
        this.listOfEntriesValue = new LinkedList<Serializable>();
        this.range = new IntegerRange(type,lowerBound, upperBound,upperBound);
        //logger = Logger.getLogger(IntegerHub.class);
    }


    public void setRange(DataRange<Integer> range) {
        this.range = range;
        this.lowerBound = range.getLowerBound();
        this.upperBound = range.getUpperBound();
    }

    @Override
    public DataRange<Integer> divideRange(int DEVIDE_SCORE) {
      return  this.range.divideRange(DEVIDE_SCORE);
    }

    public Serializable getEntry(Integer key) {
        return listOfEntriesValue.get(key);
    }

    public List<Serializable> getEntry(DataRange<Integer> key) {
        IntegerRange searchRange;
        if(key instanceof IntegerRange){
            searchRange = (IntegerRange)key;
        }else{
            IllegalArgumentException e = new IllegalArgumentException("key is not IntegerRange Type!");
            //this.logger.fatal("Illegal Argument",e);
            throw e;
        }
         LinkedList<Serializable> result = new LinkedList<Serializable>();
         int keysLowerBound;
         int keysUpperBound;

             keysLowerBound=key.getLowerBound();

             keysUpperBound=key.getUpperBound();

         int low = 0,up = 0;
         System.out.println(this.listOfEntriesIndex + " *****************");/////////////////////////////////////
         for(int i=0; i<this.listOfEntriesIndex.size(); i++) {
        	 if(keysLowerBound <= this.listOfEntriesIndex.get(i)) {
        		 low = i;
        		 break;
        	 }
         }
         for(int i=this.listOfEntriesIndex.size()-1; i>=0; i--) {
        	 if(keysUpperBound >= this.listOfEntriesIndex.get(i)) {
        		 up = i;
        		 break;
        	 }
         }
//         low = this.listOfEntriesIndex.indexOf(keysLowerBound);
//         up = this.listOfEntriesIndex.indexOf(keysUpperBound);

         for(int i=low;i<=up;i++){
             //result.add(this.listOfEntriesValue.get(i));コウスケさんの書いてたやつ
        	 result.add(this.listOfEntriesIndex.get(i));
         }

         return result;
    }


	@Override
	public void setEntry(KeyValuePair keys) {
		// TODO 自動生成されたメソッド・スタブ
		this.listOfEntriesIndex.add((Integer)keys.getKey());
        Collections.sort(this.listOfEntriesIndex);
        int keyIndex = this.listOfEntriesIndex.indexOf((Integer)keys.getKey());
        //this.listOfEntriesValue.add(keyIndex, keys.getValue());
	}


	@Override
	public void setEntry(KeyValuePair[] keyValuePairs) {
		// TODO 自動生成されたメソッド・スタブ
		int key,keyIndex;
        for(int i = 0;i<keyValuePairs.length;i++){
       key = (Integer)keyValuePairs[i].getKey();
       this.listOfEntriesIndex.add(key);
       Collections.sort(this.listOfEntriesIndex);
       keyIndex = this.listOfEntriesIndex.indexOf(key);
       //this.listOfEntriesValue.add(keyIndex, keyValuePairs[i].getValue());
        }
	}




}
