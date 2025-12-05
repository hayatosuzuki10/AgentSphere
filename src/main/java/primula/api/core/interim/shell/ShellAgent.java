/**
 * 
 */
package primula.api.core.interim.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.seasar.framework.container.autoregister.FileSystemComponentAutoRegister;
import org.seasar.framework.container.factory.S2ContainerFactory;

import primula.agent.AbstractAgent;
import primula.api.core.interim.shell.command.AbstractCommand;

/**
 * @author sumikof
 * 
 */
public class ShellAgent extends AbstractAgent {

    private ShellEnvironment shellEnv = new ShellEnvironment();
    private Map<String, AbstractCommand> commands = new HashMap<String, AbstractCommand>();

    @Override
    public String getAgentName() {
        return this.getClass().getName();
    }

    @Override
    public void runAgent() {
        org.seasar.framework.container.S2Container s2Container = S2ContainerFactory.create("setting/ShellCommand.dicon");
        org.seasar.framework.container.S2Container container = ((FileSystemComponentAutoRegister) s2Container.getComponent("commandRegister")).getContainer();
        for (int i = 0; i < container.getComponentDefSize(); i++) {
            if (container.getComponentDef(i).getComponent() instanceof AbstractCommand) {
                registerCommand((AbstractCommand) container.getComponentDef(i).getComponent());
            }
        }

        try {
            BufferedReader buf = new BufferedReader(
                    new InputStreamReader(System.in));
            while (!shellEnv.isStop()) {

                System.out.print(shellEnv.getInputLineText());
                String line = buf.readLine();
                try {
                    ArrayList<String> cmd = new ArrayList<String>();
                    for (String s : line.split("\\s+")) {
                        cmd.add(s);
                    }
                    commands.get(cmd.get(0)).runCommand(shellEnv, getOptionCommand(cmd));
                } catch (NullPointerException e) {
                    System.err.println("Not Command");
                }

            }
            buf.close();
        } catch (IOException ex) {
        }
    }

    @Override
    public void requestStop() {
        // TODO 自動生成されたメソッド・スタブ
    }

    public void registerCommand(AbstractCommand command) {
        commands.put(command.getCommandName(), command);
    }

    private List<String> getOptionCommand(List<String> cmd) {
        if (cmd.size() > 1) {
            return cmd.subList(1, cmd.size());
        }
        return new ArrayList<String>();
    }
}
