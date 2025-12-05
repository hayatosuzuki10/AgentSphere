/**
 *
 */
package primula.api.core.agent.loader.multiloader;

import java.util.HashMap;

/**
 * 入れ子構造化される事を考慮し、検索用のメソッドが存在するインターフェースです。
 *
 * @author Mr.RED
 *
 */
public interface SelectableClassLoader {

	/**
	 * バイナリ表現マップなどのキャッシュなどに該当クラスが存在するかどうかを返します。
	 * @param classname
	 * @return ロード済み、コードベース下、バイナリキャッシュに存在する場合true
	 */
	abstract public boolean isKnown(String classname);

	abstract public Class<?> loadClass(String name) throws ClassNotFoundException;

	/**
	 * ローダの保持するクラスのバイナリ表現マップを返します。
	 * @return 正準名とバイナリのHashMap
	 */
	abstract public HashMap<String, byte[]> getBinaryMap();

	/**
	 * ローダに関連付けられたセレクタを返します。
	 * @return ローダに関連するセレクタ
	 */
	abstract public ClassLoaderSelector getClassLoaderSelector();

	/**
	 * ローダにセレクタを関連付けます。
	 * @param selector
	 */
	abstract public void setClassLoaderSelector(ClassLoaderSelector selector);

	/**
	 * ローダの階層レベルを返します
	 * @return ローダの階層レベル
	 */
	abstract public int getLevel();
}