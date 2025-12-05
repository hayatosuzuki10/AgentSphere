/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing.impl;

import primula.api.core.network.dhtmodule.routing.MercuryFuture;
import primula.api.core.network.dhtmodule.routing.ServiceException;

/**
 *
 * @author VENDETTA
 */
public class MercuryFutureImpl implements MercuryFuture {

    private boolean isDone =false;
    
    private Throwable throwable = null;
    
    protected MercuryFutureImpl(){
        //なにもしない
    }
    
    final void setIsDone(){
        synchronized(this){
            this.isDone=true;
            this.notifyAll();
        }
    }
    
    
    @Override
    public Throwable getThrowable() {
        return this.throwable;
    }

    @Override
    public boolean isDone() throws ServiceException {
        if(this.throwable!=null){
            throw new ServiceException(this.throwable.getMessage(),this.throwable);
        }
        return this.isDone;
    }

    @Override
    public void waitForBeingDone() throws ServiceException, InterruptedException {
        synchronized(this){
            while(!this.isDone()){
                this.wait();
            }
        }
    }
    
}
