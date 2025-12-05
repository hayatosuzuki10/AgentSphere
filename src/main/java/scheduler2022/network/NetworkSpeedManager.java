package scheduler2022.network;

import java.util.HashMap;
import java.util.Map;

public class NetworkSpeedManager {
    private static Map<String, NetworkSpeedCache> cache = new HashMap<>();

    public static double[] getSpeed(String ip, int port) {
        NetworkSpeedCache entry = cache.get(ip);

        if (entry == null || entry.isExpired()) {
            double[] speed = NetworkSpeedMeasurer.measureSpeed(ip, port);
            if (speed[0] > 0) {
                cache.put(ip, new NetworkSpeedCache(ip, speed[0], speed[1]));
            }
            return speed;
        } else {
            return new double[]{entry.uploadMbps, entry.downloadMbps};
        }
    }
}