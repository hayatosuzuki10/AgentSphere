package sphereIO.system.content;

import java.io.Serializable;
import java.nio.file.OpenOption;

import sphereIO.SphereFile;
import sphereIO.system.FileContent;

public class OpenArg implements FileContent, Serializable {
	SphereFile path;
	OpenOption[] opt;

	public OpenArg(SphereFile path, OpenOption... opt) {
		this.path = path;
		this.opt = opt;
	}

	public SphereFile getPath() {
		return path;
	}

	public OpenOption[] getOpt() {
		return opt;
	}
}
