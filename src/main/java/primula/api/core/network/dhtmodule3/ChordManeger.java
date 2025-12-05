package primula.api.core.network.dhtmodule3;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.api.core.ICoreModule;
import primula.api.core.network.SystemDHT;
import primula.util.IPAddress;
import sphereConnection.EasySphereNetworkManeger;
import sphereConnection.NetworkUpdateListener;

public class ChordManeger implements ICoreModule, NetworkUpdateListener, SystemDHT {

	private NodeImpl<Serializable> node;

	Set<String> AfterMyIP;

	@Override
	public void initializeCoreModele() {
		// TODO 多重参加をさせないように
		if (node == null) {
			try {
				node = new NodeImpl<Serializable>("Node" + IPAddress.myIPAddress);
				System.err.println("ChordManeger:起動");
				EasySphereNetworkManeger esnm = (EasySphereNetworkManeger) SingletonS2ContainerFactory.getContainer()
						.getComponent("EasySphereNetworkManeger");
				esnm.putUpdateListener(this);
			} catch (RemoteException e) {
				// TODO 自動生成された catch ブロック
				throw new RuntimeException("DHT初期化失敗:rmiのエラー", e);
			}

		}
		//IPが大きいAgentSphereのDHTにアクセスさせるための苦肉の策
		if (AfterMyIP == null) {
			AfterMyIP = new HashSet<String>();
			SubnetUtils mysub = new SubnetUtils(IPAddress.myIPAddress + "/" + IPAddress.myNetworkPrefixLength);
			String myBroad = mysub.getInfo().getBroadcastAddress();
			mysub = mysub.getNext();
			while (!myBroad.equals(mysub.getInfo().getAddress())) {
				AfterMyIP.add(mysub.getInfo().getAddress());
				//System.err.println(mysub.getInfo().getAddress());
				mysub = mysub.getNext();
			}
		}
	}

	@Override
	public void finalizeCoreModule() {
		// TODO 自動生成されたメソッド・スタブ
		if (node != null)
			node.leave();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void updateNetwork() {
		// TODO 自動生成されたメソッド・スタブ
		boolean flag = false;
		try {
			if (node.getSuccessor() == node.getPredecessor()) {
				System.err.println("ChordManeger:updateNetwork");
				EasySphereNetworkManeger esnm = (EasySphereNetworkManeger) SingletonS2ContainerFactory.getContainer()
						.getComponent("EasySphereNetworkManeger");
				Set<String> ips = esnm.getIPTable();

				for (String ip : ips) {
					//自分自身及びループバックアドレスははじいて参加しにいかない
					//ついでに両方が同時に接続しようとしてややこしくならないようにする
					if (ip.equals(IPAddress.myIPAddress) || ip.equals("127.0.0.1") || !checkMyNetworkAddress(ip)
							|| !(AfterMyIP.contains(ip))) {
						continue;
					}
					try {
						flag = true;
						Registry registry = LocateRegistry.getRegistry(ip, NodeImpl.DEFAULT_PORT);
						node.join((Node<Serializable>) registry.lookup("Node" + ip));
						System.err.println("ChordManeger:" + ip + "からDHTに参加します");
						break;
					} catch (RemoteException e) {
						e.printStackTrace();
					} catch (NotBoundException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
				if (flag) {
					if (node.getSuccessor() == node.getPredecessor()) {
						System.err.println("ChordManeger:参加失敗");
					} else {
						System.err.println("ChordManeger:参加成功");
					}
				}
			}

		} catch (RemoteException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	private boolean checkMyNetworkAddress(String addr) {
		try {
			InetAddress.getByName(addr);
		} catch (UnknownHostException e) {
			return false;
		}
		SubnetInfo myIP = new SubnetUtils(IPAddress.myIPAddress + "/" + IPAddress.myNetworkPrefixLength).getInfo();
		SubnetInfo other = new SubnetUtils(addr + "/" + IPAddress.myNetworkPrefixLength).getInfo();

		return myIP.getNetworkAddress().equals(other.getNetworkAddress());
	}

	@Override
	public void put(String key, Serializable value) {
		// TODO 自動生成されたメソッド・スタブ
		node.put(key, value);
	}

	@Override
	public Serializable get(String key) {
		// TODO 自動生成されたメソッド・スタブ
		return node.get(key);
	}

	@Override
	public void remove(String key) {
		// TODO 自動生成されたメソッド・スタブ
		node.remove(key);
	}

	@Override
	public List<String> listAll() {
		// TODO 自動生成されたメソッド・スタブ
		return node.listAll();
	}

	@Override
	public boolean contains(String key) {
		// TODO 自動生成されたメソッド・スタブ
		return node.contains(key);
	}

}
