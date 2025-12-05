package sphereIO.system;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import sphereIO.SphereFileDescriptor;
import sphereIO.system.content.FetchArg;
import sphereIO.system.content.FetchResult;
import sphereIO.system.content.PushArg;
import sphereIO.system.content.PushResult;
import sphereIO.system.misc.SphereFileDescriptorAccess;
import sphereIO.system.misc.SphereSharedSecrets;

public class IOUtil {
	private IOUtil() {
	}

	private static SphereFileSystem sfs = (SphereFileSystem) SingletonS2ContainerFactory.getContainer()
			.getComponent("SphereFileSystem");
	private static SphereFileDescriptorAccess access = SphereSharedSecrets.getSphereFileDescriptorAccess();

	/*
	 * channel関連
	 */
	public static int fetch(SphereFileDescriptor fd, ByteBuffer buf) throws IOException {
		if (access.getPath(fd).getHostIP().equals(IPAddress.myIPAddress)) {
			// ろーかる
			ReentrantReadWriteLock lock = sfs.getFileLock(access.getPath(fd));
			lock.readLock().lock();
			try {
				SphereFileCache cache = sfs.getCache(access.getPath(fd));
				// System.out.println(sfs+" "+fd);
				long pos = sfs.getDescPos(fd);
				int count = 0;
				int index = (int) (pos / FileChunk.CHUNKSIZE);
				int offset = (int) (pos % FileChunk.CHUNKSIZE);
				while (buf.hasRemaining()) {
					FileChunk fc = cache.getdata(fd, index);
					int length = Math.min(buf.remaining(), Math.min(fc.length - offset, FileChunk.CHUNKSIZE - offset));
					buf.put(fc.data, offset, length);
					count += length;

					if (fc.length < FileChunk.CHUNKSIZE) {
						// キャッシュブロック終端
						break;
					}

					index++;
					offset = 0;
				}
				sfs.setDescPos(fd, pos + count);
				if (pos + count >= sfs.getFileInfo(fd).filesize) {
					System.out.println("しゅうたん:" + (pos + count) + "\n" + sfs.getFileInfo(fd));
					return -1;
				}
				return count;
			} finally {
				lock.readLock().unlock();
			}
		} else {
			// 外部AgentSphere
			int rem = buf.remaining();
			if (rem == 0) {
				return 0;
			}
			UUID session = UUID.randomUUID();
			FetchArg arg = new FetchArg(fd, -1, rem);
			FileTask ft = new FileTask();
			KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
					InetAddress.getByName(access.getPath(fd).getHostIP()), 55878);
			SphereFileContentContainer content = new SphereFileContentContainer(session, arg, sfs.getStrictName(),
					IPAddress.myIPAddress, FileContentType.READ, FileContentType.DATA);
			StandardEnvelope env = new StandardEnvelope(new AgentAddress(sfs.getClass().getName()), content);
			sfs.setTask(session, ft);
			MessageAPI.send(dst, env);
			try {
				// System.out.println(session);//でばっぐ用
				FetchResult fr = (FetchResult) ft.get().getData();
				buf.put(fr.getData());
				if (fr.isEof()) {
					return -1;
				} else {
					return fr.getData().length;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				sfs.removeTask(session);
			}
		}

		return -1;
	}

	public static int fetch(SphereFileDescriptor fd, ByteBuffer buf, long position) throws IOException {
		if (access.getPath(fd).getHostIP().equals(IPAddress.myIPAddress)) {
			// ろーかる
			ReentrantReadWriteLock lock = sfs.getFileLock(access.getPath(fd));
			lock.readLock().lock();
			try {
				SphereFileCache cache = sfs.getCache(access.getPath(fd));
				int count = 0;
				int index = (int) (position / FileChunk.CHUNKSIZE);
				int offset = (int) (position % FileChunk.CHUNKSIZE);
				while (buf.hasRemaining()) {
					FileChunk fc = cache.getdata(fd, index);
					if (fc.length - offset < 0)// これに引っかかるのはposの指定がひどいときだけ
						break;
					int length = Math.min(buf.remaining(), Math.min(fc.length - offset, FileChunk.CHUNKSIZE - offset));
					buf.put(fc.data, offset, length);
					count += length;

					if (fc.length < FileChunk.CHUNKSIZE) {
						break;
					}

					index++;
					offset = 0;
				}
				if (count > 0)
					return count;
				return -1;
			} finally {
				lock.readLock().unlock();
			}
		} else {
			// 外部AgentSphere
			int rem = buf.remaining();
			if (rem == 0) {
				return -1;
			}
			UUID session = UUID.randomUUID();
			FetchArg arg = new FetchArg(fd, position, rem);
			FileTask ft = new FileTask();
			KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
					InetAddress.getByName(access.getPath(fd).getHostIP()), 55878);
			SphereFileContentContainer content = new SphereFileContentContainer(session, arg, sfs.getStrictName(),
					IPAddress.myIPAddress, FileContentType.READ, FileContentType.DATA);
			StandardEnvelope env = new StandardEnvelope(new AgentAddress(sfs.getClass().getName()), content);
			sfs.setTask(session, ft);
			MessageAPI.send(dst, env);
			try {
				// System.out.println(session);//でばっぐ用
				FetchResult fr = (FetchResult) ft.get().getData();
				buf.put(fr.getData());
				if (fr.isEof()) {
					return -1;
				} else {
					return fr.getData().length;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				sfs.removeTask(session);
			}
		}
		return -1;
	}

