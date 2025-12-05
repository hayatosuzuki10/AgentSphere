

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;

public class NetClientAgent extends AbstractAgent {
  // ===== 設定 =====
  private final String SERVER_IP = "172.28.15.111"; // サーバPCのIP
  private final int    PORT      = 5050;
  private final long   DURATION_MS = 20_000;        // 送信を続ける時間
  private final int    CHUNK = 64 * 1024;           // 1回の送信サイズ
  private final long   MAX_UPLOAD_Mbps = 0;       // 0なら無制限
  private final boolean READ_BACK = true;          // サーバがエコーバック時に受信も読む

  private volatile boolean running = true;
  @Override public void requestStop() { running = false; }

  @Override public @continuable void run() {
	  
    log("NetClientAgent -> " + SERVER_IP + ":" + PORT);
    TokenBucket limiter = (MAX_UPLOAD_Mbps>0) ? TokenBucket.megabitsPerSec(MAX_UPLOAD_Mbps)
                                              : TokenBucket.unlimited();

    try (Socket sock = new Socket()) {
      sock.setTcpNoDelay(true);
      sock.connect(new InetSocketAddress(SERVER_IP, PORT), 5000);
      OutputStream out = sock.getOutputStream();
      InputStream in   = sock.getInputStream();

      byte[] buf = new byte[CHUNK]; new Random().nextBytes(buf);
      byte[] rbuf = new byte[CHUNK];
      long start = System.currentTimeMillis(), sent = 0, recv = 0, last = start;

      while (running && System.currentTimeMillis() - start < DURATION_MS) {
        limiter.consume(buf.length);
        out.write(buf); sent += buf.length;

        if (READ_BACK && in.available() > 0) {
          int r = in.read(rbuf);
          if (r > 0) recv += r;
        }

        long now = System.currentTimeMillis();
        if (now - last >= 2000) {
          log(String.format("progress TX=%.2f MB  RX=%.2f MB",
              sent/1e6, recv/1e6));
          last = now;
        }
      }
      out.flush();
      log(String.format("DONE TX=%.2f MB RX=%.2f MB", sent/1e6, recv/1e6));
    } catch (IOException e) {
      log("client error: " + e.getMessage());
    }
  }

  // シンプルなトークンバケット（bytes/s）
  static class TokenBucket {
    private final long cap, refillPerSec;
    private long tokens, lastNs;
    static TokenBucket megabitsPerSec(long mbps){ long bps=(mbps*1_000_000L)/8; return new TokenBucket(bps,bps); }
    static TokenBucket unlimited(){ return new TokenBucket(Long.MAX_VALUE/4, Long.MAX_VALUE/4); }
    TokenBucket(long cap,long refill){ this.cap=cap; this.refillPerSec=refill; this.tokens=cap; this.lastNs=System.nanoTime(); }
    synchronized void consume(long bytes){
      while (tokens < bytes){
        long now=System.nanoTime();
        long add=(long)((now-lastNs)/1e9 * refillPerSec);
        if (add>0){ tokens=Math.min(cap,tokens+add); lastNs=now; }
        if (tokens < bytes) try { wait(5); } catch (InterruptedException ignored) {}
      }
      tokens -= bytes;
    }
  }

  private static void log(String m){ System.out.println("[NetClientAgent] " + m); }
}