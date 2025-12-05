package primula.api.core.scheduler.cpu;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * スレッドのCPU時間を保持するクラス
 * @author Daisuke
 *
 */
public class CPUtime {
	private static ThreadMXBean mxbean;
	
	private long threadid;//観測対象のスレッドID
	private long lasttime;//最後に測った時のCPU時間
	private long unittime;//最後とその前に測った時の差のCPU時間
	
	static{
		mxbean = ManagementFactory.getThreadMXBean();
	}
	
	/**
	 * 引数なしコンストラクタ。
	 * <pre>
	 * 今のところclone用なのでprivateです。
	 * </pre>
	 */
	private CPUtime(){
		;
	}
	
	/**
	 * コンストラクタ
	 * <pre>
	 * 観測対象スレッドの設定と、１回目のデータを取る
	 * @param threadid 対象のスレッドID
	 */
	public CPUtime(long threadid){

		this.threadid = threadid;
		this.lasttime = mxbean.getThreadUserTime(this.threadid);
		this.unittime = lasttime;
	}
	
	/**
	 * コンストラクタ
	 * <pre>
	 * 引数のスレッドのIDを取りスレッドIDから作るコンストラクタを起動する
	 * </pre>
	 * @param thread 対象のスレッド
	 */
	public CPUtime(Thread thread){
		this(thread.getId());
	}
	
	/**
	 * 観測対象スレッドの状態を確認し、保持している状態を更新する
	 * @return 書き換わった自分を返す
	 */
	public CPUtime update(){
		long now = mxbean.getThreadUserTime(this.threadid);
		this.unittime = now - this.lasttime;
		this.lasttime = now;
		return this;		
	}
	
	/**
	 * 単位時間当たりのCPU使用時間を引数の値で割った百分率を返す。
	 * <pre>
	 * 引数に計測間隔（ナノ秒）を指定することで直近単位時間でのCPU使用率を手に入れることができる
	 * </pre>
	 * @param interval
	 * @return 百分率にした値。
	 */
	public long getProbability(long interval){
		return unittime*100/interval;
	}
	
	/**
	 * クローンを作る
	 * @return クローン
	 */
	public CPUtime clone(){
		CPUtime clone = new CPUtime();
		clone.threadid = this.threadid;
		clone.lasttime = this.lasttime;
		clone.unittime = this.unittime;
		return clone;
	}
	
	public String toString(){
		return new String("ThreadID:"+this.threadid+", LastTime:"+this.lasttime+", UnitTime:"+this.unittime); 
	}
	
	//ゲッター
	public long getThreadid(){
		return this.threadid;
	}
	
	public long getLasttime() {
		return this.lasttime;
	}

	public long getUnittime() {
		return this.unittime;
	}
}

