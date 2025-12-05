package sphereIO.system;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import sphereIO.SphereFile;
import sphereIO.SphereFileDescriptor;
import sphereIO.system.misc.SphereFileDescriptorAccess;
import sphereIO.system.misc.SphereSharedSecrets;

/**
 *
 * @author RanPork
 *
 */
public class SphereFileCache implements Serializable {
	private SphereFile path;
	private FileInfo fi;
	private SphereFileSystem sfs;
	private static SphereFileDescriptorAccess access = SphereSharedSecrets.getSphereFileDescriptorAccess();
	/*
	 * ファイルのキャッシュブロックを 先頭からのオフセット/ブロックサイズの値(インデックス)をKeyとして格納します
	 */
	HashMap<Integer, FileChunk> data;

	public SphereFileCache(SphereFile path) throws IOException {
		this.path = path;
		this.sfs = (SphereFileSystem) SingletonS2ContainerFactory.getContainer().getComponent("SphereFileSystem");
		this.fi = sfs.getFileInfo(path);
		this.data = new HashMap<Integer, FileChunk>();
	}

	/**
	 * インデックスを指定してキャッシュブロックを取り出します
	 * <p>
	 * キャッシュが存在すればそのブロックを取得し、存在しなければファイルシステムよりキャッシュを取得します
	 * またキャッシュが最新の状態であるかも確認し、そうでなければ更新します。
	 *
	 * @param index
	 * @return
	 * @throws IOException
	 */
	public FileChunk getdata(SphereFileDescriptor fd, int index) throws IOException {
		if (!data.containsKey(index) && access.isRead(fd)) {
			// 持ってないかつ記述子が読み込み可
			FileChunk chunk = IOUtil.loadData(fd, index);
			data.put(index, chunk);
			return chunk;
		} else {
			// もってる
			if (fi == null) {
				SphereFileSystem SFS = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
						.getComponent("SphereFileSystem");
				fi = SFS.getFileInfo(path);
			}
			FileChunk fc = data.get(index);
			if (access.isRead(fd)) {
				if (fc.lastModified.isBefore(fi.lastModified.get(index))) {
					// 更新アリ
					FileChunk chunk = IOUtil.loadData(fd, index);
					data.put(index, chunk);
					return chunk;
				} else if (fc.offset > 0 || fc.length % FileChunk.CHUNKSIZE != fi.filesize % FileChunk.CHUNKSIZE) {
					// キャッシュにあるデータが中途半端な書き込みである時
					FileChunk chunk = IOUtil.loadData(fd, index);
					ByteBuffer buf = ByteBuffer.wrap(chunk.data, 0, fc.offset - 1);
					buf.get(fc.data, 0, fc.offset - 1);
					buf = ByteBuffer.wrap(chunk.data, fc.length + 1, chunk.length);
					buf.get(fc.data, fc.length + 1, chunk.length);
				}
				return fc;
			} else {
				return fc;
			}
		}

	}

	/**
	 * 与えられた記述子が指すファイル実体に更新されているキャッシュ内のデータをすべて書き出します
	 *
	 * @param fd
	 * @throws IOException
	 */
	public void postAllData(SphereFileDescriptor fd) throws IOException {
		data.keySet().stream()
				.filter(a -> !fi.lastModified.containsKey(a) || data.get(a).lastModified.isAfter(fi.lastModified.get(a)))
				.forEach(a -> {
					try {
						IOUtil.postData(fd, a, data.get(a));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}

	/**
	 * 与えられた記述子が指すファイル実体のデータをすべてキャッシュします
	 *
	 * @param fd
	 * @throws IOException
	 */
	public void loadAllData(SphereFileDescriptor fd) throws IOException {
		for (int i = 0; i < (fi.filesize / FileChunk.CHUNKSIZE) + 1; i++) {
			data.put(i, IOUtil.loadData(fd, i));
		}
	}

	/*
	 * private void update() throws IOException { SphereFileSystem SFS =
	 * (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
	 * .getComponent("SphereFileSystem"); FileInfo fi = SFS.getFileInfo(path); }
	 */
}
