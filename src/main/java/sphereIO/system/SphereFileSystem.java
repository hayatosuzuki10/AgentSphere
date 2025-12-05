package sphereIO.system;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.api.MessageAPI;
import primula.api.core.ICoreModule;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.SystemDHT;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import sphereIO.FileType;
import sphereIO.SphereFile;
import sphereIO.SphereFileChannel;
import sphereIO.SphereFileDescriptor;
import sphereIO.system.content.CloseArg;
import sphereIO.system.content.FetchArg;
import sphereIO.system.content.FetchResult;
import sphereIO.system.content.OpenArg;
import sphereIO.system.content.PushArg;
import sphereIO.system.content.PushResult;
import sphereIO.system.misc.SphereFileDescriptorAccess;
import sphereIO.system.misc.SphereSharedSecrets;

/**
 * エージェントにマイグレートセーフなIOを提供するためのシステムです
 *
 * @author RanPork
 */
public class SphereFileSystem implements ICoreModule, IMessageListener {

	// メッセージング用ID
	private String systemID;

	// 管理しているファイル記述子
	private Set<SphereFileDescriptor> descriptorSet;
	private Map<SphereFileDescriptor, Long> descPositionMap;
	// ローカルでオープンしているファイルの記述子とチャンネルのぺあ
	private Map<SphereFileDescriptor, FileChannel> channelMap;
	// ローカルでオープンしているファイルの記述子とキャッシュのぺあ
	private Map<SphereFile, SphereFileCache> cacheMap;
	// ローカルのファイルのReadWriteLockを保持する連想配列
	private Map<SphereFile, ReentrantReadWriteLock> lockMap;

	// タスクIDとタスクのペアリング用連想配列
	private Map<UUID, FileTask> taskMap;
	// 見たことがあるファイルのInfo
	private Map<SphereFile, FileInfo> infoMap;
	private FileKeyTable fileKeyTable;

	private SystemDHT dht;

	public SphereFileSystem() {
	}

	@Override
	public void initializeCoreModele() {
		systemID = UUID.randomUUID().toString();
		// 各種テーブル類
		descriptorSet = Collections.synchronizedSet(new HashSet<>());
		descPositionMap = new ConcurrentHashMap<SphereFileDescriptor, Long>();
		taskMap = new ConcurrentHashMap<>();
		lockMap = new ConcurrentHashMap<>();
		channelMap = new ConcurrentHashMap<>();
		cacheMap = new ConcurrentHashMap<>();
		infoMap = new ConcurrentHashMap<>();
		fileKeyTable = new FileKeyTable();
		// 存在するファイルの登録
		dht = (SystemDHT) SingletonS2ContainerFactory.getContainer()
				.getComponent("ChordManeger");
		dht.initializeCoreModele();
		if (!Files.exists(Paths.get("share"))) {
			try {
				Files.createDirectory(Paths.get("share"));
			} catch (IOException e) {
				throw new RuntimeException("shareディレクトリ周り", e);
			}
		}
		try (Stream<Path> stream = Files.walk(Paths.get("share"))) {
			Path shareRoot = Paths.get("share");
			Set<InetAddress> myIP = sphereConnection.Util.getAllLocalHostAddresses();
			//stream.filter(a -> !a.equals(shareRoot)).forEach(a -> EDHT.setIP(a.toString(), IPAddress.myIPAddress));
			stream.filter(a -> !a.equals(shareRoot))
					.forEach(a -> myIP.stream()
							.filter(b -> b instanceof Inet4Address)
							.forEach(b -> dht.put(a.toString(), b.getHostAddress())));
		} catch (IOException e) {
			throw new RuntimeException("shareディレクトリ周り", e);
		}
		// メッセージングリスナーの登録
		try {
			MessageAPI.registerMessageListener(this);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.err.println("現在ネットワークファイル機能は無効です");
			return;
		}
		System.err.println("ファイル機能有効でござる");
	}

	public SystemDHT getDht() {
		return dht;
	}

