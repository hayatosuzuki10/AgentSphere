package primula.api.core.assh;

import java.io.File;

public class ShellEnvironment {
	private boolean stop = false;
    private String directory;
    private static ShellEnvironment shellEnv;

    public ShellEnvironment() {
        File file = new File("");
        this.directory = file.getAbsolutePath();
    }

    public static ShellEnvironment getInstance() {
    	if(shellEnv == null) {
    		shellEnv = new ShellEnvironment();
    	}
    	return shellEnv;
    }

    public String getInputLineText() {
        return directory + ">";
    }

    /**
     * @return the stop
     */
    public boolean isStop() {
        return stop;
    }

    /**
     * @param isStoped the stop to set
     */
    public void setStop() {
        this.stop = true;
    }

    /**
     * @return the directory
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @param directory the directory to set
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
