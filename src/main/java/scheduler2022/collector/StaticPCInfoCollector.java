package scheduler2022.collector;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import scheduler2022.JudgeOS;
import scheduler2022.StaticPCInfo;

public class StaticPCInfoCollector implements Serializable {
    private static final long serialVersionUID = 1L;
    private final SystemInfo si;                     // OSHIは使い回し
    private final HardwareAbstractionLayer hal;
    private final ObjectMapper mapper = new ObjectMapper();
    private final File cpuBenchFile;
    private final File gpuBenchFile;

    public StaticPCInfoCollector() {
        this(new SystemInfo(),
             new File("./public/cpu_benchmarks.json"),
             new File("./public/gpu_benchmarks.json"));
    }
    public StaticPCInfoCollector(SystemInfo si, File cpuBench, File gpuBench) {
        this.si = si;
        this.hal = si.getHardware();
        this.cpuBenchFile = cpuBench;
        this.gpuBenchFile = gpuBench;
    }

    public StaticPCInfo collect() {
        // CPU
        StaticPCInfo.CPU cpu = null;
        try {
            cpu = readCpu();
            if (cpu != null) {
                applyCpuBenchmark(cpu);
            }
        } catch (Exception ignored) {}

        // Memory
        long total = -1;
        try {
            GlobalMemory mem = hal.getMemory();
            if (mem != null) {
                total = mem.getTotal();
            }
        } catch (Exception ignored) {}

        // NIC
        Map<String, StaticPCInfo.NetworkCard> nics = null;
        try {
            nics = readNics();
        } catch (Exception ignored) {}

        // GPU
        Map<String, StaticPCInfo.GPU> gpus = null;
        try {
            gpus = readGpus();
            if (gpus != null) {
                applyGpuBenchmark(gpus);
            }
        } catch (Exception ignored) {}

        boolean hasSSD = detectSSD();  // OSごとにSSDか判定する
     // どこかに保存するなら：dpi.hasSSD = hasSSD;

        return new StaticPCInfo(cpu, total, hasSSD, nics, gpus);
    }

    /* ---------- readers (OSHI → DTO) ---------- */

    private StaticPCInfo.CPU readCpu() {
        CentralProcessor p = hal.getProcessor();
        StaticPCInfo.CPU cpu = new StaticPCInfo.CPU();
        cpu.Name         = p.getProcessorIdentifier().getName();
        cpu.Vendor       = p.getProcessorIdentifier().getVendor();
        cpu.MicroArch    = p.getProcessorIdentifier().getMicroarchitecture();
        cpu.PhysicalCore = p.getPhysicalProcessorCount();
        cpu.LogicalCore  = p.getLogicalProcessorCount();
        // BenchMarkScore は applyCpuBenchmark で設定
        return cpu;
    }

    private Map<String, StaticPCInfo.NetworkCard> readNics() {
        Map<String, StaticPCInfo.NetworkCard> out = new HashMap<>();
        List<NetworkIF> list = hal.getNetworkIFs(true);
        for (NetworkIF nif : list) {
            StaticPCInfo.NetworkCard nc = new StaticPCInfo.NetworkCard();
            nc.Name        = nif.getName();
            nc.DisplayName = nif.getDisplayName();
            nc.Bandwidth   = nif.getSpeed();
            nc.MTU         = nif.getMTU();
            out.put(nc.Name, nc);
        }
        return out;

    }

    private Map<String, StaticPCInfo.GPU> readGpus() {
        Map<String, StaticPCInfo.GPU> out = new HashMap<>();
        List<GraphicsCard> gpus = hal.getGraphicsCards();
        for (GraphicsCard g : gpus) {
            StaticPCInfo.GPU dto = new StaticPCInfo.GPU();
            dto.Name     = g.getName();
            dto.Vendor   = g.getVendor();
            dto.VRam     = g.getVRam();
            dto.DeviceID = g.getDeviceId();
            // BenchMarkScore は applyGpuBenchmark で設定
            out.put(dto.Name, dto);
        }
        return out;
    }

    /* ---------- benchmark appliers ---------- */

    private void applyCpuBenchmark(StaticPCInfo.CPU cpu) {
        try {
            if (!cpuBenchFile.exists()) return;
            Map<String, Integer> map = mapper.readValue(cpuBenchFile, new TypeReference<Map<String, Integer>>() {});
            map.entrySet().stream()
               .filter(e -> cpu.Name != null && cpu.Name.contains(e.getKey()))
               .findFirst()
               .ifPresent(e -> cpu.BenchMarkScore = e.getValue());
            System.out.println(cpu.Name);
        } catch (IOException ignored) {
        	System.err.println("CPUBenchMarkScore can't collect");
        }
    }

    private void applyGpuBenchmark(Map<String, StaticPCInfo.GPU> gpus) {
        try {
            if (!gpuBenchFile.exists()) return;
            Map<String, Integer> map = mapper.readValue(gpuBenchFile, new TypeReference<Map<String, Integer>>() {});
            gpus.values().forEach(g -> {
                map.entrySet().stream()
                   .filter(e -> g.Name != null && g.Name.contains(e.getKey()))
                   .findFirst()
                   .ifPresent(e -> g.BenchMarkScore = e.getValue());

                System.out.println(g.Name);
            });
        } catch (IOException ignored) {}
    }
    
    private boolean detectSSD() {
        try {
            if (JudgeOS.isWindows()) {
                return isSSDOnWindows();
            } else if (JudgeOS.isLinux()) {
                return isSSDOnLinux();
            } else if (JudgeOS.isMac()) {
                return isSSDOnMac();
            }
        } catch (Exception e) {
            System.err.println("[WARN] detectSSD failed: " + e.getMessage());
        }
        return false; // デフォルトで HDD 扱い
    }
    
    private boolean isSSDOnWindows() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "powershell",
            "-Command",
            "Get-PhysicalDisk | Select-Object MediaType"
        );
        Process process = pb.start();

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("nvme")) {
                    return true;
                }
                if (line.toLowerCase().contains("ssd")) {
                    return true;
                }
            }
        }
        process.waitFor();
        return false;
    }
    
    private boolean isSSDOnLinux() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "bash", "-c", "lsblk -d -o ROTA | grep -q '^0'"
        );
        Process process = pb.start();
        int exitCode = process.waitFor();
        return exitCode == 0; // ROTA=0 があれば SSD
    }
    
    private boolean isSSDOnMac() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "bash", "-c", "diskutil info disk0 | grep 'Solid State: Yes'"
        );
        Process process = pb.start();
        int exitCode = process.waitFor();
        return exitCode == 0;
    }
}