	@Override
	public void finalizeCoreModule() {
	}

	/**
	 * このファイルシステムに割り当てられたシステム全体において固有のIDを返します
	 */
	public String getStrictName() {
		return systemID;
	}

	/**
	 * 単にクラス名を返すだけです
	 * <p>
	 * StrictNameおよびSimpleNameが何なのかを知りたい方は2010年の山本先輩の論文読むとよろっす
	 */
	public String getSimpleName() {
		return this.getClass().getName();
	}

	void setTask(UUID id, FileTask ft) {
		taskMap.put(id, ft);
	}

	void removeTask(UUID id) {
		taskMap.remove(id);
	}

	FileChannel getChannel(SphereFileDescriptor fd) {
		return channelMap.get(fd);
	}

	void setCache(SphereFile path, SphereFileCache cache) throws IOException {
		cacheMap.put(path, cache);
	}

	SphereFileCache getCache(SphereFile path) {
		return cacheMap.get(path);
	}

	boolean hasCache(SphereFile path) {
		return cacheMap.containsKey(path);
	}

	public void setDescPos(SphereFileDescriptor fd, long pos) {
		descPositionMap.put(fd, pos);
	}

	public long getDescPos(SphereFileDescriptor fd) {
		return descPositionMap.get(fd);
	}

	ReentrantReadWriteLock getFileLock(SphereFile path) {
		return lockMap.get(path);
	}

