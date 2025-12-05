package scheduler2022;

import java.io.Serializable;
import java.util.Map;

public class StaticPCInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public CPU CPU = new CPU();
    public long TotalMemory;
    public Map<String, NetworkCard> NetworkCards;
    public Map<String, GPU> GPUs;

    public StaticPCInfo() {}

    public StaticPCInfo(CPU CPU, long TotalMemory,
                        Map<String, NetworkCard> NetworkCards,
                        Map<String, GPU> GPUs) {
        this.CPU = CPU;
        this.TotalMemory = TotalMemory;
        this.NetworkCards = NetworkCards;
        this.GPUs = GPUs;
    }

    @Override
    public String toString() {
        return "StaticPCInfo{CPU=" + CPU +
               ", TotalMemory=" + TotalMemory +
               ", NetworkCards=" + (NetworkCards != null ? NetworkCards.size() : 0) +
               ", GPUs=" + (GPUs != null ? GPUs.size() : 0) + "}";
    }

    public static class CPU implements Serializable {
        private static final long serialVersionUID = 1L;
        public String Name;
        public String Vendor;
        public String MicroArch;
        public int PhysicalCore;
        public int LogicalCore;
        public int BenchMarkScore;
        @Override public String toString() {
            return "CPU{Name='" + Name + "', Vendor='" + Vendor +
                   "', MicroArch='" + MicroArch + "', PhysicalCore=" + PhysicalCore +
                   ", LogicalCore=" + LogicalCore + ", BenchMarkScore=" + BenchMarkScore + "}";
        }
    }

    public static class NetworkCard implements Serializable {
        private static final long serialVersionUID = 1L;
        public String Name;
        public String DisplayName;
        public long Bandwidth;
        public long MTU;
        @Override public String toString() {
            return "NIC{Name='" + Name + "', DisplayName='" + DisplayName +
                   "', Bandwidth=" + Bandwidth + ", MTU=" + MTU + "}";
        }
    }

    public static class GPU implements Serializable {
        private static final long serialVersionUID = 1L;
        public String Name;
        public String Vendor;
        public long VRam;
        public String DeviceID;
        public int BenchMarkScore;
        @Override public String toString() {
            return "GPU{Name='" + Name + "', Vendor='" + Vendor +
                   "', VRam=" + VRam + ", DeviceID='" + DeviceID +
                   "', BenchMarkScore=" + BenchMarkScore + "}";
        }
    }
}