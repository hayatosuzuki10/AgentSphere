/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction.impl;

import java.io.Serializable;

import primula.api.core.network.dhtmodule.address.Address;


/**
 *
 * @author VENDETTA
 */
final class RemoteNodeInfo implements Serializable{
    private static final long serialVersionUID = 4492630184480812452L;

    protected Address nodeAddress;

    
    public RemoteNodeInfo(Address nodeAddress) {
        this.nodeAddress=nodeAddress;
    }
    

    
    protected Address getNodeAddress(){
        return this.nodeAddress;
    }
    

    
    protected void setNodeAddress(Address address){
        this.nodeAddress=address;
    }
}
