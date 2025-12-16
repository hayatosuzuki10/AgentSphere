package scheduler2022.server;

import java.io.Serializable;

import scheduler2022.DynamicPCInfo;

public class DynamicPCInfoUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    private final DynamicPCInfo dpi;
    private final String ip;

    public DynamicPCInfoUpdate(String ip, DynamicPCInfo dpi) {
        if (ip == null || dpi == null) {
            throw new IllegalArgumentException("ip and dpi must not be null");
        }
        this.ip = ip;
        this.dpi = dpi;
    }

    public DynamicPCInfo getDpi() {
        return dpi;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return "DynamicPCInfoUpdate[ip=" + ip + ", dpi=" + dpi + "]";
    }
}