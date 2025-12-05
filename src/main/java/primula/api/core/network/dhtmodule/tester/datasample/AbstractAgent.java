/**
 *
 */
package primula.api.core.network.dhtmodule.tester.datasample;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author yamamoto
 */
public abstract class AbstractAgent implements Serializable {
    
    private String agentID;
    private InetAddress myIP;//生成された場所
    private int agentNumber=-1;

    public AbstractAgent() {
        setAgentID(genarateUniqeID());
        setMyIP();
    }
    
    public void setAgentNumber(int num){
        this.agentNumber=num;
    }

    private String genarateUniqeID() {
        return UUID.randomUUID().toString();
    }

    /**
     * AgentのユニークなIDを返します
     * @return
     */
    public String getAgentID() {
        return agentID;
    }

    /**
     * Agentの固有名を返します
     *
     * @return
     */
    public String getAgentName(){
        return this.getClass().getName();
    }

    /**
     * Agentの生成場所（AS）を返します
     *
     * @return
     */
    public InetAddress getmyIP(){
        return this.myIP;
    }

    /**
     * Agentはここから起動されます
     */
    public abstract void runAgent();

    /**
     * 実行中の処理へ終了の要求をします
     */
    public abstract void requestStop();

    /**
     * @param agentID the agentID to set
     */
    private void setAgentID(String id) {
        if (agentID == null) {
            this.agentID = id;
        }
    }

    /**
     * エージェント生成時に自身の生成された場所（AS）をセットする
     */
    private void setMyIP(){
        if (myIP == null){
            try {
                this.myIP = InetAddress.getLocalHost();
            } catch (UnknownHostException ex) {
                Logger.getLogger(AbstractAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
