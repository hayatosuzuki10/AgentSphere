package scheduler2022;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

//2022 KOUHEI SEKI <OperatingSystem Hardware & System Information>

public class ClockSpeed {
	static SystemInfo si = new SystemInfo();
	static HardwareAbstractionLayer hal = si.getHardware();

	public static long getClockSpeed() {
		long freq = hal.getProcessor().getProcessorIdentifier().getVendorFreq();
		return freq;
	}
	//定格クロック

	public static long getMaxClockSpeed() {
		long freq = hal.getProcessor().getMaxFreq();
		return freq;
	}
	//OC時の最高クロック

	public static long getCurrentClockSpeed() {
		long freqs[] = hal.getProcessor().getCurrentFreq();
		long freqs_ave = 0;
		for(int i = 0; i < freqs.length; i++) {
			freqs_ave = freqs_ave + freqs[i];
		}
		return (freqs_ave/freqs.length);
	}
	//全コアのクロックの平均
}
