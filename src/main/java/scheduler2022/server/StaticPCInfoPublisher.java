package scheduler2022.server;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import primula.util.IPAddress;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;

public class StaticPCInfoPublisher {
	/** SchedulerMessageServer が listen しているポート */
    private final int schedulerPort = 8888;


    /** 前回送信した情報（差分検出用） */
    private Set<String> allIPAddress = Scheduler.getAliveIPs();

    public StaticPCInfoPublisher() {
    }


    /* ============================
     * メインAPI
     * ============================ */

    /**
     * 新しい DynamicPCInfo を通知
     * 変化が大きい場合のみ送信する
     */
    public void publish(StaticPCInfo current) {
        if (current == null) return;

        broadcast(current);
    }

    /* ============================
     * 内部処理
     * ============================ */

    private void broadcast(StaticPCInfo spi) {

        String myIp = IPAddress.myIPAddress;
        StaticPCInfoUpdate msg = new StaticPCInfoUpdate(myIp, spi);

        // 1回だけ取得する（ここ重要）
        Set<String> currentIPs = Scheduler.getAliveIPs();
        if (currentIPs == null) return;

        Set<String> addedIPs;
        Set<String> removedIPs;

        synchronized (this) {
            // added = current - prev
            addedIPs = new HashSet<>(currentIPs);
            addedIPs.removeAll(allIPAddress);

            // removed = prev - current
            removedIPs = new HashSet<>(allIPAddress);
            removedIPs.removeAll(currentIPs);

            // 最後に更新（ここ重要）
            allIPAddress = new HashSet<>(currentIPs);
        }

        // 追加分へ送信
        for (String ip : addedIPs) {
            sendOne(ip, msg);
        }

        // 消えた分をSchedulerから削除
        for (String ip : removedIPs) {
            Scheduler.getSpis().remove(ip);
            Scheduler.getDpis().remove(ip);
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
                "[StaticPCInfoPublisher] send failed to "
                + ip + " : " + e.getMessage()
            );
        }
    }
}
