package sphereIO.system.content;

import java.io.Serializable;

import sphereIO.SphereFileDescriptor;
import sphereIO.system.FileContent;

public class CloseArg implements FileContent,Serializable{
	public SphereFileDescriptor fd;
	public CloseArg(SphereFileDescriptor fd) {
		this.fd=fd;
	}
}
