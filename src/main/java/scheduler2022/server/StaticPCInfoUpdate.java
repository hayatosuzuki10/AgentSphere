package scheduler2022.server;

import java.io.Serializable;

import scheduler2022.StaticPCInfo;

public class StaticPCInfoUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StaticPCInfo spi;
    private final String ip;

    public StaticPCInfoUpdate(String ip, StaticPCInfo spi) {
        if (ip == null || spi == null) {
            throw new IllegalArgumentException("ip and dpi must not be null");
        }
        this.ip = ip;
        this.spi = spi;
    }

    public StaticPCInfo getSpi() {
        return spi;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return "StaticPCInfoUpdate[ip=" + ip + ", spi=" + spi + "]";
    }
}
