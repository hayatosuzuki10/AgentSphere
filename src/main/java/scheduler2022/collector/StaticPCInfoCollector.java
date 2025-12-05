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
        StaticPCInfo.CPU cpu = readCpu();
        applyCpuBenchmark(cpu);

        // Memory
        GlobalMemory mem = hal.getMemory();
        long total = mem.getTotal();

        // NIC
        Map<String, StaticPCInfo.NetworkCard> nics = readNics();

        // GPU
        Map<String, StaticPCInfo.GPU> gpus = readGpus();
        applyGpuBenchmark(gpus);

        return new StaticPCInfo(cpu, total, nics, gpus);
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
        } catch (IOException ignored) {}
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
            });
        } catch (IOException ignored) {}
    }
}