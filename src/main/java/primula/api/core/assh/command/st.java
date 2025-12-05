package primula.api.core.assh.command;

import java.util.List;

import javax.swing.JScrollPane;

import primula.api.core.assh.MainPanel;
import primula.api.core.assh.OutputTextArea;

class StandardStreamPane extends JScrollPane {
	private OutputTextArea out;

	public StandardStreamPane(){
		super();
		out = new OutputTextArea();
		out.setToSystemOut();
		//out.setToSystemErr(); //Errorも標準出力と同じとこに出力する。
		setViewportView(out);
		setFocusable(false);
		//OutputTextArea err = new OutputTextArea();
		//err.setToSystemErr();
		//this.add(err);
	}

	/**
	 * 標準出力、標準エラー出力を元に戻す
	 *
	 */
	public void returnStream(){
		//out.setBackSystemOut();
		//out.setBackSystemErr();
	}

	/**
	 * このペインに埋め込まれたテキストエリアを返します。
	 * @return out - 標準出力が出力されているテキストエリアのインスタンス
	 */
	public OutputTextArea getTextArea(){
		return out;
	}
}



//SphereTerminalを動作させるコマンド
public class st extends AbstractCommand {
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
		MainPanel panel=new MainPanel();
		panel.setVisible(true);//ここまで2行をお記載すると、Terminalが表示される
		return data;
	}
}
