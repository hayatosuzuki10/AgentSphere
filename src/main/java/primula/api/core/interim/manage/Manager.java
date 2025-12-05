/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.manage;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.SystemAPI;
import primula.api.core.agent.AgentInfo;
import primula.util.KeyValuePair;

/**
 *
 * @author kurosaki
 */

 /**
  * 生まれた場所が同じエージェントの生存を確認するためのエージェント
  * 
  */
public class Manager extends AbstractAgent{
    private KeyValuePair<String, ArrayList<KeyValuePair<Boolean, KeyValuePair<String, String>>>> list = new KeyValuePair<String, ArrayList<KeyValuePair<Boolean, KeyValuePair<String, String>>>>();
    private GeneratedAgentList generatedAgentList = GeneratedAgentList.getInstance();
    private boolean flag = true;
    private boolean finish = false;
    
    @Override
    public void runAgent() {
        //自ASに戻ってきたらfinishをtrueにする
        if(flag == false && SystemAPI.getAgentSphereId().equals(list.getKey())){
            finish = true;
        }
        //移動後の処理はfalseの時に行う
        if(flag == false){
            find();
            print();   
            if(finish == false){
                try{
                    KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName("192.168.114.13"), 55878);
                    AgentAPI.migration(address, this);
                } catch(UnknownHostException ex){
                    Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else{
                generateCommand();
            }
        }
        //生成時のみ情報を取得する
        else{
            setList();
            print();
            flag = false;
            //逐次に巡回させてるけどブロードキャストしたほうがよさそう。マシンリストないけど
            try{
                KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName("192.168.114.12"), 55878);
                AgentAPI.migration(address, this);
            } catch(UnknownHostException ex){
                Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }     
        /**
         * 本来は移動処理を一括で書きたいけど現状マシンリストがないので別々の移動処理を書いている
         *   try {
         *       KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName("192.168.114.12"), 55878);
         *       AgentAPI.migration(address, this);
         *   } catch (UnknownHostException ex) {
         *       Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
         *   }
         * 
         */
    }
    
    private void setList(){ 
        ArrayList<KeyValuePair<Boolean, KeyValuePair<String, String>>> addedList = new ArrayList<KeyValuePair<Boolean, KeyValuePair<String, String>>>();
        
        list.setKey(SystemAPI.getAgentSphereId());
        for(KeyValuePair<String, String> pair : generatedAgentList.getList()){
            if(("primula.api.core.interim.shell.ShellAgent".equals(pair.getKey())) || ("primula.api.core.interim.monitor.StationaryAgent".equals(pair.getKey())) || ("primula.api.core.interim.testagent.MessageQueueAgent".equals(pair.getKey()))){
                KeyValuePair<Boolean, KeyValuePair<String, String>> adder = new KeyValuePair<Boolean, KeyValuePair<String, String>>();
                adder.setKey(Boolean.TRUE);
                adder.setValue(pair);
                addedList.add(adder);
            }
            else{
                KeyValuePair<Boolean, KeyValuePair<String, String>> adder = new KeyValuePair<Boolean, KeyValuePair<String, String>>();
                adder.setKey(Boolean.FALSE);
                adder.setValue(pair);
                addedList.add(adder);
            }
        }
        list.setValue(addedList);
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * デバッグ用
     */
    private void print(){
        StringBuilder buffer = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        String tab = "\t";
        buffer.append(list.getKey()+":");
        buffer.append(lineSeparator);
        for(KeyValuePair<Boolean, KeyValuePair<String, String>> pair : list.getValue()){
            buffer.append(tab);
            buffer.append(pair.getKey());
            buffer.append(tab);
            buffer.append("AgentName->");
            buffer.append(pair.getValue().getKey());
            buffer.append("   AgentID->");
            buffer.append(pair.getValue().getValue());
            buffer.append(lineSeparator);
        }
        String text = new String(buffer);
        System.out.print(text);        
    }
    
    /**
     * 移動後のASで特定のエージェントを探して、リストを更新する
     */
    private void find(){
        HashMap<String, List<AgentInfo>> agentInfos = AgentAPI.getAgentInfos();
        for(String string : agentInfos.keySet()){
            if(string.equals(list.getKey())){
                for(AgentInfo info : agentInfos.get(string)){
                    for(int i = 0; i < list.getValue().size(); i++){
                        if(info.getAgentName().equals(list.getValue().get(i).getValue().getKey()) && info.getAgentId().equals(list.getValue().get(i).getValue().getValue())){
                            list.getValue().get(i).setKey(true);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 再発行が必要なエージェントがいたらそのエージェントを復元するように命令をだす
     */
    private void generateCommand(){
        for(KeyValuePair<Boolean, KeyValuePair<String, String>> pair : list.getValue()){
            if(pair.getKey() == false){
                generatedAgentList.generate(pair.getValue().getKey());
            }
        }
    }
}
