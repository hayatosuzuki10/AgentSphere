package sphereConnection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.commons.net.util.SubnetUtils;

import primula.api.SystemAPI;
import primula.api.core.ICoreModule;
import primula.api.core.resource.SystemConfigResource;
import primula.util.IPAddress;

/**
 * ブロードキャストによってAgentSphereが互いにIPを把握しあうことができるシステム
 * <p>
 * とりあえずgetIPTable使っとけば良いイメージ
 * <p>
 * ポートの55879番を使用します
 * <p>
 * keep alive信号を送りあって生存確認している感じです
 * <p>
 * 複数のNICを持つコンピュータには対応していない(信頼できるものではない)ので注意！
 * VirtualBoxとかインストールしてるとVirtualBoxホストオンリーなんちゃらが作られ、確実に引っかかるので無効化させることをお勧めします
 * <p>
 * NetworkUpdateListenerを使用することでテーブルの更新を通知できます
 * chordManegerなどで使用されているので使い方はなんとなく察してください
 * @author RanPork
 */
public class EasySphereNetworkManeger implements ICoreModule {
	ReadWriteLock IPTableLock;
	Map<String, Instant> IPTable;

	
	/**
	 * 生存信号の間隔(ミリ秒)
	 * <p>
	 * 適当に変えるはずなので<b>決してこの数字を前提にしたコードは組まないこと</b>
	 */
	static final int WAITTIME = 5 * 1000;//millisec
	/**
	 * AgentSphereの生存時間的な(ミリ秒)
	 * <p>
	 * これを超えて返事が来ないAgentSphereは死んだ！もういない！とみなす<br>
	 * 適当に変えるはずなので<b>決してこの数字を前提にしたコードは組まないこと</b>
	 */
	static final int TIMEOUT = WAITTIME * 4;//millisec
	/**
	 * 他のブロードキャストを生存信号扱いしないようにするための合言葉ですね
	 * バージョン違いのAgentSphereを識別する信号でもあります
	 */
	static final String keyWord = (String) SystemAPI.getSystemConfigData(SystemConfigResource.AGENTSPHERE_VERSION_NAME);

	private DatagramSocket socket;
	private Sender sender;
	private Reciver reciver;
	private TimeoutChecker timeoutChecker;
	Set<InetAddress> sendAddressSet;
	Set<InetAddress> myAddressSet;

	private ArrayList<NetworkUpdateListener> updateListeners;

	CountDownLatch ready;

	public EasySphereNetworkManeger() {
		ready = new CountDownLatch(1);
	}

	@Override
	synchronized public void initializeCoreModele() {
		if (ready.getCount() <= 0) {
			return;
		}
		try {
			IPTableLock = new ReentrantReadWriteLock(true);
			IPTable = new ConcurrentHashMap<String, Instant>();
			sendAddressSet = Util.getAllBroadcastAddresses();
			myAddressSet = Util.getAllLocalHostAddresses();
			updateListeners = new ArrayList<NetworkUpdateListener>();
			int port = 55879; // デフォルト
			Object configPort = SystemAPI.getSystemConfigData("AGENTSPHERE_PORT");
			if (configPort instanceof Number) {
			    port = ((Number) configPort).intValue();
			}
			socket = new DatagramSocket(port);
			socket.setBroadcast(true);
			sender = new Sender(this);
			reciver = new Reciver(this);
			timeoutChecker = new TimeoutChecker();
			sender.setName("E-NetworkManeger-Sender");
			sender.setPriority(Thread.NORM_PRIORITY + 2);   // 7

			reciver.setName("E-NetworkManeger-reciver");
			reciver.setPriority(Thread.MAX_PRIORITY);       // 10（受信は最優先）

			timeoutChecker.setName("E-NetworkManeger-TimeoutChecker");
			timeoutChecker.setPriority(Thread.NORM_PRIORITY + 1); // 6

			sender.start();
			reciver.start();
			timeoutChecker.start();
			

			ready.countDown();
		} catch (IOException e) {
			throw new RuntimeException("NICの設定とかudpポートの設定とかその辺！", e);
		}
	}

	@Override
	public void finalizeCoreModule() {
	}

	/**
	 * このマネージャが収集したIPの内タイムアウトしていないIPの集合を返します
	 * <p>
	 * この集合はこのマネージャが収集している情報とは<b>同期しない</b>ので注意
	 * @return 有効なIPが含まれるSet
	 */
	synchronized public Set<String> getIPTable() {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}

