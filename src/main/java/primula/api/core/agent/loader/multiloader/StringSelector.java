/**
 * test
 */
package primula.api.core.agent.loader.multiloader;

import java.io.Serializable;

/**
 * クラスローダのセレクタとして文字列だけで判別する用。暫定実装用。
 * HashMapのキーとしてドウゾ。
 *
 * @author Mr.RED
 *
 */
public class StringSelector implements ClassLoaderSelector, Serializable{
	/**
	 *
	 */
	private static final long	serialVersionUID	= -2187620894672380277L;
	private String entry;

	public StringSelector(String entry){
		this.entry = entry;
	}

	@Override
	public int hashCode() {
		return entry.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		else if(obj instanceof StringSelector){
			return entry.equals(((StringSelector)obj).entry);
		}else{
			return false;
		}
	}

	@Override
	public String toString() {
		return entry;
	}
}
