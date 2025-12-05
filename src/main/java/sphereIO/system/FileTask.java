package sphereIO.system;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ファイルシステムが送信した要求の返答を待つためのFutureです
 * <pre>
 * 大体の動作はFutureTaskクラスをイメージしてます
 * cancelとかは適当です
 * setException的なんかはコンテナのタグerrorにしちゃえばいいよねよくない
 * というかそのまんまFutureTaskクラス使えばよかったのでは？ボブは訝しんだ
 * </pre>
 * @author Ranton
 *
 */
public class FileTask implements Future<SphereFileContentContainer> {

	volatile private boolean cancelled;
	volatile private boolean done;
	volatile private boolean running;
	volatile private SphereFileContentContainer result;

	public FileTask() {
		cancelled = false;
		done = false;
		running = false;
		result = null;
	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (done)
			return false;

		if (!mayInterruptIfRunning && running) {
			return false;
		}
		cancelled = true;
		done = true;
		return true;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	/**
	 * このFileTaskにセットされた実行の結果を返します
	 * <p>
	 * 実行の際に例外が発生していた場合それを含んだExecutionExceptionをスローします
	 * @return 結果を保持するSphereFileContentContainer
	 * @throws InterruptedException 完了までに割り込みが発生した場合
	 * @throws ExecutionException 実行中に例外が発生していた場合
	 */
	public synchronized SphereFileContentContainer get() throws InterruptedException, ExecutionException {
		running = true;
		while (!this.isDone()) {
			if (cancelled) {
				throw new CancellationException();
			}
			this.wait();
		}
		if(result.getException()!=null) {
			throw new ExecutionException(result.getException());
		}
		return result;

	}

	@Override
	public SphereFileContentContainer get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException("まだつくってない");
	}

	public void set(SphereFileContentContainer data) {
		synchronized (this) {
			if (!done) {
				result = data;
				done = true;
				this.notifyAll();
			}
		}
	}

}
