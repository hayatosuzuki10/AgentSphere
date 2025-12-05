package scheduler2022.network;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class NetworkSpeedReceiverThread extends Thread {

    private int port;

    public NetworkSpeedReceiverThread(int port) {
        this.port = port;
        setDaemon(true); // プログラム終了時に自動で終了
    }

    @Override
    public void run() {
//    	try {
//            ProcessBuilder builder = new ProcessBuilder("/opt/homebrew/bin/iperf3", "-s", "-p", Integer.toString(port));
//            builder.start();
//            //System.out.println("送信速度: " + result.end.sum_sent.bits_per_second / 1_000_000 + " Mbps");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    	try (ServerSocket serverSocket = new ServerSocket(port+1)) {
            System.out.println("[📥 NetworkReceiver] ポート " + port + " で待機中...");

            while (true) {
                try (Socket client = serverSocket.accept()) {
                    System.out.println("[📥 NetworkReceiver] 接続: " + client.getInetAddress());

                    // クライアント → サーバー（アップロード）受信
                    InputStream in = client.getInputStream();
                    byte[] uploadBuffer = new byte[1024 * 100]; // 100KB
                    while (in.read(uploadBuffer) != -1) {
                        // 受信し続ける（タイムアウトやサイズで終了しても可）
                        break; // 1回だけで終了
                    }

                    // サーバー → クライアント（ダウンロード）送信
                    OutputStream out = client.getOutputStream();
                    byte[] downloadData = new byte[1024 * 100]; // 100KB
                    new Random().nextBytes(downloadData);
                    out.write(downloadData);
                    out.flush();

                    System.out.println("[📥 NetworkReceiver] アップロード受信 → ダウンロード送信完了");
                } catch (Exception e) {
                    System.err.println("[⚠️ NetworkReceiver] 通信エラー: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[❌ NetworkReceiver] 起動失敗: " + e.getMessage());
        }
    }
}