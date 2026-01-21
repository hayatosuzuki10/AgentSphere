package scheduler2022;

import javax.swing.JFrame;
import javax.swing.JPanel;

import primula.util.IPAddress;

public class WarningPanel extends JFrame{
	private String[] CanMigratePCs;
	private String dest;

	public String getdest() {
		return dest;
	}



	public WarningPanel() {
		JPanel panel = new JPanel();

		CanMigratePCs = new String[InformationCenter.getOthersIPs().size()];
		int i = 0;
		for(String str : InformationCenter.getOthersIPs()) {
			if(IPAddress.myIPAddress != str) {
				CanMigratePCs[i] = str;
			}
			i++;
		}




		this.getContentPane().add(panel);
	}

	public void ViewPanel() {
		JFrame frame = new WarningPanel();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setTitle("WarningPanel");
        frame.setVisible(true);
	}
}
