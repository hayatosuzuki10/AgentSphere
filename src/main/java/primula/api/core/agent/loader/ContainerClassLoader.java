package primula.api.core.agent.loader;

/**
 * クラスバイナリデータのコンテナを元にクラスデータのロードを行うクラスです。 シングルトンを叩き込んでよろしいものか疑問ですが…。
 * 
 * @author AK
 * 
 */
public class ContainerClassLoader extends ClassLoader {
	private static ContainerClassLoader uniqInst = null;
	private ClassDataCollectionContainer container = null;

	/**
	 * 
	 */
	private ContainerClassLoader() {
		;
	}

	/**
	 * @param parent
	 */
	private ContainerClassLoader(ClassLoader parent) {
		super(parent);
	}

	/**
	 * インスタンスを獲得します。
	 * 
	 * @return ユニークインスタンス
	 */
	public static synchronized ContainerClassLoader getInstance() {
		if (uniqInst == null) {
			uniqInst = new ContainerClassLoader();
			uniqInst.container = new ClassDataCollectionContainer();
		}
		return uniqInst;
	}

	/**
	 * 暫定実装です。ユニークインスタンスをnullにして、ガベージコレクションを呼びます。<br>
	 * #getInstance()でインスタンスを獲得していた全てのクラスが影響を受けます。
	 */
	public static void unload() {
		uniqInst = null;
		System.gc();
	}

	/**
	 * コンテナの要素をマージします。ファイルシステム側とレシーバ側が用います。
	 * 
	 * @param container
	 *            生成済みClassDataCollectionContainer
	 */
	public void setContainer(ClassDataCollectionContainer container) {
		this.container.merge(container);
	}

	/**
	 * @return クラスローダが用いるコンテナがセットされていればtrue、 そうでなければfalseを返します。
	 */
	public boolean isEffective() {
		if (container.size() == 0)
			return false;
		else
			return true;
	}

	/**
	 * スケジューラ、レシーバ向けメソッド。クラスローダの持つコンテナを取得します。
	 * 
	 * @return
	 */
	public ClassDataCollectionContainer getContainer() {
		return container;
	}

        @Override
	public Class<?> loadClass(String name, boolean resolve) {

		// すでにロードされていればそれを返す
		Class<?> c = findLoadedClass(name);

		try {
			// システムクラスにある場合はそれを返す
			return findSystemClass(name);
		} catch (ClassNotFoundException e) {
			// e.printStackTrace();
		}
		if (c == null) {
			byte[] data = container.getBinaryData(name);

			// Classの定義
			c = defineClass(name, data, 0, data.length);
		}
		if (resolve) {
			resolveClass(c);
		}
		return c;
	}
}
