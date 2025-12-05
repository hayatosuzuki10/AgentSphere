package scheduler2022;

import java.io.Serializable;
import java.lang.management.ManagementFactory;

import com.sun.management.ThreadMXBean;


public class MemoryMeasure implements Serializable{
	ThreadMXBean thread =(com.sun.management.ThreadMXBean)
			ManagementFactory.getThreadMXBean();
	public long memory_measure(long id) {
		return thread.getThreadAllocatedBytes(id);
	}
}
