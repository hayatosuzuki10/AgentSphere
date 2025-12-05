/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing;

/**
 *
 * @author VENDETTA
 */
public interface MercuryFuture {
    
    
    public abstract Throwable getThrowable();
    
    public abstract boolean isDone() throws ServiceException;
    
    public abstract void waitForBeingDone() throws ServiceException,InterruptedException;
}
