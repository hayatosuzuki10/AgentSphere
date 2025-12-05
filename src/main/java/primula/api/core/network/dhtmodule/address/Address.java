/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.address;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *ノードのアドレス、ポート情報等を保持するクラス
 * IPv4使用を想定
 * @author VENDETTA
 */
public class Address implements  Serializable{
    private static final long serialVersionUID = 6286064859600637337L;

    private String host;

    private int port;

    public Address(String host,int port){
        this.host=host;
        this.port=port;
        
    }
    /**
     * Hostを指定しない場合のコンストラクタ
     * VM等が存在しており、LocalIPが正しく取得できない場合には赤井さんのネットワーク機構を移植する必要あり。
     * @param port 
     */
    public Address(int port){
        try {
            this.host = Inet4Address.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Address.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.port=port;
    }
    
    public void setHost(String host){
        this.host=host;
        
    }
    

    public final String getHost(){
        return this.host;
    }

    public int getPort() {
       return this.port;
    }
    
    @Override
    public String toString(){
        return this.host+":"+this.port;
    }
}
