import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;

public class MMapLoaderAgent extends AbstractAgent {
  private final long targetGB = 8;                  // 合計マップサイズ
  private final int touchStride = 4096;             // 初期タッチ間隔 (4KB)
  private final long windowSize = 1L << 30;         // 1GBごとにスライス
  private final int activeIntervalMs = 1000;        // 維持時のアクセス周期
  private final int writeOpsPerCycle = 50000;       // 維持時のランダム書き込み数
  private final int writeStride = 16384;            // 書き込み間隔(16KB)
  private volatile boolean running = true;

  private Path backingFile;
  private final Random rnd = new Random();

  @Override public void requestStop() { running = false; }

  @Override public @continuable void run() {
    try {
      backingFile = Paths.get(System.getProperty("user.home"), "mmap.bin");
      prepareFile(backingFile, targetGB);
      long totalSize = targetGB * 1024L * 1024L * 1024L;
      log("mapping " + targetGB + "GB: " + backingFile);

      try (FileChannel ch = FileChannel.open(backingFile, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
        int sliceIndex = 0;
        long offset = 0;
        long remaining = totalSize;

        // ===== 初期タッチ（全スライスを実メモリにのせる） =====
        while (running && remaining > 0) {
          long mapSize = Math.min(windowSize, remaining);
          MappedByteBuffer buf = ch.map(FileChannel.MapMode.READ_WRITE, offset, mapSize);
          for (long i = 0; running && i < mapSize; i += touchStride) {
            buf.put((int) i, (byte) 1);
          }
          buf.force();
          log(String.format("slice #%d initialized (%.2f GB)", sliceIndex, mapSize / 1e9));
          sliceIndex++;
          offset += mapSize;
          remaining -= mapSize;
        }

        // ===== 維持フェーズ：ランダム書き込みでメモリを温存 =====
        log("entering sustain loop...");
        while (running) {
          long pos = Math.abs(rnd.nextLong()) % totalSize;
          long sliceOffset = pos / windowSize * windowSize;
          long sliceSize = Math.min(windowSize, totalSize - sliceOffset);

          MappedByteBuffer buf = ch.map(FileChannel.MapMode.READ_WRITE, sliceOffset, sliceSize);

          for (int i = 0; i < writeOpsPerCycle && running; i++) {
            int p = (int) (rnd.nextInt((int) Math.min(sliceSize - writeStride, Integer.MAX_VALUE)));
            buf.put(p, (byte) (rnd.nextInt(256)));
          }

          buf.force();
          log(String.format("touched randomly slice=%.2fGB ops=%d", sliceOffset/1e9, writeOpsPerCycle));
          Thread.sleep(activeIntervalMs);
        }
      }
    } catch (Exception e) {
      log("error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void prepareFile(Path p, long gb) throws IOException {
    long size = gb * 1024L * 1024L * 1024L;
    if (Files.notExists(p)) {
      log("create file: " + p + " (" + size + " bytes)");
      try (RandomAccessFile raf = new RandomAccessFile(p.toFile(), "rw")) {
        raf.setLength(size);
      }
    }
  }

  private static void log(String m){ System.out.println("[MMapLoaderAgent] " + m); }
}