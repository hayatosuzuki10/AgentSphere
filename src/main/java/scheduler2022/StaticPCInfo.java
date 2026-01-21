package scheduler2022;

import java.io.Serializable;
import java.util.Map;

public class StaticPCInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public CPU CPU = new CPU();
    public long TotalMemory;
    public boolean hasSSD;
    public Map<String, NetworkCard> NetworkCards;
    public Map<String, GPU> GPUs;

    public StaticPCInfo() {}

    public StaticPCInfo(CPU CPU, long TotalMemory, boolean hasSSD,
                        Map<String, NetworkCard> NetworkCards,
                        Map<String, GPU> GPUs) {
        this.CPU = CPU;
        this.TotalMemory = TotalMemory;
        this.hasSSD = hasSSD;
        this.NetworkCards = NetworkCards;
        this.GPUs = GPUs;
    }

    /**
     * 深いコピー（deep copy）を返す。
     * 全て new してデータを複製するため、
     * 状態変更が元インスタンスへ影響しない。
     */
    public StaticPCInfo deepCopy() {
        StaticPCInfo copy = new StaticPCInfo();

        // --- CPU ---
        if (this.CPU != null) {
            CPU cpuCopy = new CPU();
            cpuCopy.Name = this.CPU.Name;
            cpuCopy.Vendor = this.CPU.Vendor;
            cpuCopy.MicroArch = this.CPU.MicroArch;
            cpuCopy.PhysicalCore = this.CPU.PhysicalCore;
            cpuCopy.LogicalCore = this.CPU.LogicalCore;
            cpuCopy.BenchMarkScore = this.CPU.BenchMarkScore;
            copy.CPU = cpuCopy;
        }

        // --- TotalMemory ---
        copy.TotalMemory = this.TotalMemory;
        copy.hasSSD = this.hasSSD;

        // --- NetworkCards ---
        if (this.NetworkCards != null) {
            Map<String, NetworkCard> ncMap = new java.util.HashMap<>();
            for (Map.Entry<String, NetworkCard> e : this.NetworkCards.entrySet()) {
                NetworkCard src = e.getValue();
                if (src == null) continue;

                NetworkCard dst = new NetworkCard();
                dst.Name = src.Name;
                dst.DisplayName = src.DisplayName;
                dst.Bandwidth = src.Bandwidth;
                dst.MTU = src.MTU;

                ncMap.put(e.getKey(), dst);
            }
            copy.NetworkCards = ncMap;
        }

        // --- GPUs ---
        if (this.GPUs != null) {
            Map<String, GPU> gpuMap = new java.util.HashMap<>();
            for (Map.Entry<String, GPU> e : this.GPUs.entrySet()) {
                GPU src = e.getValue();
                if (src == null) continue;

                GPU dst = new GPU();
                dst.Name = src.Name;
                dst.Vendor = src.Vendor;
                dst.VRam = src.VRam;
                dst.DeviceID = src.DeviceID;
                dst.BenchMarkScore = src.BenchMarkScore;

                gpuMap.put(e.getKey(), dst);
            }
            copy.GPUs = gpuMap;
        }

        return copy;
    }

    @Override
    public String toString() {
        return "StaticPCInfo{CPU=" + CPU +
               ", TotalMemory=" + TotalMemory +
               ", hasSSD=" + hasSSD +
               ", NetworkCards=" + (NetworkCards != null ? NetworkCards.size() : 0) +
               ", GPUs=" + (GPUs != null ? GPUs.size() : 0) + "}";
    }

    // --- Inner Classes ---

    public static class CPU implements Serializable {
        private static final long serialVersionUID = 1L;
        public String Name;
        public String Vendor;
        public String MicroArch;
        public int PhysicalCore;
        public int LogicalCore;
        public int BenchMarkScore = 0;
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