		//Set<String> resultSet = new HashSet<>();
		IPTableLock.readLock().lock();
		try {
			Instant comp = Instant.now().minusMillis(TIMEOUT);
			return IPTable.entrySet().stream()
					.filter(a -> a.getValue().isAfter(comp))
					.map(a -> a.getKey())
					.collect(Collectors.toSet());
			//.forEach(a -> resultSet.add(a.getKey()));//こういう外付けSetはいかんらしい
			//return resultSet;//Collectorsにやらせる方がよいっぽい
		} finally {
			IPTableLock.readLock().unlock();
		}
	}

	synchronized public void putUpdateListener(NetworkUpdateListener listener) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		updateListeners.add(listener);
	}

	/**
	 * このマネージャが収集したIPとその受け取り時間の関連を保持したMapの変更不可能なビューを取得します
	 * <p>
	 * このMapはこのマネージャが収集している情報と同期します
	 * @return 収集したIPと取得時間の関連を表すMapビュー
	 */
	synchronized public Map<String, Instant> getIPMap() {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}

		return Collections.unmodifiableMap(IPTable);
	}

	private class Sender extends Thread {
		byte[] buf;
		List<DatagramPacket> packs;

		public Sender(EasySphereNetworkManeger easySphereNetworkManeger) {
			try {
				buf = keyWord.getBytes("UTF-8");
				//明示的にアドレス指定されると実は通信できる　明示的にやりたいのなら止めはしない
				System.err.println("EasySphereNetworkManeger:このAgentSphereのバージョンは"+keyWord+"です\nEasySphereNetworkManeger:同一バージョンのAgentSphereのみ通信します");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			packs = new ArrayList<>();
			String mysubStr = new SubnetUtils(IPAddress.myIPAddress + "/" + IPAddress.myNetworkPrefixLength).getInfo()
					.getNetworkAddress();
			for (InetAddress addr : sendAddressSet) {
				SubnetUtils sub = new SubnetUtils(addr.getHostAddress() + "/" + IPAddress.myNetworkPrefixLength);
				if (sub.getInfo().getNetworkAddress().equals(mysubStr)) {
					System.err.println("EasySphereNetworkManeger:"+addr.getHostAddress()+"にブロードキャストします");
					int port = 55879; // デフォルト
					Object configPort = SystemAPI.getSystemConfigData("AGENTSPHERE_PORT");
					if (configPort instanceof Number) {
					    port = ((Number) configPort).intValue();
					}
					packs.add(new DatagramPacket(buf, buf.length, addr, port));
				}
			}
			this.setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(WAITTIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				try {
					for (DatagramPacket pack : packs) {
						socket.send(pack);
						//System.err.println("Send to "+pack.getAddress().getHostAddress());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class Reciver extends Thread {
		DatagramPacket pack;
		byte[] buf;

		public Reciver(EasySphereNetworkManeger easySphereNetworkManeger) {
			buf = new byte[16];
			pack = new DatagramPacket(buf, buf.length);
			this.setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				try {
					socket.receive(pack);
				} catch (IOException e) {
					e.printStackTrace();
				}
				InetAddress addr = pack.getAddress();
				String data = null;
				try {
					data = new String(Arrays.copyOf(pack.getData(), pack.getLength()), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				if (!myAddressSet.contains(addr) && data.equals(keyWord)) {
					try {
						IPTableLock.writeLock().lock();
						IPTable.put(addr.getHostAddress(), Instant.now());
						//System.err.println("NetworkManeger:add " + addr.getHostAddress());
						for (NetworkUpdateListener networkUpdateListener : updateListeners) {
							networkUpdateListener.updateNetwork();
						}
					} finally {
						IPTableLock.writeLock().unlock();
					}
				}
			}
		}
	}

	private class TimeoutChecker extends Thread {
		public TimeoutChecker() {
			this.setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(TIMEOUT);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
				try {
					Instant comp = Instant.now().minusMillis(TIMEOUT);
					IPTableLock.writeLock().lock();
					for (Iterator<String> iterator = IPTable.keySet().iterator(); iterator.hasNext();) {
						String addr = iterator.next();
						if (IPTable.get(addr).compareTo(comp) < 0) {
							System.err.println("NetworkManeger:detect Timeout remove " + addr);
							iterator.remove();
						}
					}
				} finally {
					IPTableLock.writeLock().unlock();
				}
			}
		}
	}

}
