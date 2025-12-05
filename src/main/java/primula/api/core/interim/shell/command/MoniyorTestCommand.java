/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.shell.command;

import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.interim.monitor.MonitorTestAgent;
import primula.api.core.interim.shell.ShellEnvironment;
/**
 *
 * @author selab
 */
public class MoniyorTestCommand  extends AbstractCommand{

    @Override
  public String getCommandName(){
      return "montest";
  }

    @Override
  public void runCommand(ShellEnvironment environment, List<String> args) {
     MonitorTestAgent agent = new MonitorTestAgent();
     AgentAPI.runAgent(agent,"AgentMonitor");
    }

}
