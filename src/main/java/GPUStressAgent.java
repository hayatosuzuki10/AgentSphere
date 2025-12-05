import static org.jocl.CL.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.javaflow.api.continuable;
import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

import primula.agent.AbstractAgent;

/**
 * GPU を長時間忙しくしつつ VRAM も食わせるエージェント
 * - OpenCL で FMA 的な計算を回し続ける
 * - 大きめのバッファを複数確保して VRAM 使用量を上げる
 */
public class GPUStressAgent extends AbstractAgent {

  // 調整パラメータ
  private final int    globalWorkItems = 1_048_576; // 1024*1024 など(≒並列度)
  private final int    itersPerKernel  = 5_000;     // 1回のカーネル内ループ回数（増やすとGPU使用率↑）
  private final int    launchPerRound  = 10;        // 1ラウンドに何回カーネルを投げるか
  private final double dutyCycle       = 0.95;      // 0.0〜1.0（スリープを減らすほど負荷↑）
  private final double memoryGB        = 2.0;       // 追加で確保する VRAM 量（目安, 実際は端数あり）
  private final int    bufShardMB      = 256;       // VRAM確保時の1塊サイズ（大きすぎると失敗しやすい）
  private final int    statusEverySec  = 3;         // ログ周期（秒）

  private volatile boolean running = true;

  @Override public void requestStop() { running = false; }

