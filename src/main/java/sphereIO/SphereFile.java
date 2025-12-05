package sphereIO;

import java.io.Serializable;
import java.nio.file.Paths;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.api.core.network.SystemDHT;
import sphereIO.system.FileContent;

public class SphereFile implements Serializable, FileContent {

	private String path;
	private String hostIP;
	private static SystemDHT dht = (SystemDHT) SingletonS2ContainerFactory.getContainer()
			.getComponent("ChordManeger");

	/**
	 * 無効なSphereFileの作成
	 */
	public SphereFile() {
	}

	/**
	 * 与えられたパス文字列より、そのファイルが存在するマシンのIPをセットして初期化します
	 * <p>
	 * SystemDHTをデータソースとしてIPをセットします
	 * @param path
	 */
	public SphereFile(String path) {
		if (path == null)
			throw new NullPointerException();
		this.path = Paths.get(path).normalize().toString();
		hostIP = (String) dht.get(this.path);
	}

	/**
	 * 与えられたパス文字列とIPをセットして初期化します
	 * <p>
	 * この操作ではこのSphereFileが表すファイルが存在するかのチェックは行いません
	 * @param path
	 * @param IP
	 */
	public SphereFile(String path, String IP) {
		this.path = Paths.get(path).normalize().toString();
		this.hostIP = IP;
	}

	public String getPath() {
		return path;
	}

	public String getHostIP() {
		return hostIP;
	}

	/**
	 * pathとhostIPを比較して同一ならtrue
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SphereFile) {
			SphereFile tmp = (SphereFile) obj;
			return this.hostIP.equals(tmp.hostIP) && this.path.equals(tmp.path);
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.path);
		builder.append(":");
		builder.append(this.hostIP);
		return builder.toString();
	}

	/**
	 * ざつ
	 * <p>
	 * ちょーざつ
	 * <p>
	 * パスとマシンIP文字列のhashcodeを足しただけの雑さ
	 * @Override
	 */
	public int hashCode() {
		// TODO 自動生成されたメソッド・スタブ
		int hash = 0;
		if (path != null)
			hash += path.hashCode();
		if (hostIP != null) {
			hash += hostIP.hashCode();
		}
		return hash;
	}
}
