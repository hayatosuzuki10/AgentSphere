/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.message;

import java.io.Serializable;

/**
 *
 * @author yamamoto
 */
public class StandardContentContainer extends AbstractContentContainer {

    private Serializable content;

    public StandardContentContainer(Serializable content) {
        this.content = content;
    }

    /**
     * @return the content
     */
    public Serializable getContent() {
        return content;
    }
}
