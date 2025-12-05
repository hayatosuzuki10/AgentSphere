package primula.api.core.network;

import java.io.Serializable;
import java.util.List;

import primula.api.core.ICoreModule;

/**
 * AgentSphereのシステムに組み込まれているDHTを表すインターフェース<br>
 * 新しくDHTを実装するときにはこれを実装してもらいDHTChordAPIでの参照クラスを書き換えること
 *
 * @see primula.api.DHTChordAPI
 * @author Norito
 *
 */
public interface SystemDHT extends ICoreModule {
	void put(String key, Serializable value);

	Serializable get(String key);

	void remove(String key);

	List<String> listAll();

	boolean contains(String key);
}
