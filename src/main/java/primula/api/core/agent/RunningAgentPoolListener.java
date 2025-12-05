/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.agent;

/**
 *
 * @author yamamoto
 */
public interface RunningAgentPoolListener {

    void agentFinished(String agentID, String agentName);
}
