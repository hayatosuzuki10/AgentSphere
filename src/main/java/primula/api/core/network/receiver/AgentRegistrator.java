/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.receiver;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import primula.api.core.network.receiver.testsocket.SenderThread;
import primula.util.KeyValuePair;

/**
 *
 * @author kurosaki
 */
public class AgentRegistrator { // エージェントの登録・管理・通知を行うシングルトンクラス(インスタンスが一つしかない)
    private ArrayList<String> agentListeners = new ArrayList<String>(); // 実行しているエージェントリスト
    private static AgentRegistrator agentPool; // 唯一のインスタンス
    
    public static synchronized AgentRegistrator getInstance(){
        if(agentPool == null){ // シングルトンクラスにするための機構
            agentPool = new AgentRegistrator();
        }
        return agentPool;
    }
    
    public void registerAgentID(String targetAgentID) throws Exception {
        String regex = "[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}"; // エージェントIDの規格
        Pattern agentIDPattern = Pattern.compile(regex);
        if (agentIDPattern.matcher(targetAgentID).find()) { // エージェントIDの企画と一致したら
            agentListeners.add(targetAgentID); // 実行エージェントリストに追加
        } else {
            throw new Exception("StrictNameがUUIDではありません");
        }
    }
    
    public void removeAgentID(String targetAgentID) { 
        //KeyValuePair<IAgentListener, String> kvp = new KeyValuePair<IAgentListener, String>(listener, name);
        for(String agentListenerID : agentListeners){ // 実行しているエージェントリストの一つ一つにアクセス
            if(targetAgentID.equals(agentListenerID)){ // エージェントIDが削除したいエージェントIDと一致したら
                agentListeners.remove(agentListenerID); // リストから削除
                break;
            }
        }
    }
    
    public void receive(String targetAgentID){ // エージェントを受け取った時に登録する関数
        //System.out.println("IN");
        for(String agentID : agentListeners){
           //System.out.println("name:"+name+"  :"+listener);
            if(targetAgentID.equals(agentID)){
                //System.out.println("IN2");
                System.out.println("エージェント受信完了");
                this.removeAgentID(targetAgentID);
                try {
                    SenderThread.task.put(new KeyValuePair<String, String>("finish", agentID));
                } catch (InterruptedException ex) {
                    Logger.getLogger(AgentReceiveThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                //listener.getKey().receivedAgent(listener.getValue());
                break;
            }     
        }
    }
    
    /**
     * デバッグ用
     */
    /*
    public void print(){
        StringBuilder buffer = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        String tab = "\t";

        for(KeyValuePair<IAgentListener, String> kp : agentListeners){
            buffer.append(tab);
            buffer.append("RegistedAgentName->");
            buffer.append(kp.getKey());
            buffer.append("   receivedAgenr->");
            buffer.append(kp.getValue());
            buffer.append(lineSeparator);
        }
        String text = new String(buffer);
        System.out.println(text);        
    }
     * 
     */
}
