import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Scanner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.agent.loader.multiloader.ChainContainer;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.agent.loader.multiloader.StringSelector;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;

/**
 * TSP を複数のスレーブエージェントに分散して解かせるマスターエージェント。
 *
 * 役割:
 *  - TSP 問題をファイルから読み込む
 *  - 2番目の都市の候補を numWorkers 個に分割して担当を振る
 *  - スレーブエージェントを動的ロード & 起動し、初期データを送信
 *  - スレーブから結果を受信し、全ワーカーの完了を待つ
 *  - 最終的な最短経路とコストをログに出す
 */
public class TSPMasterAgent extends AbstractAgent implements IMessageListener {

    /** TSP インスタンスファイルパス（相対パス） */
    private String fileName = "others/tsp100.txt";

    /** 全ワーカー終了確認用のポーリング間隔(ms) */
    private long sleepMillis = 1000L;

    /** 距離行列 */
    private int[][] distanceMatrix;

    /** 都市数（distanceMatrix.length） */
    private int numCities;

    /** ワーカー数（スレーブエージェント数） */
    private int numWorkers = 8;

    /** 動的ロードしたスレーブインスタンス */
    private AbstractAgent[] workers = new AbstractAgent[numWorkers];

    /**
     * 各ワーカーの担当範囲 [start, end) を保持する配列。
     * workerRanges[w][0] = start, workerRanges[w][1] = end
     */
    private int[][] workerRanges;

    /** スレーブエージェントのクラス名（bin 直下想定） */
    private String slaveAgentClassName = "TSPSlaveAgent";

    /** 各ワーカーが結果を返したかどうか */
    private boolean[] taskHasFinished = new boolean[numWorkers];

    /** 現時点での最良コスト */
    private int bestCost = Integer.MAX_VALUE;

    /** 現時点での最良経路 */
    private int[] bestPath;

    /** 全ワーカ終了フラグ（受信スレッドからも触るので volatile） */
    private volatile boolean finished = false;


