package sphereIO.system.content;

import java.io.Serializable;

import sphereIO.SphereFileDescriptor;
import sphereIO.system.FileContent;

public class PushArg implements Serializable, FileContent {
	public SphereFileDescriptor fd;
	public byte[] data;
	public long pos;
	public PushArg(SphereFileDescriptor fd,byte[] data,long pos) {
		this.fd=fd;
		this.data=data;
		this.pos=pos;
	}
}
