package primula.api.core.assh;

import java.io.ByteArrayOutputStream;

public class TextAreaOutputStream extends ByteArrayOutputStream{
	private OutputTextArea textArea;

	public TextAreaOutputStream(OutputTextArea textArea) {
		super();
		this.textArea=textArea;
	}

	public synchronized void write(byte[] b,int off,int len){
		super.write(b,off,len);
		textArea.flush();
	}
}