	public static int push(SphereFileDescriptor fd, ByteBuffer buf) throws IOException {
		//System.out.println(access.getPath(fd));

		if (access.getPath(fd).getHostIP().equals(IPAddress.myIPAddress)) {
			// ろーかる
			ReentrantReadWriteLock lock = sfs.getFileLock(access.getPath(fd));
			lock.writeLock().lock();
			try {
				SphereFileCache cache = sfs.getCache(access.getPath(fd));
				long pos = sfs.getDescPos(fd);
				int count = 0;
				int index = (int) (pos / FileChunk.CHUNKSIZE);
				int offset = (int) (pos % FileChunk.CHUNKSIZE);
				while (buf.hasRemaining()) {
					FileChunk fc = cache.getdata(fd, index);
					if (fc == null) {
						fc = new FileChunk(access.getPath(fd), index, new byte[FileChunk.CHUNKSIZE], offset, 0, null);
						cache.data.put(index, fc);
					} else if (fc.length < offset || offset + buf.remaining() < fc.offset) {
						// チャンク内で有効部分が分断される場合 前のやつは書き込んで退避させる
						postData(fd, index, fc);
						fc = new FileChunk(access.getPath(fd), index, new byte[FileChunk.CHUNKSIZE], offset, 0, null);
						cache.data.put(index, fc);
					}
					int length = Math.min(buf.remaining(), FileChunk.CHUNKSIZE - offset);
					buf.get(fc.data, offset, length);
					count += length;
					fc.length = Math.max(fc.length, length);
					fc.offset = Math.min(offset, fc.offset);
					fc.lastModified = Instant.now();
					/*
					 * System.out.println("push"); for (int i = fc.offset; i < fc.offset+fc.length;
					 * i++) { byte b = fc.data[i]; System.out.println(b); }
					 */
					index++;
					offset = 0;
				}
				sfs.setDescPos(fd, pos + count);
				return count;
			} finally {
				lock.writeLock().unlock();
			}
		} else {
			// 外部AgentSphere
			int rem = buf.remaining();
			if (rem == 0) {
				return 0;
			}
			byte[] data = new byte[rem];
			buf.get(data);
			UUID session = UUID.randomUUID();
			PushArg arg = new PushArg(fd, data, -1);
			FileTask ft = new FileTask();
			KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
					InetAddress.getByName(access.getPath(fd).getHostIP()), 55878);
			SphereFileContentContainer content = new SphereFileContentContainer(session, arg, sfs.getStrictName(),
					IPAddress.myIPAddress, FileContentType.WRITE, FileContentType.DATA);
			StandardEnvelope env = new StandardEnvelope(new AgentAddress(sfs.getClass().getName()), content);
			sfs.setTask(session, ft);
			MessageAPI.send(dst, env);
			try {
				// System.out.println(session);//でばっぐ用
				PushResult fr = (PushResult) ft.get().getData();
				return (int) fr.result;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				sfs.removeTask(session);
			}
		}
		return 0;
	}

	public static int push(SphereFileDescriptor fd, ByteBuffer buf, long pos) throws IOException {
		if (access.getPath(fd).getHostIP().equals(IPAddress.myIPAddress)) {
			// ろーかる
			ReentrantReadWriteLock lock = sfs.getFileLock(access.getPath(fd));
			lock.writeLock().lock();
			try {
				SphereFileCache cache = sfs.getCache(access.getPath(fd));
				// System.out.println(sfs+" "+fd);
				int count = 0;
				int index = (int) (pos / FileChunk.CHUNKSIZE);
				int offset = (int) (pos % FileChunk.CHUNKSIZE);
				while (buf.hasRemaining()) {
					FileChunk fc = cache.getdata(fd, index);
					if (fc == null) {
						fc = new FileChunk(access.getPath(fd), index, new byte[FileChunk.CHUNKSIZE], offset, 0, null);
						cache.data.put(index, fc);
					} else if (fc.length < offset || offset + buf.remaining() < fc.offset) {
						// チャンク内で有効部分が分断される場合 前のやつは書き込んで退避させる もっといい方法ないかなあ
						postData(fd, index, fc);
						fc = new FileChunk(access.getPath(fd), index, new byte[FileChunk.CHUNKSIZE], offset, 0, null);
						cache.data.put(index, fc);
					}
					int length = Math.min(buf.remaining(), FileChunk.CHUNKSIZE - offset);
					buf.get(fc.data, offset, length);
					count += length;
					fc.length = Math.max(fc.length, length);
					fc.offset = Math.min(offset, fc.offset);
					fc.lastModified = Instant.now();

					/*
					 * System.out.println("push"); for (int i = fc.offset; i < fc.offset+fc.length;
					 * i++) { byte b = fc.data[i]; System.out.println(b); }
					 */
					index++;
					offset = 0;
				}
				return count;
			} finally {
				lock.writeLock().unlock();
			}
		} else {
			// 外部AgentSphere
			int rem = buf.remaining();
			if (rem == 0) {
				return 0;
			}
			byte[] data = new byte[rem];
			buf.get(data);
			UUID session = UUID.randomUUID();
			PushArg arg = new PushArg(fd, data, pos);
			FileTask ft = new FileTask();
			KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
					InetAddress.getByName(access.getPath(fd).getHostIP()), 55878);
			SphereFileContentContainer content = new SphereFileContentContainer(session, arg, sfs.getStrictName(),
					IPAddress.myIPAddress, FileContentType.WRITE, FileContentType.DATA);
			StandardEnvelope env = new StandardEnvelope(new AgentAddress(sfs.getClass().getName()), content);
			sfs.setTask(session, ft);
			MessageAPI.send(dst, env);
			try {
				// System.out.println(session);//でばっぐ用
				PushResult fr = (PushResult) ft.get().getData();
				return (int) fr.result;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				sfs.removeTask(session);
			}
		}
		return 0;
	}

	/*
	 * cache関連 それぞれファイルからキャッシュ断片の読み込みと キャッシュ断片をファイルへ書き込む動作を行う
	 */
	public static FileChunk loadData(SphereFileDescriptor fd, int index) throws IOException {
		if (access.getPath(fd).getHostIP().equals(IPAddress.myIPAddress)) {
			// ろーかる
			Lock lock = sfs.getFileLock(access.getPath(fd)).readLock();
			lock.lock();
			try {
				byte[] data = new byte[FileChunk.CHUNKSIZE];
				ByteBuffer buf = ByteBuffer.wrap(data);
				FileChannel channel = sfs.getChannel(fd);
				int size = channel.read(buf, index * FileChunk.CHUNKSIZE);
				return new FileChunk(access.getPath(fd), index, data, size,
						sfs.getFileInfo(fd).lastModified.getOrDefault(index, Instant.now()));
			} finally {
				lock.unlock();
			}
		}
		throw new IllegalArgumentException("お外のファイルは無理なう");
	}

	public static boolean postData(SphereFileDescriptor fd, int index, FileChunk fc) throws IOException {
		if (access.getPath(fd).getHostIP().equals(IPAddress.myIPAddress)) {
			// ろーかる
			Lock lock = sfs.getFileLock(access.getPath(fd)).writeLock();
			lock.lock();
			try {
				ByteBuffer buf = ByteBuffer.wrap(fc.data, fc.offset, fc.length);
				FileChannel channel = sfs.getChannel(fd);
				/*
				 * System.out.println("IOUtil#postData"); System.out.println(fc.offset + " " +
				 * fc.length);
				 *
				 * System.out.println(index * FileChunk.CHUNKSIZE + fc.offset);
				 * System.out.println(buf.remaining()); for (int i = fc.offset; i <
				 * fc.offset+fc.length; i++) { System.out.println(fc.data[i]); }
				 */
				int size = channel.write(buf, index * FileChunk.CHUNKSIZE + fc.offset);
				sfs.getFileInfo(fd).lastModified.put(index, fc.lastModified);
				return size >= 0;
			} finally {
				lock.unlock();
			}
		}
		throw new IllegalArgumentException("お外のファイルは無理なう");
	}
}
