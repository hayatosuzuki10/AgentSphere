/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package primula.api.core.interim.shell.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sumiya
 */
public class DirectoryOperator {
     File objFile;
    public String getPath(String current,List<String>args){
        if(1>args.size())
            return current;
        /*
         * \で文字を切りだす
         */
        ArrayList<String> arg = new ArrayList<String>();
        for (String s : args.get(0).split("(\\\\)|(/)")) {
            arg.add(s);
        }
        String tmp = current;
        if(tmp.charAt(tmp.length()-1)=='\\'){
            tmp=tmp.substring(0, tmp.length()-1);
        }
        /*
         * 相対パスから絶対パスを作る
         */
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
        /*
         * ディレクトリの存在確認
         */
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
