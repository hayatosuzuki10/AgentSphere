package scheduler2022.network;

public class NetworkSpeedCache {
    public String ip;
    public double uploadMbps;
    public double downloadMbps;
    public long lastMeasuredAt;

    public NetworkSpeedCache(String ip, double uploadMbps, double downloadMbps) {
        this.ip = ip;
        this.uploadMbps = uploadMbps;
        this.downloadMbps = downloadMbps;
        this.lastMeasuredAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        // 30sで失効とする
        return System.currentTimeMillis() - lastMeasuredAt >  30 * 1000;
    }
}