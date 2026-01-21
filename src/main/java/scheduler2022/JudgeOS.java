package scheduler2022;

import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

//2022 KOUHEI SEKI <OperatingSystem Hardware & System Information>

public class JudgeOS {
	static SystemInfo si = new SystemInfo();
	static OperatingSystem os = si.getOperatingSystem();
	
	public static String OSname() {
		return String.valueOf(os);
	}
	
	public static boolean isWindows() {
		return OSname().contains("Windows");
	}
	
	public static boolean isLinux() {
		return OSname().contains("Linux");
	}
	
	public static boolean isMac() {
		return OSname().contains("Mac");
	}
	
}
