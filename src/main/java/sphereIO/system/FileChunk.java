package sphereIO.system;

import java.io.Serializable;
import java.time.Instant;

import sphereIO.SphereFile;

public class FileChunk implements FileContent, Serializable {
	/**
	 * <pre>
	 * キャッシュブロック一つあたりのサイズ
	 * つまりキャッシュはこのサイズを1単位として拾うようにする
	 * </pre>
	 */
	static final int CHUNKSIZE = 8192;

	SphereFile path;
	int start;
	byte[] data;
	int offset;//=0
	int length;//=0 クラス内フィールドのデータ型は初期値を書かずとも暗黙の初期化が行われる
	Instant lastModified;

	/*
	 * 直列化用の空のファイルチャンク
	 */
	public FileChunk() {

	}

	public FileChunk(SphereFile path, int start, byte[] data, int length, Instant lastModified) {
		if (data.length > CHUNKSIZE) {
			throw new IllegalArgumentException("wwwwwww.oﾟ(^∀^)ﾟo.。データ長過ぎ！");
		}
		if (length > CHUNKSIZE) {
			throw new IllegalArgumentException("wwwwwww.oﾟ(^∀^)ﾟo.。オフセット長過ぎ！"+"offset:"+offset+" limit:"+length);
		}
		this.path = path;
		this.start = start;
		this.data = data;
		if (length > 0)//ファイルサイズがチャンクサイズの倍数だとありうるので負数はゼロに
			this.length = length;
		this.lastModified = lastModified;

	}
	public FileChunk(SphereFile path, int start, byte[] data, int offset,int length, Instant lastModified) {
		if (data.length > CHUNKSIZE) {
			throw new IllegalArgumentException("wwwwwww.oﾟ(^∀^)ﾟo.。データ長過ぎ！"+"offset:"+offset+" limit:"+length);
		}
		if (offset+length > CHUNKSIZE) {
			throw new IllegalArgumentException("wwwwwww.oﾟ(^∀^)ﾟo.。オフセット長過ぎ！"+"offset:"+offset+" limit:"+length);
		}
		this.path = path;
		this.start = start;
		this.data = data;
		this.offset=offset;
		if (length > 0)//ファイルサイズがチャンクサイズの倍数だとありうるので負数はゼロに
			this.length = length;
		this.lastModified = lastModified;

	}
}
