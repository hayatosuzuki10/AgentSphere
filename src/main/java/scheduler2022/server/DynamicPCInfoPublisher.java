package scheduler2022.server;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;

/**
 * DynamicPCInfo を他ノードへ配信する送信専用クラス
 *
 * 役割:
 *  - 自PCの DynamicPCInfo を保持
 *  - 前回値と比較して「変化があった場合のみ」送信
 *  - SchedulerMessageServer へ ObjectStream で送信
 */
public class DynamicPCInfoPublisher {

    /** SchedulerMessageServer が listen しているポート */
    private final int schedulerPort = 8888;


    /** 前回送信した情報（差分検出用） */
    private DynamicPCInfo lastSent;

    public DynamicPCInfoPublisher() {
    }


    /* ============================
     * メインAPI
     * ============================ */

    /**
     * 新しい DynamicPCInfo を通知
     * 変化が大きい場合のみ送信する
     */
    public void publishIfChanged(DynamicPCInfo current) {
        if (current == null) return;
        long now = System.currentTimeMillis();
        
        if(
    	        lastSent != null 
    	        && lastSent.timeStanp + Scheduler.getTimeStampExpire() > now
    	        && lastSent.isForecast
    	        && !current.isForecast) {
    	        return;
    	    }

        if (lastSent == null || current.hasSignificantChange(lastSent)) {
            broadcast(current);
            lastSent = current.deepCopy(); // copy() 推奨（なければ clone）
        }
    }

    /* ============================
     * 内部処理
     * ============================ */

    private void broadcast(DynamicPCInfo dpi) {
    	String myIp = IPAddress.myIPAddress;
        DynamicPCInfoUpdate msg =
                new DynamicPCInfoUpdate(myIp, dpi);

        for (String ip : Scheduler.getAliveIPs()) {
            if (ip.equals(IPAddress.myIPAddress)) continue;
            sendOne(ip, msg);
        }
    }

    private void sendOne(String ip, Serializable msg) {
        try (Socket socket = new Socket(ip, schedulerPort);
             ObjectOutputStream out =
                     new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(msg);
            out.flush();

        } catch (Exception e) {
            // ノードが落ちてるのは普通に起こる
            System.err.println(
                "[DynamicPCInfoPublisher] send failed to "
                + ip + " : " + e.getMessage()
            );
        }
    }

}