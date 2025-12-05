/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.resource;

/**
 *
 * @author yamamoto
 */
public interface ReadyRunAgentPoolListener {

    void readyRunAgentAdded(String agentID, String agentName);
}