  @Override public @continuable void run() {
    log("starting…");

    // 1) OpenCL 初期化（最初に見つかった GPU デバイスを使う）
    CL.setExceptionsEnabled(true);
    int[] numPlatformsArray = new int[1];
    clGetPlatformIDs(0, null, numPlatformsArray);
    cl_platform_id[] platforms = new cl_platform_id[numPlatformsArray[0]];
    clGetPlatformIDs(platforms.length, platforms, null);

    cl_device_id chosenDevice = null;
    cl_platform_id chosenPlatform = null;

    outer:
    for (cl_platform_id p : platforms) {
      int[] numDevicesArray = new int[1];
      int err = clGetDeviceIDs(p, CL_DEVICE_TYPE_GPU, 0, null, numDevicesArray);
      if (err != CL_SUCCESS || numDevicesArray[0] == 0) continue;
      cl_device_id[] devices = new cl_device_id[numDevicesArray[0]];
      clGetDeviceIDs(p, CL_DEVICE_TYPE_GPU, devices.length, devices, null);
      for (cl_device_id d : devices) {
        chosenPlatform = p; chosenDevice = d;
        break outer;
      }
    }
    if (chosenDevice == null) {
      log("no OpenCL GPU device found.");
      return;
    }

    // コンテキスト・キュー
    cl_context_properties props = new cl_context_properties();
    props.addProperty(CL_CONTEXT_PLATFORM, chosenPlatform); // ← ここは戻り値void
    
    cl_context context = clCreateContext(
        props,
        1, new cl_device_id[]{chosenDevice}, null, null, null);

    cl_command_queue queue = clCreateCommandQueue(context, chosenDevice, 0, null);

    // 2) カーネル準備（単純なFLOPループ）
    final String src =
        "__kernel void burn(__global float* a, __global float* b, __global float* c, int iters){\n" +
        "  int gid = get_global_id(0);\n" +
        "  float x = a[gid];\n" +
        "  float y = b[gid];\n" +
        "  for(int i=0;i<iters;i++){\n" +
        "    x = fma(x, y, 1.0f);\n" +   // FMA で計算負荷
        "    y = fma(y, x, 0.5f);\n" +
        "  }\n" +
        "  c[gid] = x + y;\n" +
        "}\n";

    cl_program program = clCreateProgramWithSource(context, 1, new String[]{src}, null, null);
    int build = clBuildProgram(program, 0, null, null, null, null);
    if (build != CL_SUCCESS) {
      // ビルドログ取得
      long[] logSize = new long[1];
      clGetProgramBuildInfo(program, chosenDevice, CL_PROGRAM_BUILD_LOG, 0, null, logSize);
      byte[] logData = new byte[(int)logSize[0]];
      clGetProgramBuildInfo(program, chosenDevice, CL_PROGRAM_BUILD_LOG, logSize[0], Pointer.to(logData), null);
      log("build log:\n" + new String(logData));
      throw new RuntimeException("OpenCL build failed: " + build);
    }
    cl_kernel kernel = clCreateKernel(program, "burn", null);

    // 3) 入出力バッファ（計算用）
    int n = globalWorkItems;
    long bytes = n * 4L;
    cl_mem bufA = clCreateBuffer(context, CL_MEM_READ_WRITE, bytes, null, null);
    cl_mem bufB = clCreateBuffer(context, CL_MEM_READ_WRITE, bytes, null, null);
    cl_mem bufC = clCreateBuffer(context, CL_MEM_READ_WRITE, bytes, null, null);

    // データ初期化
    float[] initA = new float[n];
    float[] initB = new float[n];
    for (int i = 0; i < n; i++) { initA[i] = 1.0f; initB[i] = 2.0f; }
    clEnqueueWriteBuffer(queue, bufA, true, 0, bytes, Pointer.to(initA), 0, null, null);
    clEnqueueWriteBuffer(queue, bufB, true, 0, bytes, Pointer.to(initB), 0, null, null);

    // 4) VRAM を“実占有”させるための追加バッファ（読み書きで実体化）
    List<cl_mem> hogs = new ArrayList<>();
    long wantBytes = (long)(memoryGB * 1024 * 1024 * 1024L);
    long shardBytes = bufShardMB * 1024L * 1024L;
    long acc = 0;
    try {
      while (acc + shardBytes <= wantBytes) {
        cl_mem m = clCreateBuffer(context, CL_MEM_READ_WRITE, shardBytes, null, null);
        hogs.add(m);
        // 少し書き込んで“実際に使ってる”状態に
        byte[] touch = new byte[1024];
        clEnqueueWriteBuffer(queue, m, true, 0, touch.length, Pointer.to(touch), 0, null, null);
        acc += shardBytes;
      }
      log(String.format("VRAM hogging: allocated ~%.2f GB (%d x %dMB)",
          acc / 1e9, hogs.size(), bufShardMB));
    } catch (CLException oom) {
      log("VRAM allocation capped by driver: " + oom.getMessage());
    }

    // 5) 実行ループ
    long lastLog = System.currentTimeMillis();
    Pointer pA = Pointer.to(bufA);
    Pointer pB = Pointer.to(bufB);
    Pointer pC = Pointer.to(bufC);
    clSetKernelArg(kernel, 0, Sizeof.cl_mem, pA);
    clSetKernelArg(kernel, 1, Sizeof.cl_mem, pB);
    clSetKernelArg(kernel, 2, Sizeof.cl_mem, pC);

    long[] global = new long[]{n};

    while (running) {
      for (int i = 0; i < launchPerRound && running; i++) {
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{itersPerKernel}));
        clEnqueueNDRangeKernel(queue, kernel, 1, null, global, null, 0, null, null);
      }
      clFinish(queue);

      // 負荷のデューティ比（1−dutyCycle の分だけ休む）
      if (dutyCycle < 1.0) {
        long busyMs = 100;
        long sleepMs = Math.max(0, Math.round(busyMs * (1.0 - dutyCycle) / dutyCycle));
        if (sleepMs > 0)
			try {
				Thread.sleep(sleepMs);
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
      }

      // たまにログ
      long now = System.currentTimeMillis();
      if (now - lastLog > statusEverySec * 1000L) {
        lastLog = now;
        log("running… (kernel=" + launchPerRound + "×, iters=" + itersPerKernel
            + ", hogs=" + hogs.size() + ", n=" + n + ")");
      }
    }

    // 後始末
    for (cl_mem m : hogs) clReleaseMemObject(m);
    clReleaseMemObject(bufA);
    clReleaseMemObject(bufB);
    clReleaseMemObject(bufC);
    clReleaseKernel(kernel);
    clReleaseProgram(program);
    clReleaseCommandQueue(queue);
    clReleaseContext(context);

    log("stopped.");
  }

  private static void log(String s){ System.out.println("[GPUStressAgent] " + s); }
}