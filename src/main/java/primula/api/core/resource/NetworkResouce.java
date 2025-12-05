/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.resource;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javassist.NotFoundException;
import primula.api.core.network.CommunicationInfo;
import primula.util.KeyValuePair;

/**
 *
 * @author yamamoto
 */
public class NetworkResouce {

    private CopyOnWriteArrayList<CommunicationInfo> communicationInfos = new CopyOnWriteArrayList<CommunicationInfo>();
    private static NetworkResouce networkResouce;

    private NetworkResouce() {
    }

    public static NetworkResouce getInstance() {
        if (networkResouce == null) {
            networkResouce = new NetworkResouce();
        }
        return networkResouce;
    }

    /**
     * @return the agentSphereInfos
     */
    public List<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>> getAddresses() {
        ArrayList<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>> list = new ArrayList<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>>();
        for (CommunicationInfo info : communicationInfos.subList(0, communicationInfos.size())) {
            list.add(new KeyValuePair<String, KeyValuePair<InetAddress, Integer>>(info.getAgentSphereId(), new KeyValuePair<InetAddress, Integer>(info.getAddress(), info.getPort())));
        }
        return list;
    }

    public void setAddress(String agentSphereId, KeyValuePair<InetAddress, Integer> address) {
        if (!containAddress(address)) {
            communicationInfos.add(new CommunicationInfo(agentSphereId, address.getKey(), address.getValue()));
        } else {
            updateAddress(address);
        }
    }

    private boolean containAddress(KeyValuePair<InetAddress, Integer> address) {
        for (CommunicationInfo info : communicationInfos) {
            if (info.getAddress().getHostAddress() == null ? address.getKey().getHostAddress() == null : info.getAddress().getHostAddress().equals(address.getKey().getHostAddress())) {
                return true;
            }
        }
        return false;
    }

    void updateAddress(KeyValuePair<InetAddress, Integer> address) {
        //TODO:アドレス更新を実装する
        //  throw new UnsupportedOperationException("Not supported yet.");
    }

    public KeyValuePair<InetAddress, Integer> getAddressByAgentSphereId(String agentSphereId) throws NotFoundException {
        for (CommunicationInfo communicationInfo : communicationInfos) {
            if (communicationInfo.getAgentSphereId().equals(agentSphereId)) {
                return new KeyValuePair<InetAddress, Integer>(communicationInfo.getAddress(), communicationInfo.getPort());
            }
        }
        throw new NotFoundException(agentSphereId);
    }
}
