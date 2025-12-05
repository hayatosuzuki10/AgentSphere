/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import primula.api.core.network.dhtmodule.address.Address;

/**
 *ヒストグラム作成の為のアンケートクラス
 * 現行は論文と同じくデータの担当範囲を基準としている。
 * @author VENDETTA
 */
public final class histogramData implements Serializable {
    private static final long serialVersionUID = 3574993599629580434L;
    
    //自分の位置の想定されるノード数
    int n;
    
    int TTL;
    
    DataRange range;
    
    int absoluteValueOfRange;
    
    Address addressOfNode;
    
    ArrayList<histogramData> histogramDatas;

    public  histogramData(int TTL) {
        this.TTL=TTL;
    }

    public final void setHistogramData(DataRange range, int absoluteValue,Address address){
        this.range=range;
        this.addressOfNode=address;
        this.absoluteValueOfRange=absoluteValue;
        this.histogramDatas = new ArrayList<histogramData>();

    }
    
    public final DataRange getRange(){
        return this.range;
    }
    
    public final int getAbsoluteValueOfRange(){
        return this.absoluteValueOfRange;
    }
    
    public final Address getAddress(){
        return this.addressOfNode;
    }
    
    public final void caluculateAssumptionOfNodes(histogramData[] successorDatas,histogramData[] predecessorDatas){
        int numberOfData =successorDatas.length;
        int AllRangeWithAbsoluteValue = successorDatas[0].getAbsoluteValueOfRange();
        int sumOfData=0;
        
        for(histogramData data:successorDatas){
        sumOfData+=data.getRange().caluculateDifference();
        }
        for(histogramData data:predecessorDatas){
        sumOfData+=data.getRange().caluculateDifference();    
        }
        int n =(numberOfData*2*AllRangeWithAbsoluteValue)/sumOfData;
        this.n=n;
    }
    
    public final ArrayList<Address> getLDTLink(histogramData[] successorDatas,histogramData[] predecessorDatas){

        
        
        Random random = new Random();
        
        double linkPoint = Math.exp(Math.log(n)*(random.nextDouble()-1));
        
        return null;
    }
}
