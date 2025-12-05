package scheduler2022;

public class Iperf3Result {
	public End end;

    public static class End {
        public Sum sum_sent;
        public Sum sum_received;
    }

    public static class Sum {
        public long bytes;
        public double bits_per_second;
    }
}
