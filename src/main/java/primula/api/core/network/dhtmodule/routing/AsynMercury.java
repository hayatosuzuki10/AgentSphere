/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing;

import primula.api.core.network.dhtmodule.address.Address;

/**
 *
 * @author VENDETTA
 */
public interface AsynMercury {
    
    public abstract Address getAddress();
    
    public abstract void setAddress(Address nodeAddress ) throws IllegalStateException;

    public abstract void create() throws ServiceException;
    
    public abstract void create(Address localAddress) throws ServiceException;
    
    public abstract void join(Address bootstrapAddress)throws ServiceException;
    
    public abstract void join(Address localAddress,Address bootstrapAddress) throws ServiceException;
    
    public abstract void leave() throws ServiceException;
    
}
