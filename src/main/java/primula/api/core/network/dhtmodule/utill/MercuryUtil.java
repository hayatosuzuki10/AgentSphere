package primula.api.core.network.dhtmodule.utill;

import primula.api.core.network.dhtmodule.routing.impl.MercuryImpl;

public class MercuryUtil {
	private static MercuryImpl impl = null;

	public static MercuryImpl getImpl() {
		return impl;
	}

	public static void setImpl(MercuryImpl imple) {
		impl = imple;
	}
}
