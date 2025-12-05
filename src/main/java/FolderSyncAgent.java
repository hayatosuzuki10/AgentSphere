import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.List;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;

public class FolderSyncAgent extends AbstractAgent {

  // ====== 設定 ======
  private final String srcDirStr = "C:\\Users\\selab\\Sync";
  private final String serverUri = "http://172.28.15.111:8088";
  private transient Path srcDir;         // 監視元
  private transient URI  server;   // 受け側
  private final String dataset = "default";                          // 同期セット名
  private final long scanIntervalMs = 1000;                          // ポーリング周期
  private final long maxUploadMbps  = 50;                            // 帯域上限 (Mbps)。0で無制限
  private final List<String> ignoreGlobs = List.of("**/.git/**", "**/*.tmp", "**/~*");

  private volatile boolean running = true;

  @Override public void requestStop() { running = false; }

  @Override public @continuable void run() {
	    this.srcDir = resolveBaseDir(srcDirStr);
	    this.server = URI.create(serverUri);
    log("FolderSyncAgent started. src=" + srcDir + " -> " + server);
    TokenBucket limiter = (maxUploadMbps > 0)
        ? TokenBucket.megabitsPerSec(maxUploadMbps)
        : TokenBucket.unlimited();

    try {
		ensureBaseDir(srcDir);
	} catch (IOException e) {
		// TODO 自動生成された catch ブロック
		e.printStackTrace();
	}
    try {
      while (running) {
        try {
          Files.walk(srcDir)
              .filter(Files::isRegularFile)
              .filter(this::notIgnored)
              .forEach(p -> {
                if (!running) return;
                try { syncOne(p, limiter); }
                catch (Exception e) { log("sync error: " + p + " -> " + e.getMessage()); }
              });
        } catch (IOException e) {
          log("scan error: " + e.getMessage());
          e.printStackTrace();
        }
        sleep(scanIntervalMs);
      }
    } finally {
      log("FolderSyncAgent stopped.");
    }
  }
  
  private static Path resolveBaseDir(String raw) {
	    if (raw == null || raw.isBlank()) {
	        return Paths.get(System.getProperty("user.home"), "Sync")
	                    .toAbsolutePath().normalize();
	    }
	    String s = raw.trim();

	    // ~ をホームに展開
	    if (s.startsWith("~")) s = System.getProperty("user.home") + s.substring(1);

	    // file:// も許可
	    if (s.startsWith("file:")) return Paths.get(URI.create(s)).toAbsolutePath().normalize();

	    // 区切り記号の正規化
	    s = s.replace('\\', File.separatorChar).replace('/', File.separatorChar);
	    Path p = Paths.get(s);

	    // Windowsで「」のようにドライブ無しのルート相対→現在ドライブに補完
	    if (File.separatorChar == '\\' && s.startsWith("\\") && !s.matches("^[A-Za-z]:.*")) {
	        String root = Paths.get("").toAbsolutePath().getRoot().toString(); // 例: C:\
	        p = Paths.get(root + s);
	    }
	    return p.toAbsolutePath().normalize();
	}

	private static void ensureBaseDir(Path base) throws IOException {
	    if (Files.notExists(base)) Files.createDirectories(base);
	    if (!Files.isDirectory(base)) throw new IOException("not a directory: " + base);
	    if (!Files.isReadable(base)) throw new IOException("not readable: " + base);
	}

  private boolean notIgnored(Path p) {
    for (String g : ignoreGlobs) {
      PathMatcher m = p.getFileSystem().getPathMatcher("glob:" + g);
      if (m.matches(srcDir.relativize(p))) return false;
    }
    return true;
  }

  private void syncOne(Path file, TokenBucket limiter) throws Exception {
	  String rel = srcDir.relativize(file).toString().replace(File.separatorChar,'/');
	  long size = Files.size(file);
	  log("start sync: " + rel + " (" + size + " bytes)");

	  ServerProbe probe = probe(server, dataset, rel, size, Files.getLastModifiedTime(file).toMillis());
	  long offset = Math.max(0, probe.receivedBytes);
	  log("resume offset: " + offset);

	  upload(file, rel, size, offset, limiter);
	  commit(server, dataset, rel, size, Files.getLastModifiedTime(file).toMillis());
	  log("commit ok: " + rel);
	}

  // ---- サーバ照会
  private ServerProbe probe(URI base, String dataset, String rel, long size, long mtime) throws IOException {
    URL url = base.resolve("/probe?dataset="+enc(dataset)+"&path="+enc(rel)+"&size="+size+"&mtime="+mtime).toURL();
    HttpURLConnection c = (HttpURLConnection) url.openConnection();
    c.setConnectTimeout(5000);
    c.setReadTimeout(5000);
    c.setRequestMethod("GET");
    try (InputStream in = c.getInputStream()) {
      String json = new String(in.readAllBytes());
      return ServerProbe.fromJson(json);
    } catch (FileNotFoundException e) {
      return new ServerProbe("new", 0);
    }
  }

  // ---- 本体アップロード
  private void upload(Path file, String rel, long totalSize, long offset, TokenBucket limiter) throws Exception {
    long pos = offset;
    int backoff = 0;

    try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
      ch.position(offset);
      ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 256); // 256KB チャンク

