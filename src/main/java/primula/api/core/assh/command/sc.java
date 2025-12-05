package primula.api.core.assh.command;

import java.util.List;

import primula.api.core.assh.ConsolePanel;


//SphereTerminalを動作させるコマンド
public class sc extends AbstractCommand {
	private StandardStreamPane standardStreamPane;
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {

		/*if(instance == null && fileNames.isEmpty()) {
			InstanceCreator ic = new InstanceCreator();
			data = new ArrayList<Object>();
			data.add(ic.selectDirectory());
		}
		else if(instance == null && !fileNames.isEmpty()) {
			InstanceCreator ic = new InstanceCreator(fileNames.get(0));
			data = new ArrayList<Object>();
			data.add(ic.selectDirectory());
		}
		else {
			if(AgentClassData.containsKey((String) instance)) {
				ObjectIO oio = new ObjectIO();
				Object obj = null;
				try {
					obj = oio.getObject(AgentClassData.getAgentBinary((String) instance));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				AbstractAgent agent = (AbstractAgent) obj;
				AgentAPI.runAgent(agent);
			}
		}*/
		/*JFrame frame = new JFrame("SphereTerminal");
		frame.setBounds(100, 100, 640, 480);
	    frame.setVisible(true);*/
		ConsolePanel panel=new ConsolePanel();
		panel.setVisible(true);//ここまで2行をお記載すると、Consoleが表示される
		return data;
	}
}
