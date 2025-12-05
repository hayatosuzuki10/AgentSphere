/*
* To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author Mikamiyama
 */

package primula.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;

import primula.api.SystemAPI;
import primula.api.core.resource.SystemConfigResource;

/**
 *
 * AgentSphereネットワークのIP表です
 * 動的に取得できるようにしたいね
 * @author Ranton
 */
public final class IPAddress {
	public static String myIPAddress;
	public static short myNetworkPrefixLength;
	static {
		InterfaceAddress addr = getHostInterface();
		myIPAddress = addr.getAddress().getHostAddress().toString();
		myNetworkPrefixLength = addr.getNetworkPrefixLength();
		System.err.println("myIP is " + myIPAddress + "/" + myNetworkPrefixLength);
		System.err.println(new SubnetUtils(myIPAddress + "/" + myNetworkPrefixLength).getInfo().getNetworkAddress());
	}

	/**
	 * 今のところすべてのエージェントはこのIPに対してマイグレーションを試みる様です。
	 * seting/SystemConfig.jsonに記述があるのでそこを変更してください。
	 * <p>
	 * だれか修正してもいいのよ
	 * @author Ranton
	 */
	public static String IPAddress = getDestIP();

	/**
	 * 自分自身のIPです
	 * <p>
	 * エージェントを生成する際にマイグレート先から戻ってくるのに必要
	 * 最初にSystemConfigから取得しようとする(NICが複数あって設定もややこしい場合の回避策)
	 * SystemConfig.jsonに設定がなければ自動的に取得する(動的取得であり、この動作が基本であってほしいけど…)
	 * @author RanPork
	 */

	private static InterfaceAddress getHostInterface() {
		try {
			Object ip = SystemAPI.getSystemConfigData(SystemConfigResource.DEFAULT_IP_ADDR);
			if (ip == null) {
				throw new NullPointerException("/setting/SystemConfig.jsonが不正です DefaultIPaddrの値を確認してください");
			}
			String defaddr = ip.toString();
			Object mask = SystemAPI.getSystemConfigData(SystemConfigResource.DEFAULT_NETWORK_PREFIX_LENGTH);
			if (mask == null) {
				throw new NullPointerException("/setting/SystemConfig.jsonが不正です DefaultNetworkPrefixLengthの値を確認してください");
			}
			String defsubnet = mask.toString();
			if (!(defaddr.compareTo("") == 0 || Objects.isNull(InetAddress.getByName(defaddr)))) {
				short prefix;
				try {
					prefix = Short.parseShort(defsubnet);
				} catch (NumberFormatException e) {
					throw new NumberFormatException(
							"/setting/SystemConfig.jsonが不正です DefaultNetworkPrefixLengthの値を確認してください");
				}
				NetworkInterface interface1 = NetworkInterface.getByInetAddress(InetAddress.getByName(defaddr));
				if (interface1 == null) {
					throw new NullPointerException("/setting/SystemConfig.jsonが不正です DefaultIPaddrの値を確認してください");
				}
				for (InterfaceAddress interfaceAddress : interface1.getInterfaceAddresses()) {
					if (interfaceAddress.getAddress().getHostAddress().toString().equals(defaddr)
							&& interfaceAddress.getNetworkPrefixLength() == prefix) {
						System.err.println("get HostAddr OK " + defaddr + "/" + prefix + " from SystemConfig.json");
						return interfaceAddress;
					}
				}
				return null;
			}

			for (NetworkInterface networkInterface : getAllLocalHostInterface()) {
				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					if (interfaceAddress.getAddress() instanceof Inet4Address) {
						System.err.println("get HostAddr OK " + interfaceAddress.getAddress().getHostAddress());
						return interfaceAddress;
					}
				}
			}
			return null;
		} catch (UnknownHostException | SocketException e) {
			return null;
		}
	}

	private static String getDestIP() {
		if (Objects.isNull(SystemAPI.getSystemConfigData(SystemConfigResource.DEFAULT_DEST))) {
			System.err.println("oooooo");
		}
		String dest = SystemAPI.getSystemConfigData(SystemConfigResource.DEFAULT_DEST).toString();
		try {
			if (dest.compareTo("") == 0 || Objects.isNull(InetAddress.getByName(dest))) {
				throw new RuntimeException("/setting/SystemConfig.jsonが不正です DefaultDestの値を確認してください");
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new RuntimeException("getByNameがらみのエラー");
		}
		System.err.println("get DefaultDestAddr OK " + dest);
		return dest;
	}

	private static Set<NetworkInterface> getAllLocalHostInterface() throws SocketException {

		Set<NetworkInterface> intefacelist = new HashSet<>();
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		Collections.list(interfaces).stream()
				.filter((NetworkInterface a) -> {
					try {
						return a.isUp() && !a.isLoopback();
					} catch (SocketException e) {
						return false;
					}
				})
				.forEach(intefacelist::add);
		return intefacelist;
	}

	/**
	 * これらは誰かが実験で使ったやつ
	 * <p>消してもまあ問題はない
	 */
	public static String Master = "133.220.114.125";
	public static String Slave1 = "133.220.114.125";
	public static String Slave2 = "133.220.114.125";
	public static String Slave3 = "133.220.114.125";
	public static String Slave4 = "133.220.114.245";
	public static String Slave5 = "133.220.114.245";
	public static String Slave6 = "133.220.114.245";
	public static String Slave7 = "133.220.114.245";
	/*
	 * 133.220.114.118:PC1(nakatsu）
	 * 133.220.114.122:PC2(haga)*
	 * 133.220.114.105:PC3(kazikawa)*
	 * 133.220.114.108:PC4(nakadaira)
	 * 133.220.114.112:PC5(nakazima)
	 * 133.220.114.120:PC6(takahashi)
	 * 133.220.114.121:PC7(tsuda)
	 * 133.220.114.110:PC8(katagiri)*
	 */
}