package scheduler2022;

import java.util.Arrays;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.Util;

//2022 KOUHEI SEKI <OperatingSystem Hardware & System Information>

public class Cpuload{
	SystemInfo si = new SystemInfo();
	HardwareAbstractionLayer hal = si.getHardware();
	
	
	public double cpu_load() {
		long[] prevTicks = hal.getProcessor().getSystemCpuLoadTicks();
		System.out.println("TEST cpu_load class Arrays:"+Arrays.toString(prevTicks));
		// Wait a second...
        Util.sleep(1000);
		System.out.println("TEST cpu_load class return:"+hal.getProcessor().getSystemCpuLoadBetweenTicks(prevTicks)*100);
		return (hal.getProcessor().getSystemCpuLoadBetweenTicks(prevTicks)*100);
		
	}
	
	public double[] cpu_load_average() {
		return (hal.getProcessor().getSystemLoadAverage(3));
	}
}