/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.scheduler;


/**
 *
 * @author kurosaki
 */
public class PerformanceMeasure {

//	private static long openningIncrement;
    private long time;
    private long perfo;
    private long preperfo;
    private boolean flag;

    public PerformanceMeasure(){
            time = 0;
            perfo = 0;
            preperfo = -1;
            flag = false;
    }

/******************
 *性能値測定関数。*
 ******************/
    public long measurePerfo(){
        perfo = 0;
        time = System.currentTimeMillis() + 100;

        while(System.currentTimeMillis() < time){
          	perfo++;
        }

        if(preperfo == -1){
            preperfo = perfo;
        }
        else if(perfo < preperfo){
            perfo = (perfo + preperfo) / 2;
            preperfo = perfo;
            flag = true;
        }

        return perfo;
    }

    public void flagSet(){
        this.flag = false;
    }

    public boolean flagGet(){
        return this.flag;
    }

    /*
    // 追加 okubo 2013/6/26
    // システム起動時のマシンの性能値を測定
    public void measureFirstCPUperformance() {
    	perfo = 0;
    	ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    	time = System.currentTimeMillis() + 500;
    	long start = threadMXBean.getCurrentThreadCpuTime();
    	while(System.currentTimeMillis() < time) {
    		perfo++;
    	}
    	long stop = threadMXBean.getCurrentThreadCpuTime();
    	long CPUtime = stop - start;


    	openningIncrement =  perfo/CPUtime;
    }

    public static long getOpenningIncrement() {
    	return openningIncrement;
    }
    */

    // 仮想メモリ割合＝(総メモリ量 - 空きメモリ量)/最大メモリ量
    public static double measureVMemory() {
    	return 100000000*(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/Runtime.getRuntime().maxMemory();
    }
}

