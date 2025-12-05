/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction;

/**
 *
 * @author VENDETTA
 */
public class CommunicationException extends Exception {
    private static final long serialVersionUID = 8167143913678166249L;
    
    public CommunicationException(){
        super();
    }
    
    public CommunicationException(String message){
        super();
    }
    
    public CommunicationException(Throwable t){
        super(t);
    }
    
    public CommunicationException(String message,Throwable t){
        super(message, t);
    }
}
