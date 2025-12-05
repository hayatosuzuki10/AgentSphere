package primula.api;

import java.net.InetAddress;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.api.core.network.IMessageServer;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.util.KeyValuePair;

/**
 * @author yamamoto
 */
public class MessageAPI {

    private static IMessageServer messageServer = (IMessageServer) SingletonS2ContainerFactory.getContainer().getComponent("MessageServer");

    /**
     * 同一AgentSphere内へメッセージを送信
     * @param envelope
     */
    public synchronized static void send(AbstractEnvelope envelope) {
        messageServer.send(envelope);
    }

    public synchronized static void send(KeyValuePair<InetAddress, Integer> address, AbstractEnvelope envelope) {
        messageServer.send(address, envelope);
    }

    /**
     * メッセージサーバに指定のlistenerを登録
     * @param listener
     * @throws IllegalArgumentException getStrictNameの戻り値がUUIDでない場合
     */

    public synchronized static void registerMessageListener(IMessageListener listener) throws IllegalArgumentException{
        messageServer.registerMessageListener(listener);
    }

    public synchronized static void removeMessageListener(IMessageListener listener) {
        messageServer.removeMessageListener(listener);
    }

    public synchronized static void sendToGroup(String group, AbstractEnvelope envelope) {
    }

    public synchronized static boolean checkMessageListener(IMessageListener listener) {
    	return messageServer.checkMessageListener(listener.getSimpleName());
    }

    public synchronized static IMessageListener getListener(IMessageListener listener) {
    	return messageServer.getListener(listener.getSimpleName());
    }
}
