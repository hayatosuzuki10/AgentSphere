/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 便利ツール
 * @author yamamoto
 */
public class AgentUtil {

    /**
     * 簡単なディープコピーを提供します
     * @param <T>
     * @param t
     * @return
     */
    public static synchronized <T> T easyDeepCopy(final T t) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(arrayOutputStream);
        objectOutputStream.writeObject(t);
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(arrayOutputStream.toByteArray()));
        return (T) objectInputStream.readObject();
    }
}
