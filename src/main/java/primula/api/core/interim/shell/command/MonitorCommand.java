/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.shell.command;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.api.AgentAPI;
import primula.api.core.interim.monitor.AgentMonitorAgent;
import primula.api.core.interim.shell.ShellEnvironment;
import primula.util.KeyValuePair;
/**
 *
 * @author onda
 */
public class MonitorCommand extends AbstractCommand{

    @Override
  public String getCommandName(){
      return "am";
  }
  
    @Override
  public void runCommand(ShellEnvironment environment, List<String> args) {
        AgentMonitorAgent agent = new AgentMonitorAgent();
        try {
            KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName("192.168.114.13"),55878);
            AgentAPI.migration(address, agent);
        } catch (UnknownHostException ex) {
            Logger.getLogger(MonitorCommand.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //AgentAPI.runAgent(agent,"AgentMonitor");
        
    }
}
