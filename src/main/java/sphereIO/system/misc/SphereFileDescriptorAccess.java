package sphereIO.system.misc;

import java.io.IOException;
import java.util.UUID;

import sphereIO.SphereFile;
import sphereIO.SphereFileDescriptor;

public interface SphereFileDescriptorAccess {
	public void setPath(SphereFileDescriptor sfd, SphereFile id);

	public SphereFile getPath(SphereFileDescriptor sfd);

	public void setId(SphereFileDescriptor sdf, UUID id);

	public UUID getId(SphereFileDescriptor sdf);

	public void setAppend(SphereFileDescriptor sfd, boolean append);

	public boolean getAppend(SphereFileDescriptor sfd);

	public void close(SphereFileDescriptor sfd) throws IOException;

	public void setDeleteOnClose(SphereFileDescriptor sfd, boolean flag);

	public boolean isDeleteOnClose(SphereFileDescriptor sfd);

	public void setWrite(SphereFileDescriptor fd, boolean flag);

	public boolean isWrite(SphereFileDescriptor fd);

	public void setRead(SphereFileDescriptor fd, boolean flag);

	public boolean isRead(SphereFileDescriptor fd);

}
