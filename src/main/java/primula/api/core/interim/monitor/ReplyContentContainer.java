/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.monitor;

import primula.api.core.network.message.AbstractContentContainer;


/**
 * 返信を要する通信のContent
 * @author onda
 */
public class ReplyContentContainer extends AbstractContentContainer {
    private String agentID,agentSphereID,operation;
    private String groupName; //AgentGroup用
    public ReplyContentContainer(String agentID, String agentSphereID, String operation){
        this.agentID = agentID;
        this.agentSphereID = agentSphereID;
        this.operation = operation;
    }


    public ReplyContentContainer(String agentID, String agentSphereID, String operation, String groupName){
        this.agentID = agentID;
        this.agentSphereID = agentSphereID;
        this.operation = operation;
        this.groupName= groupName;
    }

    public void setAgentID(String agentID){
        this.agentID=agentID;
    }

    public String getAgentID(){
        return agentID;
    }

    public String getAgentSphereID(){
        return agentSphereID;
    }

    public String getOperation(){
        return operation;
    }

    public String getGroupName(){
        return groupName;
    }
}
