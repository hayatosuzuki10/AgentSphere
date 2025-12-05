package sphereConcurrent.test;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.NetworkAPI;

/**
 *
 * @author akita
 */
public class Migrator extends AbstractAgent{
    int a;
    Integer b;
    public Migrator(){
        a=b=0;
    }

    @Override
    public void runAgent() {
        System.err.println("a : "+a+", b : "+b);
        if(a==10 || b == 10){
            System.err.println("End to a : "+a+", b : "+b);
            return;
        }
        a++;b++;
        AgentAPI.migration(NetworkAPI.getAddresses().get(0).getValue(), this);
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
