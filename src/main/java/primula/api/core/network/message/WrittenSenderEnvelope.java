package primula.api.core.network.message;

import primula.api.core.network.AgentAddress;

//okubo 
//2013/11/13
//送り主が記載されたEnvelope

public class WrittenSenderEnvelope extends AbstractEnvelope{

	String sender;
	
    public WrittenSenderEnvelope(AgentAddress agentAddress, AbstractContentContainer content, String sender) {
        setTargetAgentAddress(agentAddress);
        setContent(content);
        this.sender = sender;
    }
    
    public String getSenderName() {
    	return sender;
    }

}
