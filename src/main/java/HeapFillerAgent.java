import java.util.ArrayList;
import java.util.List;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;

public class HeapFillerAgent extends AbstractAgent {
  // 調整用パラメータ
  private final double targetUsage = 0.80;   // MaxHeap の 80% まで盛る
  private final double releaseRatio = 0.15;  // 山の頂点で 15% 解放して谷を作る
  private final int    chunkKB = 1024;       // 1MB チャンク
  private final int    touchEveryMs = 500;   // 定期的に触って“生存”させる

  private volatile boolean running = true;
  private final List<byte[]> heapHold = new ArrayList<>();
  private long maxHeap;

  @Override public void requestStop() { running = false; }

  @Override public @continuable void run() {
    maxHeap = Runtime.getRuntime().maxMemory();
    log("maxHeap = " + pretty(maxHeap));

    try {
      while (running) {
        // 1) ターゲット使用量まで増やす
        long targetBytes = (long)(maxHeap * targetUsage);
        while (running && used() < targetBytes) {
          heapHold.add(new byte[chunkKB * 1024]); // 強参照で保持
          // たまに触っておく（エスケープ解析や最適化回避の保険）
          if ((heapHold.size() & 0x3FF) == 0) touchSome();
        }

        log("peak: used=" + pretty(used()) + " / committed~" + pretty(committed()));

        // 2) 少し解放して谷を作る
        int toRemove = (int)Math.max(1, heapHold.size() * releaseRatio);
        for (int i = 0; i < toRemove && !heapHold.isEmpty(); i++) {
          heapHold.remove(heapHold.size() - 1);
        }
        System.gc(); // 観察を分かりやすく（必須ではない）

        // 3) 生存させつつ待つ（グラフで plateau を作る）
        long until = System.currentTimeMillis() + 5_000;
        while (running && System.currentTimeMillis() < until) {
          touchSome();
          Thread.sleep(touchEveryMs);
        }
      }
    } catch (InterruptedException ignored) {
    } finally {
      heapHold.clear();
      System.gc();
      log("stopped.");
    }
  }

  private void touchSome() {
    // いくつかの塊に書き込み（本当にヒープ上にあることを担保）
    for (int i = 0; i < heapHold.size(); i += 1024) {
      byte[] b = heapHold.get(i);
      if (b.length > 0) b[0]++; // 微書き込み
    }
  }

  private static String pretty(long b) {
    return String.format("%.2f GB", b / 1024.0 / 1024.0 / 1024.0);
  }
  private static long used() {
    Runtime rt = Runtime.getRuntime();
    return rt.totalMemory() - rt.freeMemory(); // ＝ JvmHeapUsed に近い
  }
  private static long committed() {
    return Runtime.getRuntime().totalMemory();
  }
  private static void log(String m) { System.out.println("[HeapFillerAgent] " + m); }
}