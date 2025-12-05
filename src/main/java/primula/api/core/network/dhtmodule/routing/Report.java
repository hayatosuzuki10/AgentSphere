/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing;

/**
 *
 * @author VENDETTA
 */
public interface Report {
    
    public abstract String printEntries();
    
    public abstract String printLongDistanceTable();
    
    /**
     * Successorをリストにするかどうかは未定
     * @return 
     */
    public abstract  String printSuccessorList();
    
    public abstract String printReferences();
    
    public abstract String printPredecessor();
}