    @Override
    public void run() {

        System.out.println("[TSPMaster] START: id=" + getAgentID() + ", name=" + getAgentName());

        // メッセージ受信リスナ登録
        try {
            MessageAPI.registerMessageListener(this);
            System.out.println("[TSPMaster] MessageListener registered.");
        } catch (Exception e) {
            System.err.println("[TSPMaster] Failed to register MessageListener.");
            e.printStackTrace();
        }

        // TSP インスタンス読み込み
        try {
            distanceMatrix = loadTSPProblem(fileName);
            numCities = distanceMatrix.length;
            System.out.println("[TSPMaster] TSP file loaded: " + fileName
                    + "  cities=" + numCities);
        } catch (Exception e) {
            System.err.println("[TSPMaster] ERROR: failed to load TSP problem.");
            e.printStackTrace();
            return; // これ以上進めないので終了
        }

        // 探索空間（2番目の都市の候補）をワーカー数で分割
        workerRanges = divideSearchSpace(distanceMatrix);
        for (int w = 0; w < numWorkers; w++) {
            System.out.printf("[TSPMaster] Worker %d range = [%d, %d)%n",
                    w, workerRanges[w][0], workerRanges[w][1]);
        }

        // ここではマスター自体は移動しない前提
        // （移動させたい場合は、メッセージルーティングを DHT 経由でちゃんと設計すること）

        // スレーブエージェントを起動して仕事を投げる
        String masterId = getStrictName();
        InetAddress masterIp = getmyIP();
        System.out.println("[TSPMaster] Master runs on IP=" + masterIp.getHostAddress()
                + " id=" + masterId);

        for (int w = 0; w < numWorkers; w++) {

            int start = workerRanges[w][0];
            int end   = workerRanges[w][1];

            System.out.printf("[TSPMaster] Spawning worker %d (start=%d, end=%d)%n",
                    w, start, end);

            Object agentInstance = loadAgentInstance(slaveAgentClassName);
            if (agentInstance instanceof AbstractAgent) {
                AbstractAgent agent = (AbstractAgent) agentInstance;

                // この Sphere 上で起動
                AgentAPI.runAgent(agent);
                workers[w] = agent;

                // スレーブに渡すパケット構造:
                // pack[0][0][0] = numCities
                // pack[0][0][1] = workerId
                // pack[1][0][0] = startIndex
                // pack[1][0][1] = endIndex
                // pack[2]       = distanceMatrix
                
                TSPMasterMessage message = new TSPMasterMessage();
                message.numCities = this.numCities;
                message.workerId = w;
                message.start = start;
                message.end = end;
                message.distanceMatrix = this.distanceMatrix;
                message.masterId = this.getAgentID();
                
                int[][][] pack = new int[3][numCities][numCities];
                pack[0][0][0] = numCities;
                pack[0][0][1] = w;
                pack[1][0][0] = start;
                pack[1][0][1] = end;
                pack[2] = distanceMatrix;

                // マスターの strictName も一緒に渡す（結果返送先）
                Object[] payload = new Object[2];
                payload[0] = pack;
                payload[1] = masterId;

                try {
                    KeyValuePair<InetAddress, Integer> ip =
                            new KeyValuePair<>(agent.getmyIP(), 55878);
                    StandardEnvelope env = new StandardEnvelope(
                            new AgentAddress(agent.getAgentID()),
                            new StandardContentContainer(message)
                    );
                    MessageAPI.send(ip, env);
                    System.out.printf(
                            "[TSPMaster] Sent task to worker %d, agentId=%s, ip=%s%n",
                            w, agent.getAgentID(), agent.getmyIP().getHostAddress()
                    );
                } catch (Exception ex) {
                    System.err.println("[TSPMaster] ERROR: Failed to send init task to worker " + w);
                    ex.printStackTrace();
                }

            } else {
                System.err.println("[TSPMaster] Failed to instantiate slave agent for worker " + w);
            }
        }

        // 全ワーカが終わるまで待機
        System.out.println("[TSPMaster] Waiting for all workers to finish...");
        while (!this.finished) {
            try {
                Thread.sleep(this.sleepMillis);
            } catch (InterruptedException e) {
                System.err.println("[TSPMaster] Sleep interrupted.");
                e.printStackTrace();
            }
        }

        // 最終結果のログ出力
        System.out.println("[TSPMaster] All workers finished.");
        System.out.println("[TSPMaster] Best cost = " + bestCost);
        if (bestPath != null) {
            System.out.print("[TSPMaster] Best path: ");
            for (int i = 0; i < bestPath.length; i++) {
                System.out.print(bestPath[i]);
                if (i + 1 < bestPath.length) System.out.print(" -> ");
            }
            System.out.println();
        } else {
            System.out.println("[TSPMaster] Warning: bestPath is null.");
        }

        System.out.println("[TSPMaster] END: id=" + getAgentID());
    }

    /**
     * GhostClassLoader を使って、bin フォルダからエージェントクラスをロード・インスタンス化する。
     */
    private Object loadAgentInstance(String className) {
        Object obj = null;
        Class<?> cls;
        ChainContainer cc;
        GhostClassLoader gcl;
        String path = ".\\bin";

        gcl = GhostClassLoader.unique;
        cc = gcl.getChainContainer();

        try {
            cc.resistNewClassLoader(new StringSelector(path), new File(path));
        } catch (IOException e) {
            Logger.getLogger(getClass()).log(Level.TRACE, null, e);
        }

        try {
            cls = gcl.loadClass(className);
            obj = cls.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            System.err.println("[TSPMaster] Failed to load class: " + className);
            e.printStackTrace();
        }

        return obj;
    }

