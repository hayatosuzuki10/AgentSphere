import primula.agent.AbstractAgent;

/**
 * @author Mikamiyama
 */

//JVM内のメモリ使用量等を出力するエージェントプログラム。

public class MemoryUsageAgent extends AbstractAgent{
	public void run(){
		while(true){
			long total=Runtime.getRuntime().totalMemory();     //JVMのメモリー総容量
			long free=Runtime.getRuntime().freeMemory();     //JVM内の空きメモリー量
			long used=total-free;     //JVMのメモリー使用量

			System.out.println("total => "+total/(1024*1024)+"MB");
			System.out.println("free  => "+free/(1024*1024)+"MB");
			System.out.println("used  => "+used/(1024*1024)+"MB");
			System.out.println();     //改行

			try{
				Thread.sleep(5000);     //5秒ごとに計測して表示
			}catch(InterruptedException e){
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}