package primula.api.core.network;

import java.net.InetAddress;

import primula.api.core.ICoreModule;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.util.KeyValuePair;

/**
 * @see primula.api.core.network.message.AbstractEnvelope
 * @author yamamoto
 *
 */
public interface IMessageServer extends ICoreModule {

    /**
     * 同一AgentSphere内へメッセージを送信
     * @param envelope
     */
    void send(AbstractEnvelope envelope);

    void send(KeyValuePair<InetAddress, Integer> address, AbstractEnvelope envelope);

    /**
     * メッセージング機能を使用する際にはこの関数を使用しメッセージングサーバー機能に該当オブジェクトを登録する必要があります。
     * @param listener
     * @throws IllegalArgumentException
     */
    void registerMessageListener(IMessageListener listener) throws IllegalArgumentException;

    void removeMessageListener(IMessageListener listener);

    /**
     * AgentGroupへメッセージを送信します。Envelopeへの宛先は無視されます
     * @param group
     * @param envelope
     */
    void sendToGroup(String group, AbstractEnvelope envelope);

    boolean checkMessageListener(String simpleAgentName);
    IMessageListener getListener(String simpleAgentName);
}
