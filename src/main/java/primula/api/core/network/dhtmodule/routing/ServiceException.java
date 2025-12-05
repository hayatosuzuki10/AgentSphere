/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing;

/**
 *
 * @author VENDETTA
 */
public final class ServiceException extends Exception{
    
    private static final long serialVersionUID = 2369650089017379276L;
    
    public ServiceException(String message){
        super(message);
    }
    
    public ServiceException(String message,Throwable t){
        super(message, t);
    }
    
}
