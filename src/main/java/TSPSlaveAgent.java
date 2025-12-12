import java.net.InetAddress;

import primula.agent.AbstractAgent;
import primula.agent.util.DHTutil; // Agent用の DHTutil
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;

public class TSPSlaveAgent extends AbstractAgent implements IMessageListener {

    int numCities;
    int start, end;
    int[][] distanceMatrix;
    boolean received;
    long sleepMillis = 1000L;
    int workerId;

    boolean shouldStop = false;

    int bestCost = Integer.MAX_VALUE;
    int[] bestPath;

    // マスターの ID (StrictName = AgentID)
    String masterId;

    @Override
    public void run() {
        try {
            // メッセージリスナ登録
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("[TSPSlave] started. id=" + getStrictName());

        // マスターから問題データが届くまで待機
        while (!received) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("[TSPSlave] problem received. workerId=" + workerId
                + " range=[" + start + "," + end + ")");

        // TSP 探索
        solveTSP();

        // 結果をマスターへ返す
        sendResultToMaster();

        System.out.println("[TSPSlave] finished. workerId=" + workerId
                + " bestCost=" + bestCost);
    }

    /**
     * マスターへ計算結果を返す。
     * フォーマットは TSPMasterAgent の receivedMessage が期待している int[][]:
     *   data[0][0] = workerId
     *   data[0][1] = bestCost
     *   data[1]    = bestPath[]
     */
    private void sendResultToMaster() {
        if (bestPath == null) {
            System.out.println("[TSPSlave] bestPath が null のため結果送信をスキップします");
            return;
        }
        if (masterId == null) {
            System.out.println("[TSPSlave] masterId が null のため結果送信先が分かりません");
            return;
        }

        // 送信データ構築
        int[][] result = new int[2][];
        result[0] = new int[2];
        result[0][0] = workerId;
        result[0][1] = bestCost;
        result[1] = bestPath.clone();

        try {
            // マスターの IP を DHT から取得
            InetAddress masterIp = DHTutil.getAgentIP(masterId);
            if (masterIp == null) {
                System.out.println("[TSPSlave] masterIp が DHT から取れませんでした: masterId=" + masterId);
                return;
            }

            KeyValuePair<InetAddress, Integer> ip =
                    new KeyValuePair<>(masterIp, 55878);

            StandardEnvelope env = new StandardEnvelope(
                    new AgentAddress(masterId),
                    new StandardContentContainer(result)
            );

            MessageAPI.send(ip, env);
            System.out.println("[TSPSlave] result sent to master. masterId=" + masterId
                    + " bestCost=" + bestCost);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * このスレーブが担当する 2 番目の都市の範囲 [start, end) について
     * TSP を DFS + 分枝限定で全探索し、自分の中での最良経路を bestCost / bestPath に記録する。
     */
    private void solveTSP() {
        bestCost = Integer.MAX_VALUE;
        bestPath = null;

        if (distanceMatrix == null || numCities <= 1) {
            System.out.println("[TSPSlave] distanceMatrix が不正なので探索をスキップします");
            return;
        }

        boolean[] used = new boolean[numCities];
        int[] path = new int[numCities];

        // 出発都市を 0 に固定
        path[0] = 0;
        used[0] = true;

        // このスレーブが担当する「2番目の都市」の候補だけを試す
        for (int city = start; city < end && city < numCities; city++) {
            if (shouldStop) {
                System.out.println("[TSPSlave] shouldStop=true で中断します");
                break;
            }

            if (used[city]) {
                continue;
            }

            path[1] = city;
            used[city] = true;

            int cost01 = distanceMatrix[0][city];

            // 深さ = 2, 現在コスト = cost01 から DFS 開始
            dfs(path, used, 2, cost01);

            used[city] = false;
        }

        System.out.println("[TSPSlave] solveTSP end. workerId=" + workerId
                + " bestCost=" + bestCost);
    }

    /**
     * 深さ depth まで path[0..depth-1] が埋まっている状態で、
     * 未訪問都市をすべて試しながら完全な巡回路を探す。
     */
    private void dfs(int[] path, boolean[] used, int depth, int currentCost) {
        if (shouldStop) return;

        // すでに現状コストが最良解以上なら、これ以上伸ばしても意味がないので枝刈り
        if (currentCost >= bestCost) {
            return;
        }

        // 全都市を訪問済み → スタート(0)に戻って閉路を完成
        if (depth == numCities) {
            int last = path[numCities - 1];
            int totalCost = currentCost + distanceMatrix[last][0]; // 0 に戻る

            if (totalCost < bestCost) {
                bestCost = totalCost;
                bestPath = path.clone();
                System.out.println("[TSPSlave] workerId=" + workerId
                        + " newBestCost=" + bestCost);
            }
            return;
        }

        int last = path[depth - 1];

        // 次に行く都市候補を総当たり
        for (int next = 0; next < numCities; next++) {
            if (used[next]) continue;

            int nextCost = currentCost + distanceMatrix[last][next];

            // ここでも簡単な枝刈り
            if (nextCost >= bestCost) continue;

            path[depth] = next;
            used[next] = true;

            dfs(path, used, depth + 1, nextCost);

            used[next] = false;
        }
    }

    @Override
    public String getStrictName() {
        return this.getAgentID();
    }

    @Override
    public String getSimpleName() {
        return this.getAgentName();
    }

    @Override
    public void receivedMessage(AbstractEnvelope envelope) {
        System.out.println("[TSPSlave] receivedMessage");

        StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
        Object raw = cont.getContent();

        // ====== TSPMasterAgent.TSPMasterMessage を解釈する ======
        TSPMasterAgent.TSPMasterMessage msg;

        if (raw instanceof TSPMasterAgent.TSPMasterMessage) {
            msg = (TSPMasterAgent.TSPMasterMessage) raw;
        } else if (raw instanceof Object[]) {
            Object[] arr = (Object[]) raw;
            if (arr.length == 0 || !(arr[0] instanceof TSPMasterAgent.TSPMasterMessage)) {
                System.err.println("[TSPSlave] Unexpected content structure (Object[] but no TSPMasterMessage). first="
                        + (arr.length > 0 && arr[0] != null ? arr[0].getClass() : "null"));
                return;
            }
            msg = (TSPMasterAgent.TSPMasterMessage) arr[0];
        } else {
            System.err.println("[TSPSlave] Unexpected content type: "
                    + (raw == null ? "null" : raw.getClass()));
            return;
        }

        // メッセージからフィールドをセット
        this.numCities      = msg.numCities;
        this.workerId       = msg.workerId;
        this.start          = msg.start;
        this.end            = msg.end;
        this.distanceMatrix = msg.distanceMatrix;
        this.masterId       = msg.masterId;

        this.received = true;

        System.out.printf("[TSPSlave] init done. workerId=%d, cities=%d, range=[%d,%d), masterId=%s%n",
                workerId, numCities, start, end, masterId);
    }

    @Override
    public void requestStop() {
        shouldStop = true;
    }
}