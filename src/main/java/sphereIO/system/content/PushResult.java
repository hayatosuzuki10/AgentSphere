package sphereIO.system.content;

import java.io.Serializable;

import sphereIO.system.FileContent;

public class PushResult implements FileContent,Serializable{
	public int result;
	public PushResult(int result) {
		this.result=result;
	}
}
