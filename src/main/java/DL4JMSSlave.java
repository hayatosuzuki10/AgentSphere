import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.javaflow.api.continuable;
import org.deeplearning4j.datasets.fetchers.DataSetType;
import org.deeplearning4j.datasets.iterator.impl.Cifar10DataSetIterator;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;

public class DL4JMSSlave extends AbstractAgent implements IMessageListener {

    private String parentId, masterIp;
    private volatile boolean running = true;
    private final int batchSize = 32, localEpochs = 1;
    private boolean loaded = false;

    private int partIndex = 0;       
    private int totalParts = 1;    

    private List<DataSet> localData = null;

    //各種セッター
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setMasterIp(String masterIp) { this.masterIp = masterIp; }
    public void setPartIndex(int idx) { this.partIndex = idx; }
    public void setTotalParts(int tot) { this.totalParts = tot; }

    public @continuable void run() {
        System.out.println("[Slave] 起動: " + getAgentName());
        try { MessageAPI.registerMessageListener(this); }  //リスナ登録
        catch (Exception e) { e.printStackTrace(); return; }

        loadLocalTrainingData();  //この関数で学習用データをロード
        loaded = true;            //ロードできたかフラグ

        //データを読み込んだらひたすら待機
        //学習モデルを受け取ったらそれに応じて処理するだけ
        while (running) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        
        //---------------------------------------------
        //Slave側を終了させる処理はまだ記述していません
        //---------------------------------------------
    }
    
    
    //------------------------------------------------------------------------
    //Masterから初期化or平均化された学習モデルを受け取り、担当するデータで学習
    //その後、学習済みの学習モデルをMasterへ送信
    //------------------------------------------------------------------------
    @Override
    public void receivedMessage(AbstractEnvelope env) {

        try {
            byte[] payload =
                (byte[]) ((StandardContentContainer) env.getContent()).getContent();

            byte type = payload[0];
            byte[] body = java.util.Arrays.copyOfRange(payload, 1, payload.length);

            //①学習データが読み込まれるまで待つ
            //  読み込まれたら学習開始
            if (!loaded) {
                System.out.println("[Slave] 学習データ読み込み待ち中...");
                while (!loaded) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
                System.out.println("[Slave] 学習データロード完了 → 学習可能");
            }

            //type=2 → 学習モデル
            if (type == 2) {

                if (localData == null) {
                    System.out.println("[Slave] エラー：学習データがまだ用意されていません");
                    return;
                }

                MultiLayerNetwork model = null;
                ListDataSetIterator<DataSet> iter = null;

              //②実際に学習処理を開始する
                try {
                	
                    System.out.println("[Slave] モデル受信 → 学習開始");

                    model = ModelSerializer.restoreMultiLayerNetwork(new ByteArrayInputStream(body));

                    iter = new ListDataSetIterator<>(localData, batchSize);

                    for (int e = 0; e < localEpochs; e++) {
                        model.fit(iter);
                        iter.reset();
                    }

                    System.out.println("[Slave] 学習完了 → モデル返送");

                    byte[] updated = serializeModel(model);

                    //返送は type=3
                    byte[] sendPayload = wrapWithType((byte)3, updated);

                    KeyValuePair<InetAddress,Integer> back =
                            new KeyValuePair<>(Inet4Address.getByName(masterIp), 55878);

                    //③学習済み学習モデルをMasterへ返す
                    MessageAPI.send(
                        back,
                        new StandardEnvelope(new AgentAddress(parentId),
                            new StandardContentContainer(sendPayload))
                    );

                    //④以下メモリ関連のエラー対策
                } finally {
                    //このラウンドで使った Iterator の参照を切る
                    if (iter != null) {
                        iter = null;
                    }

                    //モデルの内部リソースを解放し、参照を切る
                    if (model != null) {
                        try {
                            model.clear();
                        } catch (Exception ignore) {}
                        model = null;
                    }

                    //ND4J のワークスペースとメモリをこのスレッド分クリア
                    Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
                    Nd4j.getMemoryManager().invokeGc();
                    System.gc();
                }

                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    //学習用データをロードするための関数
    private void loadLocalTrainingData() {

        System.out.println("[Slave] CIFAR-10 データ読み込み開始");

        try {
            final int rngSeed = 123;

            DataSetIterator trainIt = new Cifar10DataSetIterator(
                    batchSize,
                    null,                 // 画像サイズ → null 固定（DL4J仕様）
                    DataSetType.TRAIN,    // 学習用データ 50,000 枚
                    null,                 // データ拡張なし
                    rngSeed               // シャッフルシード
            );

            //すべての DataSet をリストへ展開（各 DataSet を workspace から detach）
            List<DataSet> all = new ArrayList<>(); // バッチが入る
            while (trainIt.hasNext()) {
                DataSet ds = trainIt.next();
                ds.detach();        
                all.add(ds);
            }

            int total = all.size();
            int partSize = total / totalParts;

            int start = partIndex * partSize;
            int end = (partIndex == totalParts - 1) ? total : start + partSize;

            //Slave が担当するデータだけ取り出す
            localData = new ArrayList<>(all.subList(start, end));

            //自分の担当分以外の DataSet 参照を全部消す
            all.clear();

            System.out.println("[Slave] データ読み込み完了: 担当 part " +
                    partIndex + "/" + totalParts +
                    "  start=" + start +
                    "  end=" + end +
                    "  size=" + localData.size()
            );

            //ワークスペースとメモリをクリーンアップ(メモリが足りなくなる)
            Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
            Nd4j.getMemoryManager().invokeGc();
            System.gc();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[Slave] データ読み取りエラー");
        }
    }

    

    //シリアライズした学習モデルを送信できる形にする関数
    private byte[] wrapWithType(byte type, byte[] body) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(type);
        try { baos.write(body); }
        catch (IOException e) { throw new RuntimeException(e); }
        return baos.toByteArray();
    }

    
    //学習モデルをシリアライズする関数
    private static byte[] serializeModel(MultiLayerNetwork model) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ModelSerializer.writeModel(model, baos, true);
            return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override public String getStrictName() { return this.getAgentID(); }
    @Override public String getSimpleName() { return this.getAgentName(); }
    @Override public void requestStop() {}
}



