/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api;

import java.net.InetAddress;
import java.util.List;

import javassist.NotFoundException;
import primula.api.core.resource.NetworkResouce;
import primula.util.KeyValuePair;

/**
 *
 * @author yamamoto
 */
public class NetworkAPI {

    private static NetworkResouce networkResouce = NetworkResouce.getInstance();

    public synchronized static List<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>> getAddresses() {
        return networkResouce.getAddresses();
    }

    public synchronized static KeyValuePair<InetAddress, Integer> getAddressByAgentSphereId(String agentSphereId) throws NotFoundException {
        return networkResouce.getAddressByAgentSphereId(agentSphereId);
    }

    public synchronized static void setAddress(String agentSphereId, KeyValuePair<InetAddress, Integer> addresses) {
        networkResouce.setAddress(agentSphereId, addresses);
    }

    public synchronized static void setAddresses(List<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>> addresses) {
        for (KeyValuePair<String, KeyValuePair<InetAddress, Integer>> keyValuePair : addresses) {
            setAddress(keyValuePair.getKey(), keyValuePair.getValue());
        }
    }
}
