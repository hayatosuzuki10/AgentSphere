

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;
import primula.api.core.assh.command.demo;
import primula.util.IPAddress;
import scheduler2022.RecommendedDest;
import scheduler2022.Scheduler;

public class DemoAgent extends AbstractAgent {

    public int durationInSeconds = 180;
    public long startTime = System.currentTimeMillis();
    public long endTime;
    public long updateTime = 5;
    public String defaltDest = IPAddress.myIPAddress;
    public List<String> route = new ArrayList<>();

    @Override
    public @continuable void run() {
        startTime = System.currentTimeMillis();
        long endTime = startTime + durationInSeconds * 1000L;
        long migrateTime = startTime + updateTime * 1000L;

        long counter = 0;
        System.out.println("DemoAgent loaded from = " +
        	    DemoAgent.class.getProtectionDomain().getCodeSource().getLocation());
        System.out.println("[DemoAgent] 開始: " + getAgentID() + ", 実行時間 = " + durationInSeconds + " 秒");

        while(System.currentTimeMillis() < endTime) {
            // 簡単な計算：インクリメント
        	double dummy = 0;
            for (int i = 0; i < 10_000_000; i++) {
                dummy += Math.sqrt(i);
            }
            //System.out.println(shouldMove);
            shouldMove = Scheduler.getStrategy().shouldMove(this);
            nextDestination = Scheduler.getStrategy().getDestination(this);

            // Strategy2022 のときだけ上書きしたいなら「{}」必須
            if (Scheduler.getStrategy().getClass().getName().contains("Strategy2022")) {
                nextDestination = RecommendedDest.recomDest(this.getAgentName());
            }
            if (shouldMove
            	    && migrateTime < System.currentTimeMillis()
            	    && !IPAddress.myIPAddress.equals(nextDestination)) {
            	System.out.println("will move");
            	route.add(IPAddress.myIPAddress);
            	System.out.println("[DemoAgent] 強制移動。移動先: " + nextDestination);
            	migrate(nextDestination);
            	System.out.println("I'm migarted here!!");
            	migrateTime = System.currentTimeMillis() + updateTime * 1000L;
            	shouldMove = false;
            }
        }
        migrate(defaltDest);
        demo.agentsAlive += 1;
        //demo.routes.add(route);
        System.out.println("[DemoAgent] 終了!!!: " + getAgentID() + ", カウント = " + counter);
        
        return ;
    }

    /**
     * 外部からの停止命令（未使用でもOK）
     */
    @Override
    public void requestStop() {
        System.out.println("[DemoAgent] requestStop() 呼び出されました。");
    }
    

}