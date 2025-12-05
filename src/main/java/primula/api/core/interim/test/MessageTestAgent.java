/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.test;

import primula.agent.AbstractAgent;
import primula.api.core.network.AgentAddress;

/**
 *
 * @author yamamoto
 */
@Deprecated
@SuppressWarnings("直せば使える")
public class MessageTestAgent extends AbstractAgent {

    private String agentSphereId;
    private AgentAddress address;

    public MessageTestAgent(String agentSphereId, AgentAddress address) {
        this.agentSphereId = agentSphereId;
        this.address = address;
    }

    public MessageTestAgent() {
    }

    @Override
    public String getAgentName() {
        return "MessageTestAgent";
    }

    @Override
    public void runAgent() {
//        StringBuilder builder = new StringBuilder();
//        for (AgentInfo agentInfo : AgentAPI.getAgentInfos()) {
//            builder.append(agentInfo.getAgentName() + ":" + agentInfo.getAgentId() + System.getProperty("line.separator"));
//        }
//        StandardContent content = new StandardContent(builder.toString());
//        StandardEnvelope envelope = new StandardEnvelope(address, content);
//        try {
//            MessageAPI.send(NetworkAPI.getAddressByAgentSphereId(agentSphereId), envelope);
//        } catch (NotFoundException ex) {
//            Logger.getLogger(MessageTestAgent.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
