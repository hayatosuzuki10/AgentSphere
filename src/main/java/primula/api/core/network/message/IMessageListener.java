/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.message;

/**
 *
 * @author yamamoto
 */
public interface IMessageListener {

    /**
     * 厳密な指定方法
     * <p>
     * 戻り値はUUID形式に沿った文字列でなければなりません
     * </p>
     * @return このリスナーを表すUUID形式の文字列
     * @see java.util.UUID
     */
    String getStrictName();

    /**
     * 簡易的な指定
     * @return
     */
    String getSimpleName();

    void receivedMessage(AbstractEnvelope envelope);


}
