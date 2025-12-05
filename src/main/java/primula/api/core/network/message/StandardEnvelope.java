/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.message;

import primula.api.core.network.AgentAddress;

/**
 *
 * @author yamamoto
 */
public class StandardEnvelope extends AbstractEnvelope {

    public StandardEnvelope(AgentAddress agentAddress, AbstractContentContainer content) {
        setTargetAgentAddress(agentAddress);
        setContent(content);
    }
}
