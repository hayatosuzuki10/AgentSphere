/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dthmodule2.data.hubimpl;

import primula.api.core.network.dthmodule2.data.DataRange;


/**
 *
 * @author VENDETTA
 */
public class IntegerRange extends DataRange<Integer> {

    public IntegerRange(String type,Integer lower,Integer upper,int max){
        super(type, lower, upper, max);
    }



    /**
     *
     * @param value
     * @return 完全に範囲内の場合＝０、完全に範囲外の場合=-1。範囲内＆範囲以上＝１。範囲内＆範囲以下≒２,範囲内だがmax,min共にはみ出てる場合＝３,error =-2
     *
     */
    @Override
    public int isInRange(DataRange rangep) {

        IntegerRange range = (IntegerRange)  rangep;

        if((this.upperBound>=range.getUpperBound())&&(range.getLowerBound()>=this.lowerBound)){
            return 0;
        }
        else if((this.upperBound<range.getUpperBound())&&(range.getLowerBound()>this.lowerBound)){
            return -1;
        }
        else if((this.upperBound>range.getUpperBound())&&(range.getLowerBound()<this.lowerBound)){
            return 1;
        }else if((this.upperBound<range.getUpperBound())&&(range.getLowerBound()<this.lowerBound)){
            return 2;
        }
        return -2;
    }

    //?????
    public int isInRange(int value){
    	if((this.upperBound>=value)&&(value>=this.lowerBound)){
            return 0;
        }
        else{
        	return -1;
        }
    }

    @Override
    public DataRange<Integer> divideRange(int DEVIDE_SCORE) {
        int newBound,oldBound,remainder;
        oldBound = this.upperBound;
        newBound=(upperBound-lowerBound)/DEVIDE_SCORE;
        remainder=(upperBound-lowerBound)%DEVIDE_SCORE;
        if(remainder!=0){
            newBound+=remainder;
        }
        this.upperBound=newBound;
        return new IntegerRange(this.getType(),newBound, oldBound,this.maxAbsoluteValue);
    }

    @Override
    public int caluculateDifference() {
       return this.upperBound-this.lowerBound;
    }


}
