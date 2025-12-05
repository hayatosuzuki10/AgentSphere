package primula.api.core.scheduler.vmemory;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class VMemoryViewer extends JFrame {
	private JLabel label = new JLabel();
	
	public VMemoryViewer(String title){
		super(title);
		this.setSize(200, 200);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		this.setContentPane(label);
		
		this.setVisible(true);
	}
	
	public void setLabel(String str){
		this.label.setText(str);
	}

}