      while (pos < totalSize && running) {
    	  long reported = offset;
    	  
        buf.clear();
        int read = ch.read(buf);
        if (read < 0) break;
        buf.flip();

        // 帯域制御（バイト単位）
        limiter.consume(read);

        long chunkFrom = pos;
        long chunkTo   = pos + read - 1;

        // HTTP PUT with Content-Range
        URL url = server.resolve("/upload?dataset="+enc(dataset)+"&path="+enc(rel)).toURL();
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(20000);
        c.setDoOutput(true);
        c.setRequestMethod("PUT");
        c.setRequestProperty("Content-Range", "bytes " + chunkFrom + "-" + chunkTo + "/" + totalSize);
        try (OutputStream out = c.getOutputStream()) {
          // zero-copy できないので direct buffer → byte[] 経由
          byte[] tmp = new byte[read];
          buf.get(tmp);
          out.write(tmp);
        }
        int code = c.getResponseCode();
        if (code == 308 || code == 200 || code == 204) { // 308: resume in progress, 200/204: ok
          pos += read;
    	  if (pos - reported >= 5L * 1024 * 1024) {
      	    log(String.format("uploading %s: %,d / %,d (%.1f%%)",
      	        rel, pos, totalSize, 100.0 * pos / totalSize));
      	    reported = pos;
      	  }
          backoff = 0; // 成功したらリセット
        } else {
          // 失敗 → バックオフしてリトライ
          String msg = readBody(c);
          throw new IOException("HTTP " + code + " " + msg);
        }
        
      }
    } catch (IOException ioe) {
      // リトライ（指数バックオフ）
      if (!running) throw ioe;
      int waitMs = Math.min(15_000, (int)Math.pow(2, Math.min(5, ++backoff)) * 250);
      log("upload chunk failed, retry in " + waitMs + "ms: " + ioe.getMessage());
      sleep(waitMs);
      // 再帰せず、プローブでオフセット再取得してやり直す
      long newOffset = probe(server, dataset, rel, totalSize, Files.getLastModifiedTime(file).toMillis()).receivedBytes;
      upload(file, rel, totalSize, newOffset, limiter);
    }
  }

  // ---- コミット
  private void commit(URI base, String dataset, String rel, long size, long mtime) throws IOException {
    URL url = base.resolve("/commit?dataset="+enc(dataset)+"&path="+enc(rel)+"&size="+size+"&mtime="+mtime).toURL();
    HttpURLConnection c = (HttpURLConnection) url.openConnection();
    c.setConnectTimeout(5000);
    c.setReadTimeout(5000);
    c.setRequestMethod("POST");
    if (c.getResponseCode() / 100 != 2) {
      throw new IOException("commit failed: " + c.getResponseCode());
    }
  }

  // ---- Utils
  private static String enc(String s) { return s.replace(" ", "%20"); }
  private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
  private static void log(String m) { System.out.println("[FolderSyncAgent] " + m); }
  private static String readBody(HttpURLConnection c) {
    try (InputStream in = (c.getErrorStream()!=null?c.getErrorStream():c.getInputStream())) {
      return in==null? "": new String(in.readAllBytes());
    } catch (Exception e) { return ""; }
  }

  // オプション：厳密比較に使う sha256
  @SuppressWarnings("unused")
  private static String sha256(Path p) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    try (InputStream in = Files.newInputStream(p)) {
      byte[] buf = new byte[1024*1024];
      int r; while ((r = in.read(buf)) >= 0) md.update(buf,0,r);
    }
    byte[] d = md.digest();
    StringBuilder sb = new StringBuilder();
    for (byte b : d) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  // 受け側の /probe の返答を受け取るための最小 DTO
  static class ServerProbe {
    final String status;      // "new" | "partial" | "upToDate"
    final long receivedBytes; // レジューム開始位置
    ServerProbe(String s, long r){ status=s; receivedBytes=r; }
    static ServerProbe fromJson(String j) {
      // 超雑実装： {"status":"partial","received":12345}
      String s = j.replaceAll("\\s","");
      String st = s.replaceAll(".*\"status\":\"([^\"]+)\".*","$1");
      long rec = 0;
      try { rec = Long.parseLong(s.replaceAll(".*\"received\":(\\d+).*","$1")); } catch(Exception ignored){}
      if (st.equals(s)) st = "new";
      return new ServerProbe(st, rec);
    }
  }

  // 帯域制御（トークンバケット, bytes/s）
  static class TokenBucket {
    private final long capacity;
    private final long refillPerSec;
    private long tokens;
    private long lastNs;

    static TokenBucket megabitsPerSec(long mbps) {
      long bytesPerSec = (mbps * 1_000_000L) / 8;
      return new TokenBucket(bytesPerSec, bytesPerSec);
    }
    static TokenBucket unlimited() { return new TokenBucket(Long.MAX_VALUE/4, Long.MAX_VALUE/4); }

    TokenBucket(long capacity, long refillPerSec) {
      this.capacity = capacity;
      this.refillPerSec = refillPerSec;
      this.tokens = capacity;
      this.lastNs = System.nanoTime();
    }

    synchronized void consume(long bytes) {
      while (tokens < bytes) {
        refill();
        if (tokens < bytes) {
          try { wait(5); } catch (InterruptedException ignored) {}
        }
      }
      tokens -= bytes;
    }

    private void refill() {
      long now = System.nanoTime();
      long elapsedNs = now - lastNs;
      if (elapsedNs <= 0) return;
      long add = (long)((elapsedNs / 1_000_000_000.0) * refillPerSec);
      if (add > 0) {
        tokens = Math.min(capacity, tokens + add);
        lastNs = now;
      }
    }
  }
}