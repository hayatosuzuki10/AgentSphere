/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dthmodule2.data;

import java.io.Serializable;

/**
 *　Hubにおける担当範囲を保持し、その担当範囲のデータを格納するクラス、
 * 　Hubに対して担当範囲をセットする際にはこのクラスを利用する。
 * 　データはCompalableを実装する事。
 * @author VENDETTA
 */
public abstract class DataRange<T> implements Serializable {

    //protected Logger logger ;

    private String type;

    protected T lowerBound;

    protected int maxAbsoluteValue;

    protected int minAbsoluteValue;

    protected T upperBound;


    public DataRange(String type,T lower,T upper,int max){
        if(type==null||lower==null||upper==null){
            throw new NullPointerException("LowerBound or UpperBound is null,Which is not permitted!");
        }
        //logger = Logger.getLogger(this.getClass().getName());
        this.type = type;
        this.lowerBound=lower;
        this.upperBound=upper;
        this.maxAbsoluteValue=max;
    }

    public int getMaxValue(){
        return this.maxAbsoluteValue;
    }

    public int getMinValue(){
        return this.minAbsoluteValue;
    }

    public String getType(){
        return this.type;
    }

    public void setType(String type){
        this.type=type;
    }

    public void setUpper(T value){
        this.upperBound=value;
    }

    public void setLower(T value){
        this.lowerBound=value;
    }

    public T getUpperBound(){
        return this.upperBound;
    }

    public T getLowerBound(){
        return this.lowerBound;
    }

    public void discardRange(){

    }

    public void setRange(DataRange<T> newRange){
        this.lowerBound=newRange.lowerBound;
    }

   public abstract int isInRange(DataRange range);

   //public abstract int isInRange(Object object);

   /**
    * Rangeを分割する。どの割合で分割するかは実装に任せる。
    * この関数を使って分割されたRangeは値の低い方を元のノードで保持し、値の高いほうを分担先のノードに渡す。
    * @return
    */
   public abstract DataRange<T> divideRange(int divideScore);

   public abstract int caluculateDifference();

    @Override
   public final String toString(){
       StringBuilder builder = new StringBuilder();
       builder.append("Areas of responsivility ::");
        builder.append("upper bound is ").append(this.upperBound);
        builder.append(": lower bound is ").append(this.lowerBound);

      return  builder.toString();
   }

}
