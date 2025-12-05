/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.tester.datasample;

import java.io.Serializable;

/**
 *　データ保存、取得用のテストサンプル
 * @author VENDETTA
 */
public class TestEntry implements Serializable{
    
   private int key;
    
   private String name;
    
   private double doubleValue;
    
    public TestEntry(int key,String name,double doubleValue){
        this.key=key;
        this.name=name;
        this.doubleValue=doubleValue;
    }
    
    public int getIntegerKey(){
        return this.key;
    }
    
    public String getName(){
        return this.name;
    }
    
    public double doubleValue(){
        return this.doubleValue;
    }
    
}
