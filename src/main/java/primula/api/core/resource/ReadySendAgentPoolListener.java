/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.resource;

/**
 *
 * @author yamamoto
 */
public interface ReadySendAgentPoolListener {

    void readySendAgentAdded(String agentID, String agentName);
}
