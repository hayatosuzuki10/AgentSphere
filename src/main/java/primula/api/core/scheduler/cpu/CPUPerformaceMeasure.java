package primula.api.core.scheduler.cpu;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public class CPUPerformaceMeasure {
	private static List<CPUtime> cputimes = new ArrayList();
	private static long openningIncrement;
	
	public long measureCPUperfo() {
		return measureAverageCPUusage() * this.openningIncrement;
	}
	
	private long measureAverageCPUusage() {
		long averageCPUusage = 0;
		for(CPUtime cputime : cputimes) {
			cputime.update();
			long CPUusage = cputime.getProbability(500000000);
			averageCPUusage += CPUusage;
		}
		return averageCPUusage / cputimes.size();
	}
	
	public static void add(Thread agentThread) {
		CPUtime newAgentThread = new CPUtime(agentThread);
		cputimes.add(newAgentThread);
	}
	
	
    // 追加 okubo 2013/6/26
    // システム起動時のマシンの性能値を測定
    public static void measureFirstCPUperformance() {
    	long perfo = 0;
    	ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    	long measureTime = System.currentTimeMillis() + 500;
    	long start = threadMXBean.getCurrentThreadCpuTime();
    	while(System.currentTimeMillis() < measureTime) {
    		perfo++;
    	}
    	long stop = threadMXBean.getCurrentThreadCpuTime();
    	long time = stop - start;
    	openningIncrement =  (perfo*1000000000)/time;
    	System.err.println(time + " " + start + " " + stop);
    }
    
    public static long getOpenningIncrement() {
    	return openningIncrement;
    }
}
