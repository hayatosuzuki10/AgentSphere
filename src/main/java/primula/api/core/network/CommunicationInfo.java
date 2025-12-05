/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;

import primula.util.KeyValuePair;

/**
 * アドレスの使用履歴などを格納
 * @author yamamoto
 */
public class CommunicationInfo {

    /**
     * 最終使用時刻を記録
     */
    private Date lastAccessTime;
    /**
     * アドレス
     */
    private KeyValuePair<InetAddress, Integer> address;
    /**
     *
     */
    private String agentSphereId;

    public CommunicationInfo(String agentSphereId, InetAddress address, int port) {
        this.agentSphereId = agentSphereId;
        this.address = new KeyValuePair<InetAddress, Integer>(address, port);
        updateAccessTime();
    }

    /**
     * @return the address
     */
    public InetAddress getAddress() {
        return address.getKey();
    }

    public int getPort() {
        return address.getValue();
    }

    public String getAgentSphereId() {
        return agentSphereId;
    }

    /**
     * 最終使用時刻を更新
     */
    public void updateAccessTime() {
        lastAccessTime = Calendar.getInstance().getTime();
    }
}