    /**
     * TSP 問題をファイルから読み込む。
     * フォーマット:
     *   1行目: 都市数 n
     *   2行目以降: n × n 行列（距離）
     */
    private int[][] loadTSPProblem(String fileName) throws Exception {
        File f = new File(fileName);
        if (!f.exists()) {
            throw new IllegalArgumentException(
                    "TSP file not found: " + f.getAbsolutePath());
        }

        System.out.println("[TSPMaster] Loading TSP from: " + f.getAbsolutePath());

        try (FileInputStream in = new FileInputStream(f);
             Scanner sc = new Scanner(in)) {

            int n = sc.nextInt();
            int[][] d = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    d[i][j] = sc.nextInt();
                }
            }
            return d;
        }
    }

    /**
     * 2番目の都市の候補（1..cities-1）を numWorkers 個にほぼ均等に分割する。
     * 返り値 ranges[w] = {start, end} （end は排他的）
     */
    private int[][] divideSearchSpace(int[][] dist) {
        int cities = dist.length;
        int choices = cities - 1; // 0番以外の都市数

        int[][] ranges = new int[numWorkers][2];

        int base = choices / numWorkers;   // 各 worker の基本担当数
        int rem  = choices % numWorkers;   // 余りを前から順に +1 して配る

        int cur = 1; // 2番目都市の候補は 1..(cities-1)

        for (int w = 0; w < numWorkers; w++) {
            int size = base + (w < rem ? 1 : 0);

            ranges[w][0] = cur;        // inclusive
            ranges[w][1] = cur + size; // exclusive

            cur += size;
        }

        return ranges;
    }

    /**
     * 新しく届いた経路・コストが、現在のベストより良ければ更新する。
     */
    private synchronized void compareBestCost(int[] newPath, int newCost) {
        if (newCost < this.bestCost) {
            System.out.printf("[TSPMaster] Best updated: oldCost=%d -> newCost=%d%n",
                    this.bestCost, newCost);
            this.bestCost = newCost;
            this.bestPath = newPath.clone();
        }
    }

    @Override
    public void requestStop() {
        System.out.println("[TSPMaster] requestStop() called.");
    }

    @Override
    public String getStrictName() {
        return this.getAgentID();
    }

    @Override
    public String getSimpleName() {
        return this.getAgentName();
    }

    /**
     * スレーブから結果メッセージを受信したときに呼ばれる。
     *
     * 想定フォーマット:
     *   int[][] data
     *   data[0][0] = workerId (0..numWorkers-1)
     *   data[0][1] = bestCost
     *   data[1]    = bestPath（経路）
     */
    @Override
    public void receivedMessage(AbstractEnvelope envelope) {
        System.out.println("[TSPMaster] Received message.");

        try {
            StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
            int[][] newData = (int[][]) cont.getContent();

            int workerId = newData[0][0];
            int newCost  = newData[0][1];
            int[] newPath = newData[1];

            System.out.printf(
                    "[TSPMaster] Result from worker %d: cost=%d%n",
                    workerId, newCost
            );

            // workerId の妥当性チェック（念のため）
            if (workerId < 0 || workerId >= numWorkers) {
                System.err.println("[TSPMaster] WARNING: invalid workerId=" + workerId);
                return;
            }

            taskHasFinished[workerId] = true;
            compareBestCost(newPath, newCost);

            // 全ワーカーが完了したかチェック
            boolean allDone = true;
            for (int w = 0; w < numWorkers; w++) {
                if (!taskHasFinished[w]) {
                    allDone = false;
                    break;
                }
            }

            if (allDone) {
                System.out.println("[TSPMaster] All workers reported. Mark as finished.");
                this.finished = true;
            }

        } catch (ClassCastException cce) {
            System.err.println("[TSPMaster] ERROR: Unexpected message content type.");
            cce.printStackTrace();
        } catch (Exception e) {
            System.err.println("[TSPMaster] ERROR in receivedMessage.");
            e.printStackTrace();
        }
    }
    
    public static class TSPMasterMessage implements Serializable {

    	public int numCities;
    	public int workerId;
    	public int start;
    	public int end;
    	public int[][] distanceMatrix;
    	
    	public String masterId;
    }
}