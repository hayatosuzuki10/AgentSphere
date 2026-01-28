package sphereConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import primula.api.core.ICoreModule;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.InformationCenter;
import scheduler2022.JudgeOS;
import scheduler2022.Scheduler;
import scheduler2022.collector.DynamicPcInfoCollector;
import scheduler2022.collector.PCInfoCollector;
import scheduler2022.util.DHTutil;
import sphereConnection.stub.SphereSpec;

/**
 * 簡易的な分散ハッシュテーブル
 * <p>
 * 各種操作をブロードキャストするごり押しシステムです。
 * ポートの55880番を使用します。
 * <p>
 * 最終更新を保持しておくことでなんかうまい感じに保持できるこうどなシステム（）
 * <p>
 * タイミングとかの問題でremoveはサポートしてません
 * <p>
 * 2022年度chordDHT導入により現在は使用を推奨しません<br>
 * src/setting/AgentSphere.diconでコメントアウトされているのでそういう感じです
 * @author Norito
 *
 */
public class EasyDistributeHashTable implements ICoreModule {
	Map<Integer, String> pathHT;
	Map<Integer, Instant> pathModified;
	ReadWriteLock pathRWLock;

	Map<Integer, SphereSpec> specHT;
	Map<Integer, Instant> specModified;
	ReadWriteLock specRWLock;

	Map<Integer, DynamicPCInfo> pcinfoHT;
	Map<String, Instant> pcinfoModified;
	ReadWriteLock pcinfoRWLock;
	String myIP;
	DatagramSocket socket;

	CountDownLatch ready;
	static final int PORT = 55880;
	static final int WAITTIME = 5 * 1000;
	
	static final DynamicPcInfoCollector collector = new DynamicPcInfoCollector();

	public void print() {
		for(String str : pcinfoModified.keySet()) {
			System.out.println(str+" @@ "+pcinfoModified.get(str));
		}
		for(Integer i : specHT.keySet()) {
			System.out.println(specHT.get(i).time);
		}
	}

