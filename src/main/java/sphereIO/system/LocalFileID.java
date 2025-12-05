package sphereIO.system;

import java.io.IOException;
import java.util.UUID;

import primula.util.IPAddress;
import sphereIO.SphereFile;
import sphereIO.util.SystemUtility;

public class LocalFileID {
	UUID ID;
	SphereFile path;
	Object fileKey;

	public LocalFileID(UUID uuid, SphereFile path) {
		this.ID = uuid;
		this.path = path;
		if (path.getHostIP().equals(IPAddress.myIPAddress)) {
			try {
				this.fileKey = SystemUtility.getFileKey(path.getPath());
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				this.fileKey = null;
				e.printStackTrace();
			}
		} else {
			this.fileKey = null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LocalFileID) {
			LocalFileID tmpFileID = (LocalFileID) obj;
			return this.ID.equals(tmpFileID.ID)&&this.fileKey.equals(tmpFileID.fileKey);
		}
		return false;
	}
}
