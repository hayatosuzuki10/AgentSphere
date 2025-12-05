package sphereConcurrent;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SphereFuture<V> implements Future<V>,Comparable<SphereFuture<V>>,Serializable{
	/**
	 *
	 */
	private static final long serialVersionUID = 100L;

	private Callable<V> callable;
	private V result;
	private UUID uuid;
	public static enum Status{RUNNING,TERMINATED,CANCELLED};
	private Status status;
	Exception exception;


	SphereFuture(SphereTask<V> sphereTask) {
		// TODO 自動生成されたコンストラクター・スタブ
		this.callable = sphereTask.callable;
		this.uuid = sphereTask.uuid;
		this.status = Status.RUNNING;
	}

	/**
	 * @deprecated
	 */
	@Override
	public boolean cancel(boolean flag) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	/**
	 *
	 */
	@Override
	public synchronized V get() throws InterruptedException, ExecutionException {
		// TODO 自動生成されたメソッド・スタブ
		while(!isDone())wait();
		return this.result;
	}

	/**
	 * @deprecated
	 */
	@Override
	public V get(long l, TimeUnit timeunit) throws InterruptedException,
			ExecutionException, TimeoutException {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/**
	 * @deprecated
	 */
	@Override
	public boolean isCancelled() {
		// TODO 自動生成されたメソッド・スタブ
		return status.equals(Status.CANCELLED);
	}

	@Override
	public synchronized boolean isDone() {
		// TODO 自動生成されたメソッド・スタブ
		return status.equals(Status.TERMINATED);
	}

	@Override
	public synchronized int compareTo(SphereFuture<V> o) {
		// TODO 自動生成されたメソッド・スタブ
		return this.uuid.compareTo(o.uuid);
	}

	synchronized boolean setResult(SphereTask<V> sphereTask){
		if(sphereTask == null) throw new NullPointerException();
		if( !this.uuid.equals(sphereTask.uuid)
				|| !this.status.equals(Status.RUNNING)
				|| !sphereTask.status.equals(Status.TERMINATED)){
			this.notifyAll();
			return false;
		}
		this.result = sphereTask.result;
		this.status = sphereTask.status;
		this.exception = sphereTask.exception;
		this.notifyAll();
		return true;
	}

	public Callable<V> getCallable() {
		return callable;
	}

}
