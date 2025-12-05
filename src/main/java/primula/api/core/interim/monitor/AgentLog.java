/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.monitor;

import java.io.Serializable;

/**
 *
 * @author onda
 */
public class AgentLog implements Serializable{

    String AgentName, AgentID, AgentMove;

    public AgentLog(String AgentName, String AgentID, String AgentMove) {
        this.AgentName = AgentName;
        this.AgentID = AgentID;
        this.AgentMove = AgentMove;
    }

   public String getAgentName() {
        return AgentName;
    }

    public String getAgentID() {
        return AgentID;
    }

    public String getAgentMove() {
        return AgentMove;
    }
}
