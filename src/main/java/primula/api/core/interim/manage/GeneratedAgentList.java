/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.manage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.SystemAPI;
import primula.api.core.agent.AgentInstanceInfo;
import primula.util.KeyValuePair;

/**
 *
 * @author kurosaki
 */
public class GeneratedAgentList implements Serializable{
    private static ArrayList<KeyValuePair<String, String>> list = new ArrayList<KeyValuePair<String, String>>();
    private static GeneratedAgentList agentPool;
   
    public static synchronized GeneratedAgentList getInstance(){
        if(agentPool == null){
            agentPool = new GeneratedAgentList();
        }
        return agentPool;
    }
    
    public synchronized void startup() {
        HashMap<String, List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();
        for(String string : agentInfos.keySet()){
            for(AgentInstanceInfo info : agentInfos.get(string)){
                KeyValuePair<String, String> kp = new KeyValuePair<String, String>(info.getAgentName(), info.getAgentId());
                list.add(kp);
            }
        }
    }

    public synchronized void updateList(AbstractAgent agent, String group){
        boolean exist_flag = false;
        
        if(group.equals(SystemAPI.getAgentSphereId())){
            for(KeyValuePair<String, String> kp : list){
                if(agent.getAgentID().equals(kp.getValue())){//すでにリストにあるかどうかチェック
                    exist_flag = true;
                }
            }
            if(exist_flag == false){//リストに存在してなかったら追加
                KeyValuePair<String, String> pair = new KeyValuePair<String, String>(agent.getAgentName(), agent.getAgentID());
                list.add(pair);
            } 
        }
        //print();
    }
    
    public synchronized ArrayList<KeyValuePair<String, String>> getList(){
        return list;
    }
    
    public synchronized void generate(String agentName){
        
    }
    
    /**
     * デバッグ用
     */
    private void print(){
        StringBuilder buffer = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        String tab = "\t";

        for(KeyValuePair<String, String> kp : list){
            buffer.append(tab);
            buffer.append("AgentName->");
            buffer.append(kp.getKey());
            buffer.append("   AgentID->");
            buffer.append(kp.getValue());
            buffer.append(lineSeparator);
        }
        String text = new String(buffer);
        System.out.println(text);        
    }
}
