/**
 *
 */
package primula.api.core.agent.loader.UnknownObjectStream;

import java.io.Serializable;
import java.util.HashMap;

import primula.api.core.agent.loader.multiloader.ClassLoaderSelector;
import primula.api.core.agent.loader.multiloader.SelectableClassLoader;


/**
 * クラスローダの持つデータとセレクタをそのまま送る為のデータパック。
 *
 * @author Mr.RED
 *
 */
public class ClassLoaderPack implements Serializable {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 3301027140277481869L;
	private ClassLoaderSelector selector;
	private HashMap<String, byte[]> binarymap;
	private int level;

	public ClassLoaderPack(ClassLoaderSelector selector, HashMap<String, byte[]> binarymap, int level) {
		this.selector = selector;
		this.binarymap = binarymap;
		this.level = level;
	}

	public ClassLoaderPack(SelectableClassLoader loader) {
		this.selector = loader.getClassLoaderSelector();
		this.binarymap = loader.getBinaryMap();
		this.level = loader.getLevel();
	}

	public ClassLoaderSelector getSelector(){
		return selector;
	}

	public HashMap<String, byte[]> getBinaryMap(){
		return binarymap;
	}

	public int getLevel(){
		return level;
	}
}