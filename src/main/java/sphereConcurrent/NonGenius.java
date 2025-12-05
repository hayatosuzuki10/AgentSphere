/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sphereConcurrent;

import java.net.InetAddress;
import java.util.LinkedList;

import primula.api.NetworkAPI;
import primula.util.KeyValuePair;

/**
 *
 * @author akita
 */
public class NonGenius implements Geniusable{
    LinkedList<String> machineList;
    final LinkedList<SphereTask> taskList;//sizeを見るだけにすること
    final LinkedList<String> executors;
    final int MIN = 1;
    int nextMachine;
    public NonGenius(LinkedList<SphereTask> taskList, LinkedList<String> executors){
        this.machineList = new LinkedList<String>();
        //this.machineList.add(SystemAPI.getAgentSphereId());//無いと単一実行時にエラー。どうにか改善したい
        for(KeyValuePair<String, KeyValuePair<InetAddress, Integer>> pair : NetworkAPI.getAddresses()){
            machineList.add(pair.getKey());
        }
        /*
        System.err.println("NonGenius mList size : " + this.machineList.size());
        System.err.println("Nongenius mList : " + this.machineList);
        */
        this.nextMachine = 0;
        this.taskList = taskList;
        this.executors = executors;
    }
    @Override
    public int pollTasksSize() {
    	int x = this.taskList.size()/(this.executors.size()*2);
    	int size = (x > MIN) ? x : MIN;
    	//System.err.println(size);
        return size ;
    }

    @Override
    public String execMachine() {
        String ans = this.machineList.get(nextMachine++);
        if(nextMachine == this.machineList.size())nextMachine = 0;
        return ans;
    }

}
