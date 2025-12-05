
	import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.Timer;

import primula.agent.AbstractAgent;
	 
	public class backuptest extends AbstractAgent{
	 
	    private static int cnt;
	 
	    public void run() {
	  	 
	  new JFrame().setVisible(true);
	 
	  ActionListener actListner = new ActionListener() {
		  
	@Override
	 
	public void actionPerformed(ActionEvent event) {
		
	    cnt += 1;
	 
	    System.out.println("Counter = "+cnt);
	 
	}
	 
	  };
	  this.backup();
	  Timer timer = new Timer(500, actListner);
	 
	  timer.start();

	  System.out.println("////////Backed Up////////////");
	    }

		@Override
		public void requestStop() {
			// TODO Auto-generated method stub
			
		}
	}