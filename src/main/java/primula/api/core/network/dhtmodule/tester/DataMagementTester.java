/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.tester;

import java.io.Serializable;
import java.util.LinkedList;

import primula.api.core.network.dhtmodule.data.DataRange;
import primula.api.core.network.dhtmodule.data.HubContainer;
import primula.api.core.network.dhtmodule.data.KeyValuePair;
import primula.api.core.network.dhtmodule.data.hubimpl.IntegerHub;
import primula.api.core.network.dhtmodule.data.hubimpl.IntegerRange;
import primula.api.core.network.dhtmodule.tester.datasample.TestEntry;

/**
 *
 * @author kousuke
 */
public class DataMagementTester {
    
    public static void main(String args[]){
        String type ="INTEGER";
        int listIndex;
        TestEntry hoge,gero,roge;
        hoge = new TestEntry(12, "hoge", 12.22);
        gero = new TestEntry(1, "おえええええええええ", 15.22);
        roge = new TestEntry(8, "ひぎいいいいいいいい", 15);
        
        HubContainer container=new HubContainer(2);
        
        IntegerHub intHub = new IntegerHub(type, 5, 1,100);
        IntegerRange intRange = new IntegerRange("INTEGER",0, 300,300);
        intHub.setRange(intRange);
        container.addHub(intHub);
        
        intRange = new IntegerRange("INTEGER",hoge.getIntegerKey(), hoge.getIntegerKey(),2000);
        listIndex=container.isRange(intRange);
        if(listIndex!=-1){
            KeyValuePair pair = new KeyValuePair(hoge.getIntegerKey(), hoge);
            System.err.println("Inserting Entry Name:"+hoge.getName()+"  Double:"+hoge.doubleValue()+"  IntegerValue:"+hoge.getIntegerKey());
            container.setEntry(type, listIndex, pair);
        }
        
        System.err.println("direct successor shutdown confirmed!");
        
        intRange = new IntegerRange("INTEGER",gero.getIntegerKey(), gero.getIntegerKey(),2000);
        listIndex=container.isRange(intRange);
        if(listIndex!=-1){
            KeyValuePair pair = new KeyValuePair(gero.getIntegerKey(), gero);
            container.setEntry(type, listIndex, pair);
        }
        
        DataRange devidingRange = intHub.divideRange(2);
        IntegerHub newHub = new IntegerHub(type,5 , 1,100);
        newHub.setRange(devidingRange);
        container.addHub(newHub);
        
                
        
        DataRange devidingRange2 = intHub.divideRange(2);
        IntegerHub newHub2 = new IntegerHub(type,5 ,1, 100);
        newHub2.setRange(devidingRange2);
        container.addHub(newHub2);
        
        
        intRange = new IntegerRange("INTEGER",12, 12,300);
         LinkedList<Serializable>result=(LinkedList<Serializable>)container.getEntries(intRange);
        
         TestEntry resultTest = (TestEntry)result.get(0);
         
         System.err.println("GET SUCCESSFUL:"+resultTest.getName()+" :DoubleValue "+resultTest.doubleValue()
                 +" IntegerValue: "+resultTest.getIntegerKey());
         
        System.out.print(container.toString());

       
        
    }
}
