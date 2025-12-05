package primula.api.core.scheduler.vmemory;

public class VMemoryPerformanceMeasure {

	/**
	 * 仮想メモリ残量を測定して結果を返す 2014.11.18
	 * @return
	 */
	public long measureVMemoryPerfo(){
		///1000000で割って％にしてください
		return 100000000*(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/Runtime.getRuntime().maxMemory();
	}
	
}
