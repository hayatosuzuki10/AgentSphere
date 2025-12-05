/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.message;

import primula.api.MessageAPI;

/**
 * メッセージを送信するための標準的なメッセンジャーです
 * 対象ASに到着後対象Agentがいない場合メッセージはsendメソッドが処理します
 * @author yamamoto
 */
public class MessengerAgent extends AbstractMessenger {

    private AbstractEnvelope envelope;

    public MessengerAgent(AbstractEnvelope envelope) {
        this.envelope = envelope;
    }

    @Override
    public String getAgentName() {
        return "MessengerAgent";
    }

    @Override
    /**
     * 送信先にたどり着いた際にこのメソッドが呼ばれ、<br>
     * 現在いるAgentSphereにsendすることで送信を完了します
     */
    public void runAgent() {
        MessageAPI.send(envelope);
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @return the envelope
     */
    public AbstractEnvelope getEnvelope() {
        return envelope;
    }
}