/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.nodefunction.impl;

/**
 *
 * @author VENDETTA
 */
final class MethodConstants {
    
    static final int CONNECT = -1;
    
    static final int FIND_SUCCESSOR= 0;

    static final int JOIN_REQUEST=1;
    
    
    static final int LEAVES_NETWORK=2;
    
    static final int SHUTDOWN = 3;
    
    static final int NOTIFY = 4;
    
    static final int INSERT_ENTRY=5;
    
    static final int GET_ENTRY = 6;
    
    static final int JOIN_MERCURY_REQUEST= 7;
    
    static final int ACCEPT_MERCURY = 8;

    static final int SEND_HUB = 9;
    
    static final int CREATE_HUB_NETWORK=10;
    
    static final int  GET_HISTOGRAM_DATA=11;
    
    static final int INSERT_MULTI_ENTRY =12;
    
    static final String[] METHOD_NAMES = new String[]{
    "CONNECT","JOIN_REQUEST","GET_NODE_ID","LEAVES_NETWORK","SHUTDOWN","NOTIFY","INSERT_ENTRY","GET_ENTRY"
   ,"JOIN_MERCURY_REQUEST","ACCEPT_MERCURY","SEND_HUB","CREATE_HUB_NETWORK,GET_HISTOGRAM_DATA","INSERT_MULTI_ENTRY" };
    
    static String getMethodName(int methodIdentifier){
        return METHOD_NAMES[methodIdentifier];
    }
}