	public void setIP(String key, String value) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		try {
			pathRWLock.writeLock().lock();
			pathHT.put(key.hashCode(), value);
			pathModified.put(key.hashCode(), Instant.now());
		} finally {
			pathRWLock.writeLock().unlock();
		}
	}
	/**
	 * 指定されたキーがマップされている値を返します。このDHTにそのキーのマッピングが含まれていない場合はnullを返します。
	 * @param key 関連付けられた値が返されるキー
	 * @return 指定されたキーがマップされている値。このマップにそのキーのマッピングが含まれていない場合はnull
	 */
	public String getIP(String key) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		try {
			pathRWLock.readLock().lock();
			return pathHT.get(key.hashCode());
		} finally {
			pathRWLock.readLock().unlock();
		}
	}
	/**
	 * 指定のキーのマッピングがこのDHTに含まれている場合にtrueを返します。
	 * @param key このマップ内にあるかどうかが判定されるキー
	 * @return このDHTが指定のキーのマッピングを保持する場合はtrue。
	 */
	public boolean containsIP(String Key) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		try {
			pathRWLock.readLock().lock();
			return pathHT.containsKey(Key.hashCode());
		} finally {
			pathRWLock.readLock().unlock();
		}
	}

	/**
	 * 指定されたSpecと指定されたキーをこのDHTで関連付けます
	 * <p>
	 * システムが正常にinitializeCoreModeleを呼び終わるまでは無期限にブロックされます
	 * @param key 指定されたSpecが関連付けられるキー
	 * @param spec 指定されたキーに関連付けられるSpec
	 */
	public void setSpec(String key, SphereSpec spec) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}

		try {
			specRWLock.writeLock().lock();
			specHT.put(key.hashCode(), spec);
			specModified.put(key.hashCode(), Instant.now());
		} finally {
			specRWLock.writeLock().unlock();
		}
	}

	/**
	 * 指定されたキーがマップされている値を返します。このDHTにそのキーのマッピングが含まれていない場合はnullを返します。
	 * @param key 関連付けられた値が返されるキー
	 * @return 指定されたキーがマップされている値。このマップにそのキーのマッピングが含まれていない場合はnull
	 */
	public SphereSpec getSpec(String key) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		try {
			specRWLock.readLock().lock();
			return specHT.get(key.hashCode());
		} finally {
			specRWLock.readLock().unlock();
		}
	}

	/**
	 * 指定のキーのマッピングがこのDHTに含まれている場合にtrueを返します。
	 * @param key このマップ内にあるかどうかが判定されるキー
	 * @return このDHTが指定のキーのマッピングを保持する場合はtrue。
	 */
	public boolean containsSpec(String key) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		try {
			specRWLock.readLock().lock();
			return specHT.containsKey(key.hashCode());
		} finally {
			specRWLock.readLock().unlock();
		}
	}

	/*
	 * ipアドレスをキーにPC状況を格納するやつ
	 * ipアドレスがStringなのが気になる
	 */
	public void setPcInfo(String key, DynamicPCInfo cpi) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}

		try {
			pcinfoRWLock.writeLock().lock();
			pcinfoHT.put(key.hashCode(), cpi);
			pcinfoModified.put(key, Instant.now());
		} finally {
			pcinfoRWLock.writeLock().unlock();
		}
	}

	public DynamicPCInfo getPcInfo(String key) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		try {
			pcinfoRWLock.readLock().lock();
			return pcinfoHT.get(key.hashCode());
		} finally {
			pcinfoRWLock.readLock().unlock();
		}
	}

	public Instant getPcTimeStamp(String str) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		return pcinfoModified.get(str);
	}

	public Set<String> getAllSuvivalIPaddresses(){
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		Set<String> allIPaddresses = new HashSet<>();
		try {
			pcinfoRWLock.readLock().lock();
			Instant comp = Instant.now().minusSeconds(60);
			for(String key : pcinfoModified.keySet()) {
				Instant temp = getPcTimeStamp(key);
				if(temp.isAfter(comp)) {
					allIPaddresses.add(key);
				}
			}
			return allIPaddresses;
		} finally {
			pcinfoRWLock.readLock().unlock();
		}
	}

	public boolean containPcInfo(String key) {
		try {
			ready.await();
		} catch (InterruptedException e) {
		}
		try {
			pcinfoRWLock.readLock().lock();
			return pcinfoHT.containsKey(key.hashCode());
		} finally {
			pcinfoRWLock.readLock().unlock();
		}
	}


	public EasyDistributeHashTable() {
		ready = new CountDownLatch(1);
	}

	@Override
	/**
	 * 最初の一回目しか機能しないしんせつせっけー
	 */
	public synchronized void initializeCoreModele() {
		if(ready.getCount()<=0) {
			return;
		}
		try {
			pathHT = new HashMap<>();
			pathModified = new HashMap<Integer, Instant>();
			specHT = new HashMap<>();
			specModified = new HashMap<Integer, Instant>();
			pcinfoHT = new HashMap<>();
			pcinfoModified = new HashMap<String, Instant>();
			pathRWLock = new ReentrantReadWriteLock();
			specRWLock = new ReentrantReadWriteLock();
			pcinfoRWLock = new ReentrantReadWriteLock();
			myIP = IPAddress.myIPAddress;
			socket = new DatagramSocket(PORT);
			socket.setBroadcast(true);

			new Sender().start();
			new Receiver().start();


			ready.countDown();
			myPcinfoUpdate();
			System.err.println(myIP+":EasyDHT有効です");
		} catch (IOException e) {
			throw new RuntimeException("NICの設定とかudpポートの設定とかその辺！", e);
		}
	}

	@Override
	public void finalizeCoreModule() {
	}

	private class Sender extends Thread {
		DatagramPacket packet;

		public Sender() {
			packet = new DatagramPacket(new byte[0], 0);
			packet.setPort(PORT);
			this.setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				myPcinfoUpdate();
				try {
					Thread.sleep(WAITTIME);
				} catch (InterruptedException e) {
				}
				//System.err.println("EasyDistributeHashTable:send");
				try {
					pathRWLock.readLock().lock();
					for (Iterator<Integer> iterator = pathHT.keySet().iterator(); iterator.hasNext();) {
						Integer key = iterator.next();
						PathData data = new PathData(pathModified.get(key), key, pathHT.get(key));
						ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
						ObjectOutputStream oos;
						try {
							oos = new ObjectOutputStream(baos);
							oos.writeObject(data);
						} catch (IOException e1) {
							// TODO 自動生成された catch ブロック
							e1.printStackTrace();
						}
						byte[] buf = baos.toByteArray();
						packet.setData(buf);
						try {
							for (InetAddress address : Util.getAllBroadcastAddresses()) {
								packet.setAddress(address);
								socket.send(packet);
								//System.err.println("EasyDistributeHashTable:send " + key + " " + pathHT.get(key));
							}
						} catch (SocketException e) {
						} catch (IOException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
				} finally {
					pathRWLock.readLock().unlock();
				}
				try {
					specRWLock.readLock().lock();
					for (Iterator<Integer> iterator = specHT.keySet().iterator(); iterator.hasNext();) {
						Integer key = iterator.next();
						SpecData data = new SpecData(specModified.get(key), key, specHT.get(key));
						ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
						ObjectOutputStream oos;
						try {
							oos = new ObjectOutputStream(baos);
							oos.writeObject(data);
						} catch (IOException e1) {
							// TODO 自動生成された catch ブロック
							e1.printStackTrace();
						}
						byte[] buf = baos.toByteArray();
						packet.setData(buf);
						try {
							for (InetAddress address : Util.getAllBroadcastAddresses()) {
								packet.setAddress(address);
								socket.send(packet);
							}
						} catch (SocketException e) {
						} catch (IOException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
				} finally {
					specRWLock.readLock().unlock();
				}
				try {
					pcinfoRWLock.readLock().lock();
					for (Iterator<String> iterator = pcinfoModified.keySet().iterator(); iterator.hasNext();) {
						String key = iterator.next();
						PcInfoData data = new PcInfoData(pcinfoModified.get(key), key, pcinfoHT.get(key.hashCode()));
						ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
						ObjectOutputStream oos;
						try {
							oos = new ObjectOutputStream(baos);
							oos.writeObject(data);
						} catch (IOException e1) {
							// TODO 自動生成された catch ブロック
							e1.printStackTrace();
						}
						byte[] buf = baos.toByteArray();
						packet.setData(buf);
						try {
							for (InetAddress address : Util.getAllBroadcastAddresses()) {
								packet.setAddress(address);
								socket.send(packet);
							}
						} catch (SocketException e) {
						} catch (IOException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
				} finally {
					pcinfoRWLock.readLock().unlock();
				}
				System.err.println("EasyDistributeHashTable:sended");
			}
		}
	}

	private class Receiver extends Thread {
		static final int DATALENGTH = 512;
		DatagramPacket packet;

		public Receiver() {
			packet = new DatagramPacket(new byte[512], 512);
			this.setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				packet.setLength(DATALENGTH);
				try {
					socket.receive(packet);
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
				InetAddress address = packet.getAddress();
				try {
					if (Util.getAllLocalHostAddresses().contains(address)) {
						continue;
					}
				} catch (SocketException e) {
					continue;
				}
				ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
				try {
					
					ObjectInputStream ois = new ObjectInputStream(bais);
					
					TableData td = (TableData) ois.readObject();
					if (td instanceof PathData) {
						PathData pd = (PathData) td;
						try {
							pathRWLock.writeLock().lock();
							if (!pathModified.containsKey(pd.pathHashKey)) {
								pathHT.put(pd.pathHashKey, pd.IP);
								pathModified.put(pd.pathHashKey, pd.timeStamp);
								System.err.println("EasyDHT:add "+pd);
							} else if (pd.timeStamp.isAfter(pathModified.get(pd.pathHashKey))) {
								pathHT.put(pd.pathHashKey, pd.IP);
								pathModified.put(pd.pathHashKey, pd.timeStamp);
							}
						} finally {
							pathRWLock.writeLock().unlock();
						}
					} else if (td instanceof SpecData) {
						SpecData sd = (SpecData) td;
						try {
							specRWLock.writeLock().lock();
							if (!specModified.containsKey(sd.specHashKey)) {
								specHT.put(sd.specHashKey, sd.spec);
								specModified.put(sd.specHashKey, sd.timeStamp);
							} else if (sd.timeStamp.isAfter(specModified.get(sd.specHashKey))) {
								specHT.put(sd.specHashKey, sd.spec);
								specModified.put(sd.specHashKey, sd.timeStamp);
							}
						} finally {
							specRWLock.writeLock().unlock();
						}
					}else if(td instanceof PcInfoData) {
						PcInfoData pid = (PcInfoData) td;
						try {
							pcinfoRWLock.writeLock().lock();
							if(!pcinfoModified.containsKey(pid.pcinfoHashKey)) {
								pcinfoHT.put(pid.pcinfoHashKey.hashCode(), pid.cpi);
								pcinfoModified.put(pid.pcinfoHashKey, pid.timeStamp);
							}else if(pid.timeStamp.isAfter(pcinfoModified.get(pid.pcinfoHashKey))){
								pcinfoHT.put(pid.pcinfoHashKey.hashCode(), pid.cpi);
								pcinfoModified.put(pid.pcinfoHashKey, pid.timeStamp);
							}
						}finally {
							pcinfoRWLock.writeLock().unlock();
						}
					}
				} catch (IOException | ClassNotFoundException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
		}
	}

	public void myPcinfoUpdate() {
		CentralProcessor cp = new SystemInfo().getHardware().getProcessor();
	    double tmp = cp.getSystemLoadAverage(1)[0];
	    if (tmp < 0) return;
	    if (JudgeOS.isWindows()) tmp += 1.0;

	    final double la = tmp; 
	    DynamicPCInfo prevDPI = DHTutil.getPcInfo(myIP);
	    DynamicPCInfo dpi;
	    if (prevDPI != null && prevDPI.isForecast
	            && prevDPI.timeStanp + Scheduler.getTimeStampExpire() > System.currentTimeMillis()) {

	        dpi = prevDPI;

	    } else {
	    	dpi = collector.collect(
	    			InformationCenter.getOthersIPs(),
		            Scheduler.getReceiverPort() + 1,
		            Scheduler.isFirst(),
		            PCInfoCollector.snapshot.gcCount,          // JFRからの値
		            PCInfoCollector.snapshot.gcPauseMillis     // JFRからの値
		    );
	        PCInfoCollector.getPcInfoRepo().saveDynamic(IPAddress.myIPAddress, dpi);
	    }
	    
	    setPcInfo(myIP, dpi);
	}

	abstract static private class TableData implements Serializable {
		Instant timeStamp;

		public TableData(Instant timeStamp) {
			this.timeStamp = timeStamp;
		}
	}

	private static class PathData extends TableData {
		public int pathHashKey;
		public String IP;

		public PathData(Instant timeStamp, Integer key, String IP) {
			super(timeStamp);
			this.pathHashKey = key.hashCode();
			this.IP = IP;
		}

		public PathData(Instant timeStamp, String key, String IP) {
			super(timeStamp);
			this.pathHashKey = key.hashCode();
			this.IP = IP;
		}

		@Override
		public String toString() {
			return pathHashKey+IP;
		}
	}

	private static class SpecData extends TableData {
		public int specHashKey;
		public SphereSpec spec;

		public SpecData(Instant timeStamp, String key, SphereSpec spec) {
			super(timeStamp);
			this.specHashKey = key.hashCode();
			this.spec = spec;
		}

		public SpecData(Instant timeStamp, Integer key, SphereSpec spec) {
			super(timeStamp);
			this.specHashKey = key;
			this.spec = spec;
		}
	}

	private static class PcInfoData extends TableData{
		public String pcinfoHashKey;
		public DynamicPCInfo cpi;

		public PcInfoData(Instant timeStamp, String key, DynamicPCInfo cpi) {
			super(timeStamp);
			this.pcinfoHashKey = key;
			this.cpi = cpi;
		}

//		public PcInfoData(Instant timeStamp, Integer key, CrtPcInfo cpi) {
//			super(timeStamp);
//			this.pcinfoHashKey = key;
//			this.cpi = cpi;
//		}
	}

	public static void main(String[] args) throws ClassNotFoundException {
		try {
			EasyDistributeHashTable table = new EasyDistributeHashTable();
			table.initializeCoreModele();
			while (!table.containsSpec("AbstractAgent")) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			System.out.println(table.getSpec("AbstractAgent").spec);

			if (true)
				return;
			PathData pd = new PathData(Instant.now(), "share\\testdkdkdkdkdkdk", "1.1.1.1");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(pd);
			System.out.println(baos.toString());
			System.out.println(baos.toByteArray().length);

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			
			ObjectInputStream ois = new ObjectInputStream(bais);

			if (ois != null) {
				TableData td = (TableData) ois.readObject();
			}
			System.out.println(((PathData) ois.readObject()).timeStamp.toString());

//			SpecData sd = new SpecData(Instant.now(), "AbstractAgent", new SphereSpec(10000, 4949494));
//			ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
//			ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
//			oos2.writeObject(sd);
//			System.out.println(baos2.toString());
//			System.out.println(baos2.toByteArray().length);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
}
