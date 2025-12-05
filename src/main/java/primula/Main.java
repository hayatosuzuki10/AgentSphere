/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula;

import primula.api.core.AgentSphereCoreLoader;
import primula.api.core.network.dthmodule2.data.HubContainer;

/**
 *
 * @author yamamoto
 */
public class Main {
	public static HubContainer container = new HubContainer(2);

    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) {
        // TODO code application logic here
        AgentSphereCoreLoader loader = new AgentSphereCoreLoader();
        loader.start();
    }
}

//テストコメント