package sphereConcurrent;

import java.io.Serializable;
import java.util.UUID;

public interface SphereExecutorService extends Serializable{
	public static enum Status{RUNNING,SHUTDOWN,STOP,TERMINATED}
	public <T> SphereFuture<T> submit(SphereCallable<T> sphereCallable);
	public void shutdown();
	boolean addExecutor(String executorId);
	public UUID getId();
}
