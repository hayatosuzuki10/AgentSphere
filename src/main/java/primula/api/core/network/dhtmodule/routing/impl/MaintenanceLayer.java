/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing.impl;

import java.util.ArrayList;
import java.util.logging.Level;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.data.Hub;
import primula.api.core.network.dhtmodule.data.HubContainer;
import primula.api.core.network.dhtmodule.data.MachineData;
import primula.api.core.network.dhtmodule.data.MachineDataManeger;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;
import primula.api.core.network.dhtmodule.utill.Logger;

/**
 *
 * @author kousuke
 */
public class MaintenanceLayer implements Runnable {

    private int TTL;

    private MachineData ownThreshold;

    private  MachineDataManeger maneger;

    private NodeImpl impl;

    private Logger logger;

    private HubContainer container;

    public MaintenanceLayer(MachineData ownThreshold, NodeImpl impl,HubContainer container) {
        this.ownThreshold = ownThreshold;
        this.maneger = new MachineDataManeger(ownThreshold);
        this.impl = impl;
        this.container=container;
        this.logger = Logger.getLogger(MaintenanceLayer.class.getName()+impl.toString());
    }




    private boolean checkThreshold(){
       return this.maneger.checkThreshold();
    }

    private void createHub(){
        ArrayList<Address> addresess= new ArrayList<Address>();
        Hub hubToSplit=this.container.getHubRandomly(impl);
        for(int i = 0;i<this.TTL;i++){
           addresess.add(this.container.getAccessLinkRandomly().getNodeAddress());
        }
        try {
            impl.createHubNetwork(TTL, hubToSplit, addresess);
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(MaintenanceLayer.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    @Override
    public void run() {
        if(this.checkThreshold()){
            this.logger.debug("Start creating HUB");

        }else{
//            this.logger.debug("NOT started Creating hub");//////////////////////////////////////////////////debag
        }
    }

}
