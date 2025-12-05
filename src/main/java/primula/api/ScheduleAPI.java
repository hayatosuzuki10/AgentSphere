package primula.api;

import java.net.InetAddress;

import primula.api.core.scheduler.ScheduleThread;

/**
 * 古いスケジューラAPIです
 * 2023年現在で新しくスケジューラ機能を使用したい場合はScheduler2022パッケージで開発されているものを活用してください
 * @author selab
 *
 */
@Deprecated public class ScheduleAPI {


    public synchronized static InetAddress getHighPerformancedMachine() {
    	return ScheduleThread.adviseWhereAgentShouldMigrate();
    }

    public synchronized static double getVMemory(){
    	return ScheduleThread.getMyMachineInfo().getVMperfo();
    }

}
