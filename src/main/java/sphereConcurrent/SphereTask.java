package sphereConcurrent;

import java.io.Serializable;
import java.util.UUID;

import sphereConcurrent.SphereFuture.Status;

/**
 * 転送用タスク
 * @author Owner
 *
 * @param <V> 答えの型
 */
public class SphereTask<V> implements Serializable{
	/**
	 *
	 */
	private static final long serialVersionUID = 100L;
	SphereCallable<V> callable;
	UUID uuid;
	V result;
	Status status;
	Exception exception;

	SphereTask(SphereCallable<V> sphereCallable) {
		// TODO 自動生成されたコンストラクター・スタブ
		callable = sphereCallable;
		uuid = UUID.randomUUID();
		status = Status.RUNNING;
	}

	boolean execute(){
		try {
			this.result = callable.call();
			status = Status.TERMINATED;
			return true;
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			this.exception = e;
			e.printStackTrace();
		}
		return false;
	}

}
