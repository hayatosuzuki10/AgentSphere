/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network;

import java.io.Serializable;

/**
 *
 * @author yamamoto
 */
public class AgentAddress implements Serializable {

    private String address;

    public AgentAddress() {
    }

    /**
     * AgentID(≒IMessageListener#getStrictNameの戻り値)もしくはAgentNameを指定してください
     * @param address
     */
    public AgentAddress(String address) {
        this.address = address;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }
}
