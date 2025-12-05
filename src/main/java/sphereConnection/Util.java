package sphereConnection;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Util {

	private Util() {
	}

	private static Set<InetAddress> broadcastSet = null;

	public static Set<InetAddress> getAllBroadcastAddresses() throws SocketException {
		if (broadcastSet != null) {
			return broadcastSet;
		}
		broadcastSet = new HashSet<>();
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();

			if (networkInterface.isLoopback() || !networkInterface.isUp()) {
				continue;
			}

			networkInterface.getInterfaceAddresses().stream()
					.map(a -> a.getBroadcast())
					.filter(Objects::nonNull)
					.forEach(broadcastSet::add);
		}
		return broadcastSet;
	}

	private static Set<InetAddress> addressList = null;

	public static Set<InetAddress> getAllLocalHostAddresses() throws SocketException {
		if (addressList != null) {
			return addressList;
		}
		addressList = new HashSet<>();
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();

			if (networkInterface.isLoopback() || !networkInterface.isUp()) {
				continue;
			}

			networkInterface.getInterfaceAddresses().stream()
					.map(a -> a.getAddress())
					.filter(Objects::nonNull)
					.forEach(addressList::add);
		}
		for (InetAddress inetAddress : addressList) {
			System.err.println("myIP is " + inetAddress.getHostAddress());
		}
		return addressList;
	}
}
