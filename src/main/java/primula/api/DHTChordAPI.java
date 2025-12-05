package primula.api;

import java.io.Serializable;
import java.util.List;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.api.core.network.SystemDHT;

/**
 * AgentSphereのネットワークで分散管理されるDHTにアクセスするAPI<br>
 * keyの衝突を避けるためにさらにほかのクラスでラッピングしておくことをお勧めします<br>
 * 例としてスケジューラ機能での使い方を関連事項にのせときます
 *
 * @see scheduler2022.util.DHTutil
 */
public class DHTChordAPI implements Serializable {

	private static SystemDHT dht;
	static {//ここでDHTを呼び出してるので新しくDHTを作成したらここを書き換えること
		if (dht == null) {
			dht = (SystemDHT) SingletonS2ContainerFactory.getContainer()
					.getComponent("ChordManeger");
		}
	}

	public synchronized static void put(String key, Serializable object) {
		if(key==null||object==null) {
			throw new NullPointerException("ｶﾞｯ");
		}
		try {
			dht.put(key, object);
		} catch (NullPointerException e) {
			throw new UnsupportedOperationException("DHTがモジュールとして登録されていません");
		}
	}

	public synchronized static Serializable get(String key) {
		if(key==null) {
			throw new NullPointerException("ｶﾞｯ");
		}
		try {
			return dht.get(key);
		} catch (NullPointerException e) {
			throw new UnsupportedOperationException("DHTがモジュールとして登録されていません");
		}

	}

	public synchronized static void remove(String key) {
		if(key==null) {
			throw new NullPointerException("ｶﾞｯ");
		}
		try {
			dht.remove(key);
		} catch (NullPointerException e) {
			throw new UnsupportedOperationException("DHTがモジュールとして登録されていません");
		}
	}

	public synchronized static boolean contains(String key) {
		if(key==null) {
			throw new NullPointerException("ｶﾞｯ");
		}
		try {
			return dht.contains(key);
		} catch (NullPointerException e) {
			throw new UnsupportedOperationException("DHTがモジュールとして登録されていません");
		}
	}

	public synchronized static List<String> listAll() {
		try {
			return dht.listAll();
		} catch (NullPointerException e) {
			throw new UnsupportedOperationException("DHTがモジュールとして登録されていません");
		}
	}

}
