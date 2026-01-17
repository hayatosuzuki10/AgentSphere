/**
 *
 */
package primula.api.core.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import primula.agent.util.DHTutil;
import primula.api.AgentAPI;
import primula.api.core.agent.AgentInstanceInfo;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.MessengerAgent;
import primula.util.AgentUtil;
import primula.util.KeyValuePair;

/**
 * @author yamamoto
 *
 */
public class MessageServer implements IMessageServer {

    private ArrayList<IMessageListener> messageListeners = new ArrayList<IMessageListener>();

    public void send() {
    }

    @Override
    public void send(AbstractEnvelope envelope) {
        try {
            AbstractEnvelope newEnvelope = AgentUtil.easyDeepCopy(envelope);

            for (IMessageListener listener : messageListeners) {
                if ((listener.getStrictName() == null ? newEnvelope.getTargetAgentAddress().getAddress() == null
                        : listener.getStrictName().equals(newEnvelope.getTargetAgentAddress().getAddress()))
                    || (listener.getSimpleName() == null ? newEnvelope.getTargetAgentAddress().getAddress() == null
                        : listener.getSimpleName().equals(newEnvelope.getTargetAgentAddress().getAddress()))) {

                    System.out.println("ifの中:" + listener);
                    listener.receivedMessage(newEnvelope);
                    return;
                }
            }

            // リスナーに存在しなかった場合 → DHT経由で再送
            if (newEnvelope.getTTL() > 0 && DHTutil.containsAgentIP(newEnvelope.getTargetAgentAddress().getAddress())) {
                newEnvelope.decrementTTL();
                InetAddress resendAddr = DHTutil.getAgentIP(newEnvelope.getTargetAgentAddress().getAddress());

                System.err.println("[MessageServer] TTL残り=" + newEnvelope.getTTL() + 
                                   " → 再送先IP=" + resendAddr.getHostAddress() + 
                                   " agentID=" + newEnvelope.getTargetAgentAddress().getAddress());

                send(new KeyValuePair<>(resendAddr, 55878), newEnvelope);
                return;
            }

            // TTL = 0 or DHTにも存在しない → 廃棄
            String targetId = newEnvelope.getTargetAgentAddress().getAddress();
            boolean existedInDHT = DHTutil.containsAgentIP(targetId);
            int ttl = newEnvelope.getTTL();

            System.err.println("[MessageServer] メッセージ廃棄: agentID=" + targetId +
                               " TTL=" + ttl +
                               " DHT存在=" + existedInDHT +
                               " envelope=" + envelope);

        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(MessageServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initializeCoreModele() {
    }

    @Override
    public void finalizeCoreModule() {
    }

    @Override
    public void registerMessageListener(IMessageListener listener) throws IllegalArgumentException{
    	if(!messageListeners.contains(listener)) {
    		String regex = "[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}";
    		Pattern p = Pattern.compile(regex);
    		if (p.matcher(listener.getStrictName()).find()) {
    			messageListeners.add(listener);
    		} else {
    			throw new IllegalArgumentException("StrictNameがUUIDではありません");
    		}
    	}
    }

    @Override
    public void removeMessageListener(IMessageListener listener) {
        messageListeners.remove(listener);
    }

    @Override
    public void send(KeyValuePair<InetAddress, Integer> address, AbstractEnvelope envelope) {
        MessengerAgent agent = new MessengerAgent(envelope);
        AgentAPI.migration(address, agent);
    }

    @Override
    public void sendToGroup(String group, AbstractEnvelope envelope) {
        try {
            AbstractEnvelope newEnvelope;
            newEnvelope = AgentUtil.easyDeepCopy(envelope);

            HashMap<String, List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();
            if (!agentInfos.containsKey(group)) {
                return;
            }
            for (AgentInstanceInfo agentInfo : agentInfos.get(group)) {
                for (IMessageListener listener : messageListeners) {
                    if ((agentInfo.getAgentId() == null ? listener.getStrictName() == null : agentInfo.getAgentId().equals(listener.getStrictName()))
                            || (agentInfo.getAgentName() == null ? listener.getSimpleName() == null : agentInfo.getAgentName().equals(listener.getSimpleName()))) {
                        listener.receivedMessage(newEnvelope);
                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(MessageServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MessageServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean checkMessageListener(String simpleAgentName) {
    	for(IMessageListener m : messageListeners) {
    		if(m.getSimpleName().equals(simpleAgentName)) {
    			return true;
    		}
    	}
    	return false;
    }

	@Override
	public IMessageListener getListener(String simpleAgentName) {
    	for(IMessageListener m : messageListeners) {
    		if(m.getSimpleName().equals(simpleAgentName)) {
    			System.out.println(m + " ********");//////////////////
    			return m;
    		}
    	}
    	return null;
	}
}
