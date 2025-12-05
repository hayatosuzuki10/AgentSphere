/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.receiver;

/**
 *
 * @author kurosaki
 */
/*
 * 現在使用されてない。そのうち消すと思う
 */
public interface IAgentListener {
    String getStrictName();
    void receivedAgent(String ID);
}