	/**
	 * メッセージを受け取った時に要求されたものを送り返したりとかするときのあれ
	 */
	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
		SphereFileContentContainer container = null;
		if (envelope.getContent() instanceof SphereFileContentContainer) {
			container = (SphereFileContentContainer) envelope.getContent();
			Set<FileContentType> tag = container.getTag();
			if (tag.contains(FileContentType.OPEN)) {
				// OPEN
				receiveOpenFile(container);
			} else if (tag.contains(FileContentType.CLOSE)) {
				// CLOSE
				//TODO
				receiveCloseFile(container);
			} else if (tag.contains(FileContentType.DATA)) {
				if (tag.contains(FileContentType.READ)) {
					// READ DATA
					receiveReadData(container);
				} else if (tag.contains(FileContentType.WRITE)) {
					// WRITE DATA
					receiveWriteData(container);
				}
			} else if (tag.contains(FileContentType.INFO)) {
				if (tag.contains(FileContentType.READ)) {
					// READ INFO
					receiveReadInfo(container);
				} else if (tag.contains(FileContentType.WRITE)) {
					// WRITE INFO

				}
			} else if (tag.contains(FileContentType.COMPLETE)) {
				// COMPLETE
				FileTask ft = taskMap.get(container.getSession());
				if (ft != null) {
					ft.set(container);
				}
				return;
			} else if (tag.contains(FileContentType.ERROR)) {
				// ERROR
				FileTask ft = taskMap.get(container.getSession());
				if (ft != null) {
					ft.set(container);
				}
				return;
			} else {
				// プロトコル満たしてない
				return;
			}
		} else {
			// SphereFileContentContainerじゃない
			return;
		}

	}

	private void receiveReadData(SphereFileContentContainer container) {
		// READ DATA
		FileContent content = container.getData();
		if (content instanceof FetchArg) {
			FetchArg arg = (FetchArg) content;
			try {
				KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
						InetAddress.getByName(container.getFrom()), 55878);
				try {
					ByteBuffer buf = ByteBuffer.allocate(arg.length);
					int count;
					if (arg.pos < 0) {
						// オフセット未指定
						count = IOUtil.fetch(arg.fd, buf);
					} else {
						count = IOUtil.fetch(arg.fd, buf, arg.pos);
					}
					buf.flip();
					byte[] data = new byte[buf.remaining()];
					buf.get(data);
					SphereFileContentContainer resultContainer = new SphereFileContentContainer(container.getSession(),
							new FetchResult(data, descPositionMap.get(arg.fd), count == -1), this.getStrictName(),
							IPAddress.myIPAddress, FileContentType.COMPLETE);
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							resultContainer);
					MessageAPI.send(dst, env);
				} catch (IOException e) {
					// IOえくせぷしょん
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							new SphereFileContentContainer(container.getSession(), null, this.getStrictName(),
									IPAddress.myIPAddress, FileContentType.ERROR));
					((SphereFileContentContainer) env.getContent()).setException(e);
					MessageAPI.send(dst, env);
				}
			} catch (UnknownHostException e) {
				// 不正なIP指定とか握りつぶしても...ばれへんか
				e.printStackTrace();
			}
		}
	}

	private void receiveWriteData(SphereFileContentContainer container) {
		FileContent content = container.getData();
		if (content instanceof PushArg) {
			PushArg arg = (PushArg) content;

			try {
				KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
						InetAddress.getByName(container.getFrom()), 55878);
				try {
					ByteBuffer buf = ByteBuffer.wrap(arg.data);
					for (int i = 0; i < arg.data.length; i++) {
						System.err.print(arg.data[i]);
					}
					System.err.println(buf.remaining());
					int count;
					if (arg.pos < 0) {
						// オフセット未指定
						count = IOUtil.push(arg.fd, buf);
						System.err.println("みしてい");
					} else {
						count = IOUtil.push(arg.fd, buf, arg.pos);
						System.err.println("してい");
					}
					SphereFileContentContainer resultContainer = new SphereFileContentContainer(container.getSession(),
							new PushResult(count), this.getStrictName(), IPAddress.myIPAddress,
							FileContentType.COMPLETE);
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							resultContainer);
					MessageAPI.send(dst, env);
				} catch (IOException e) {
					// IOえくせぷしょん
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							new SphereFileContentContainer(container.getSession(), null, this.getStrictName(),
									IPAddress.myIPAddress, FileContentType.ERROR));
					((SphereFileContentContainer) env.getContent()).setException(e);
					MessageAPI.send(dst, env);
				}
			} catch (UnknownHostException e) {
				// 不正なIP指定とか握りつぶしても...ばれへんか
				e.printStackTrace();
			}
		}
	}

	private void receiveReadInfo(SphereFileContentContainer container) {
		FileContent content = container.getData();
		if (content instanceof SphereFile) {
			SphereFile path = (SphereFile) content;
			KeyValuePair<InetAddress, Integer> pair;
			try {
				pair = new KeyValuePair<InetAddress, Integer>(InetAddress.getByName(container.getFrom()), 55878);
				try {
					FileInfo info = this.getFileInfo(path);
					SphereFileContentContainer resultContainer = new SphereFileContentContainer(container.getSession(),
							info, this.getStrictName(), IPAddress.myIPAddress, FileContentType.COMPLETE);
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							resultContainer);
					MessageAPI.send(pair, env);
				} catch (IOException e) {
					// 読み込みエラー
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							new SphereFileContentContainer(container.getSession(), null, this.getStrictName(),
									IPAddress.myIPAddress, FileContentType.ERROR));
					((SphereFileContentContainer) env.getContent()).setException(e);
					MessageAPI.send(pair, env);
				}
			} catch (UnknownHostException e) {
				// 送られたIPが不正
				e.printStackTrace();
			}
		}
	}

	private void receiveOpenFile(SphereFileContentContainer container) {
		FileContent content = container.getData();
		if (content instanceof OpenArg) {
			// System.out.println("うけとったなり");
			OpenArg arg = (OpenArg) content;
			try {
				KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
						InetAddress.getByName(container.getFrom()), 55878);
				try {
					SphereFileChannel channel = fileOpen(arg.getPath(), arg.getOpt());
					SphereFileContentContainer resultContainer = new SphereFileContentContainer(container.getSession(),
							channel, this.getStrictName(), IPAddress.myIPAddress, FileContentType.COMPLETE);
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							resultContainer);
					MessageAPI.send(dst, env);
					// System.out.println("おくったなり");
				} catch (IOException e) {
					// IOえくせぷしょん
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							new SphereFileContentContainer(container.getSession(), null, this.getStrictName(),
									IPAddress.myIPAddress, FileContentType.ERROR));
					((SphereFileContentContainer) env.getContent()).setException(e);
					MessageAPI.send(dst, env);
				}
			} catch (UnknownHostException e) {
				// 不正なIP指定とか握りつぶしても...ばれへんか
				e.printStackTrace();
			}
		}
	}

	private void receiveCloseFile(SphereFileContentContainer container) {
		FileContent content = container.getData();
		if (content instanceof CloseArg) {
			//System.out.println("うけとったなり");
			CloseArg arg = (CloseArg) content;
			try {
				KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
						InetAddress.getByName(container.getFrom()), 55878);
				try {
					fileClose(arg.fd);
					SphereFileContentContainer resultContainer = new SphereFileContentContainer(container.getSession(),
							null, this.getStrictName(), IPAddress.myIPAddress, FileContentType.COMPLETE);
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							resultContainer);
					MessageAPI.send(dst, env);
					// System.out.println("おくったなり");
				} catch (IOException e) {
					// IOえくせぷしょん
					StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
							new SphereFileContentContainer(container.getSession(), null, this.getStrictName(),
									IPAddress.myIPAddress, FileContentType.ERROR));
					((SphereFileContentContainer) env.getContent()).setException(e);
					MessageAPI.send(dst, env);
				}
			} catch (UnknownHostException e) {
				// 不正なIP指定とか握りつぶしても...ばれへんか
				e.printStackTrace();
			}
		}
	}

	/**
	 * ファイルを開くか作成し、そのファイルにアクセスするためのSphereFileChannelを返します。
	 *
	 * @param path 開くまたは作成するファイルのパス
	 * @param opt  ファイルを開く方法を指定するオプション
	 * @return 新しいSphereFileChannel
	 * @throws IOException 入出力エラーが発生した場合
	 * @see {@link StandardOpenOption}
	 */
	public SphereFileChannel fileOpen(SphereFile path, OpenOption... opt) throws IOException {
		if (path.getHostIP() == null) {
			//現状はIP指定なしの場合ローカル扱い
			//最終的にはちょうどよいマシンを自動検知させる
			path = new SphereFile(path.getPath(), IPAddress.myIPAddress);
		}
		if (path.getHostIP().equals(IPAddress.myIPAddress)) {
			// ローカル
			SphereFileDescriptor tmp = new SphereFileDescriptor();
			SphereFileDescriptorAccess access = SphereSharedSecrets.getSphereFileDescriptorAccess();
			access.setPath(tmp, new SphereFile(path.getPath(), path.getHostIP()));
			access.setId(tmp, UUID.randomUUID());
			boolean append = false;
			boolean read = false;
			boolean write = false;
			for (OpenOption openOption : opt) {
				if (openOption.equals(StandardOpenOption.APPEND)) {
					access.setAppend(tmp, true);
					append = true;
				} else if (openOption.equals(StandardOpenOption.READ)) {
					read = true;
				} else if (openOption.equals(StandardOpenOption.WRITE)) {
					write = true;
				} else if (openOption.equals(StandardOpenOption.SPARSE)) {
					throw new UnsupportedOperationException("スパースってなんだよ(哲学)");
				} else if (openOption.equals(StandardOpenOption.DELETE_ON_CLOSE)) {
					throw new UnsupportedOperationException("対応未定");
				} else if (openOption.equals(StandardOpenOption.DSYNC)) {
					throw new UnsupportedOperationException("まだ未対応");
				} else if (openOption.equals(StandardOpenOption.SYNC)) {
					throw new UnsupportedOperationException("まだ未対応");
				}
			}
			access.setRead(tmp, read);
			access.setWrite(tmp, write);
			SphereFileChannel sChannel = new SphereFileChannel(tmp, read, write);
			FileChannel channel = FileChannel.open(Paths.get(path.getPath()), opt);
			System.err.println("FileOpen:" + path);
			setInfo(path);

			descriptorSet.add(tmp);
			descPositionMap.put(tmp, channel.position());
			if (append) {
				sChannel.position(channel.position());
			}
			if (!cacheMap.containsKey(access.getPath(tmp))) {
				cacheMap.put(access.getPath(tmp), new SphereFileCache(access.getPath(tmp)));
			}
			lockMap.put(path, new ReentrantReadWriteLock());
			channelMap.put(tmp, channel);
			return sChannel;
		} else {
			UUID session = UUID.randomUUID();
			OpenArg arg = new OpenArg(path, opt);
			FileTask ft = new FileTask();
			KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
					InetAddress.getByName(path.getHostIP()), 55878);
			SphereFileContentContainer content = new SphereFileContentContainer(session, arg, getStrictName(),
					IPAddress.myIPAddress, FileContentType.OPEN);
			StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()), content);
			taskMap.put(session, ft);
			MessageAPI.send(dst, env);
			// System.out.println("ファイルオープン転送");
			try {
				// System.out.println(session);//でばっぐ用
				return (SphereFileChannel) ft.get().getData();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				taskMap.remove(session);
			}
		}
		return null;
	}

	public void fileClose(SphereFileDescriptor fd) throws IOException {
		SphereFileDescriptorAccess access = SphereSharedSecrets.getSphereFileDescriptorAccess();
		System.err.println("くろーず:" + access.getPath(fd));
		if (access.getPath(fd).getHostIP().equals(IPAddress.myIPAddress)) {
			// ろーかる
			cacheMap.get(access.getPath(fd)).postAllData(fd);
			cacheMap.remove(access.getPath(fd));
			descPositionMap.remove(fd);
			descriptorSet.remove(fd);
			lockMap.remove(access.getPath(fd));
			channelMap.get(fd).close();
			channelMap.remove(fd);
		} else {
			// TODO
			UUID session = UUID.randomUUID();
			CloseArg arg = new CloseArg(fd);
			FileTask ft = new FileTask();
			KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
					InetAddress.getByName(access.getPath(fd).getHostIP()), 55878);
			System.err.println(dst.getKey());
			SphereFileContentContainer content = new SphereFileContentContainer(session, arg, getStrictName(),
					IPAddress.myIPAddress, FileContentType.CLOSE);
			StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()), content);
			taskMap.put(session, ft);
			MessageAPI.send(dst, env);
			//System.out.println("ファイルクローズ転送");
			try {
				//System.out.println(session);//でばっぐ用
				ft.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				taskMap.remove(session);
			}
		}
	}

	/**
	 * 指定したパスが示すファイルの情報を取得します
	 *
	 * @param path
	 * @return 指定したファイルのFileInfo
	 * @throws IOException SphereFileが表すファイルの情報取得時のIOエラー
	 */
	public FileInfo getFileInfo(SphereFile path) throws IOException {
		// if(true)throw new UnsupportedOperationException("まだ");
		if (path == null) {
			throw new NullPointerException("ｶﾞｯ");
		}
		if (path.getHostIP().equals(IPAddress.myIPAddress)) {
			// 新規ろーかる

			if (!infoMap.containsKey(path)) {
				FileInfo info = null;
				try {
					info = setInfo(path);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return info;
			} else {
				// もうある
				// 実はTOCTOU問題とかあるかも？(研究フック)
				return infoMap.get(path);
			}
		} else {
			// 外部
			if (!dht.contains(path.getPath())) {
				throw new NoSuchFileException(path.getPath());
			}
			// タスクIDの生成
			UUID session = UUID.randomUUID();
			String destIP = path.getHostIP();
			FileTask ft = new FileTask();
			StandardEnvelope env = new StandardEnvelope(new AgentAddress(this.getClass().getName()),
					new SphereFileContentContainer(session, path, this.getStrictName(), IPAddress.myIPAddress,
							FileContentType.READ, FileContentType.INFO));
			KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<InetAddress, Integer>(
					InetAddress.getByName(destIP), 55878);
			taskMap.put(session, ft);
			MessageAPI.send(dst, env);
			try {
				// System.out.println(session);//でばっぐ用
				SphereFileContentContainer resultconContainer = ft.get();
				return (FileInfo) resultconContainer.getData();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				taskMap.remove(session);
			}

		}
		return null;
	}

	/**
	 * 指定した記述子と結びついているファイルの情報を取得します
	 *
	 * @param SphereFileDescriptor
	 * @return 指定したファイルのFileInfo
	 * @throws IOException SphereFileDescriptorと結びついているファイルの情報取得時のIOエラー
	 */
	public FileInfo getFileInfo(SphereFileDescriptor fd) throws IOException {
		return getFileInfo(SphereSharedSecrets.getSphereFileDescriptorAccess().getPath(fd));
	}

	/**
	 * 指定したパスが示すファイルの情報を取得します
	 * <p>
	 * パスはSphereFileの構築を介して解決されます
	 *
	 * @param path
	 * @return 指定したファイルのパス
	 * @throws IOException パス文字列が表すファイルの情報取得時のIOエラー
	 */
	public FileInfo getFileInfo(String Path) throws IOException {
		return getFileInfo(new SphereFile(Path));
	}

	/*
	 * 指定したパスのFileInfoを登録 ローカルファイル専用
	 */
	private synchronized FileInfo setInfo(SphereFile f) throws IOException {

		long fileSize = 0L;
		FileType ft = null;
		boolean hidden = false;
		Instant modifiedTime = null;
		Map<Integer, Instant> lastModified = null;

		Path path = Paths.get(f.getPath());
		BasicFileAttributes attr = null;

		attr = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
		if (infoMap.containsKey(f)) {
			return infoMap.get(f);
		}
		if (attr.isDirectory()) {
			ft = FileType.DIRECTORY;
		} else if (attr.isRegularFile()) {
			ft = FileType.REGULAR;
			fileSize = attr.size();
		} else if (attr.isSymbolicLink()) {
			ft = FileType.SYMBOLIC;
		} else if (attr.isOther()) {
			ft = FileType.OTHER;
		}
		modifiedTime = attr.lastModifiedTime().toInstant();
		// lastModified = new Instant[(int) (fileSize / FileChunk.CHUNKSIZE + 1)];
		lastModified = new ConcurrentHashMap<Integer, Instant>();
		for (int i = 0; i < (int) (fileSize / FileChunk.CHUNKSIZE + 1); i++) {
			lastModified.put(i, modifiedTime);
		}
		FileInfo info = new FileInfo(f, fileSize, ft, hidden, lastModified, modifiedTime);
		UUID uuid = UUID.randomUUID();
		fileKeyTable.setPair(attr.fileKey(), uuid);
		infoMap.put(f, info);
		return info;
	}

	private class FileKeyTable {
		Map<Object, UUID> idTable;
		Map<UUID, Object> keyTable;
		ReadWriteLock lock = new ReentrantReadWriteLock();

		public FileKeyTable() {
			idTable = new HashMap<>();
			keyTable = new HashMap<>();
		}

		void setPair(Object fileKey, UUID id) {
			lock.writeLock().lock();
			try {
				idTable.put(fileKey, id);
				keyTable.put(id, fileKey);
			} finally {
				lock.writeLock().unlock();
			}
		}

		/*
		 * UUID getUUID(Object fileKey) { lock.readLock().lock(); try { return
		 * idTable.get(fileKey); } finally { lock.readLock().unlock(); } }
		 */

		/*
		 * Object getFileKey(UUID id) { lock.readLock().lock(); try { return
		 * keyTable.get(id); } finally { lock.readLock().unlock(); } }
		 */
		/*
		 * UUID getUUID(String path) throws IOException { return
		 * getUUID(SystemUtility.getFileKey(path)); }
		 */
	}
}
