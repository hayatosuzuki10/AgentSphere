package scheduler2022.network;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

public class NetworkSpeedMeasurer {

    public static double[] measureSpeed(String ip, int port) {
    	double uploadMbps = -1.0;
        double downloadMbps = -1.0;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 2000);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 100KBの送信データ（アップロード）
            byte[] data = new byte[1024 * 100];
            new Random().nextBytes(data);

            long uploadStart = System.nanoTime();
            out.write(data);
            out.flush();
            long uploadEnd = System.nanoTime();

            uploadMbps = (data.length * 8) / ((uploadEnd - uploadStart) / 1e9 * 1_000_000);

            // 100KBの受信データ（ダウンロード）
            byte[] buffer = new byte[1024 * 100];
            int total = 0;
            long downloadStart = System.nanoTime();
            while (total < buffer.length) {
                int read = in.read(buffer, total, buffer.length - total);
                if (read == -1) break;
                total += read;
            }
            long downloadEnd = System.nanoTime();

            downloadMbps = (total * 8) / ((downloadEnd - downloadStart) / 1e9 * 1_000_000);

        } catch (Exception e) {
            System.err.println("[❌ 測定失敗]: " + e.getMessage());
        }

        return new double[]{uploadMbps, downloadMbps};
    }
}