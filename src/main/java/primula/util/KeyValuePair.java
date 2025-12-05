/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.util;

import java.io.Serializable;

/**
 *
 * @author yamamoto
 */
public class KeyValuePair<K extends Serializable, V extends Serializable> implements Serializable {

    private K key;
    private V value;

    public KeyValuePair() {
    }

    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return the key
     */
    public K getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(K key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public V getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(V value) {
        this.value = value;
    }
}
