package primula.api.core.assh;

import java.awt.HeadlessException;
import java.io.PrintStream;

import javax.swing.JTextArea;

public class OutputTextArea extends JTextArea{
	private TextAreaOutputStream out;

	public OutputTextArea() throws HeadlessException{
		super();
		this.setEditable(false);
		out=new TextAreaOutputStream(this);
	}
	
	public void setToSystemOut(){
	    System.setOut(new PrintStream(this.getOut()));
	  }

	  public void setToSystemErr(){
	    System.setErr(new PrintStream(this.getOut()));
	  }

	  public TextAreaOutputStream getOut() {
	    return out;
	  }
	
	public void flush() {
		this.append(out.toString());
		out.reset();

	}


}

