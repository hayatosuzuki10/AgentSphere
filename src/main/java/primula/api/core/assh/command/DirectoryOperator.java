/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.assh.command;

import java.io.File;
import java.util.ArrayList;


public class DirectoryOperator {
    File objFile;
    public String getPath(String current, String args){
        if(args == null)
            return current;
        ArrayList<String> arg = new ArrayList<String>();
        for (String s : args.split("(\\\\)|(/)")) {
            arg.add(s);
        }
        String tmp = current;
        if(tmp.charAt(tmp.length()-1)=='\\'){
            tmp=tmp.substring(0, tmp.length()-1);
        }
        for (String s : arg) {
            if (s.equals(".")) {
                ;
            } else if (s.equals("..")) {
                objFile = new File(tmp);
                tmp = objFile.getParent();
            } else {
                tmp = tmp + "\\" + s;
            }
        }
        return tmp;
    }

    public boolean exist(String s) {
        objFile = new File(s);

        if (objFile.exists()) {
            if (objFile.isDirectory()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
