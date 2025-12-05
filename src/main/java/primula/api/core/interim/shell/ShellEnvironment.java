/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.shell;

import java.io.File;

/**
 *
 * @author sumikof
 */
public class ShellEnvironment {

    private boolean stop = false;
    private String directory;

    public ShellEnvironment() {
        File file = new File("");
        this.directory = file.getAbsolutePath();
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
