package scheduler2022;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.sun.management.OperatingSystemMXBean;

import primula.agent.AbstractAgent;
import primula.api.core.assh.ConsolePanel;
import primula.util.IPAddress;
import scheduler2022.util.DHTutil;
import sphereConnection.stub.SphereSpec;

public class RecommendedDest {
    static private double magni = 1.0;

    public RecommendedDest() {}

    static public boolean elsePCsurvival() {
        return InformationCenter.getOthersIPs().size() > 1;
    }

    /** pcInfo が null の場合は false を返すように変更 */
    static private boolean canReceive(DynamicPCInfo cpi, long usedmemory) {
        if (cpi == null) {
            return false;
        }
        // if (cpi.CpuLoadPercent > 99) return false;
        if (cpi.FreeMemory < (long) (magni * usedmemory)) {
            return false;
        }
        return true;
    }

    static public boolean canExecute(AbstractAgent aa) {

        String agentname = aa.getAgentName();

        if (!DHTutil.containsSpec(agentname)) {
            System.out.println("no data. memory observe");
            return true;
        }

        SphereSpec ss = DHTutil.getSpec(agentname);
        if (ss == null) {
            System.out.println("no spec object. memory observe");
            return true;
        }

        OperatingSystemMXBean osMx =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        if (Runtime.getRuntime().freeMemory() < (long) (magni * ss.memoryused))
            return false;
        if (osMx.getSystemCpuLoad() > 0.99)
            return false;
        return true;
    }

    static public Set<String> canmigDests(String agentname) {

        Set<String> canmigPCs = new HashSet<>();

        // ★ getSpec より先に containsSpec を見る
        if (!DHTutil.containsSpec(agentname)) {
            return canmigPCs;
        }
        SphereSpec spec = DHTutil.getSpec(agentname);
        if (spec == null) {
            return canmigPCs;
        }

        long agentusedmem = spec.memoryused;

        for (String key : InformationCenter.getOthersIPs()) {
            DynamicPCInfo pcInfo = InformationCenter.getOtherDPI(key);
            if (IPAddress.myIPAddress.equals(key)) {
                continue;
            }
            if (canReceive(pcInfo, agentusedmem)) {
                canmigPCs.add(key);
            }
        }

        return canmigPCs;
    }

    /**
     * 渡されたエージェント名からCPU使用率が規定値以下でメモリが充足しているエージェントのうち
     * ロードアベレージが最も低いエージェントスフィアIPアドレスを返します
     */
    static public String recomDest(String agentname) {
        String recomm = IPAddress.myIPAddress;

        // 自ノードの情報を安全に取得
        DynamicPCInfo selfInfo = InformationCenter.getMyDPI();
        double minLoadAverage =
                (selfInfo != null && selfInfo.LoadAverage >= 0)
                        ? selfInfo.LoadAverage
                        : Double.MAX_VALUE;  // 情報が無い・負値なら無限大扱い

        // agentname == null でも動くようにしておく
        boolean hasSpec = (agentname != null && DHTutil.containsSpec(agentname));

        if (!hasSpec) {
            // ---- スペック情報が無い場合：単純に LoadAverage が最小のノードを探す ----
            for (String key : InformationCenter.getOthersIPs()) {
                DynamicPCInfo pcInfo = InformationCenter.getOtherDPI(key);
                if (pcInfo == null) {
                    System.err.println(RecommendedDest.class.getName()
                            + ": pcInfo is null for " + key);
                    continue;
                }
                double load = pcInfo.LoadAverage;
                if (load < 0) {
                    continue;
                }
                System.err.println(RecommendedDest.class.getName() + ":" + key + "->" + load);
                if (minLoadAverage > load) {
                    minLoadAverage = load;
                    recomm = key;
                }
            }
        } else {
            // ---- スペック情報がある場合：メモリ条件も見る ----
            SphereSpec spec = DHTutil.getSpec(agentname);
            if (spec == null) {
                System.err.println(RecommendedDest.class.getName()
                        + ": spec is null for " + agentname
                        + ", fallback to LoadAverage only.");
                // スペック取れない場合は上と同じロジックでフォールバック
                for (String key : InformationCenter.getOthersIPs()) {
                    DynamicPCInfo pcInfo = InformationCenter.getOtherDPI(key);
                    if (pcInfo == null) {
                        System.err.println(RecommendedDest.class.getName()
                                + ": pcInfo is null for " + key);
                        continue;
                    }
                    double load = pcInfo.LoadAverage;
                    if (load < 0) continue;
                    if (minLoadAverage > load) {
                        minLoadAverage = load;
                        recomm = key;
                    }
                }
            } else {
                long agentusedmem = spec.memoryused;
                for (String key : InformationCenter.getOthersIPs()) {
                    DynamicPCInfo pcInfo = InformationCenter.getOtherDPI(key);
                    if (pcInfo == null) {
                        System.err.println(RecommendedDest.class.getName()
                                + ": pcInfo is null for " + key);
                        continue;
                    }
                    if (pcInfo.LoadAverage < 0) {
                        continue;
                    }
                    boolean canrecv = canReceive(pcInfo, agentusedmem);
                    System.err.println(RecommendedDest.class.getName() + ":" + key + "->"
                            + pcInfo.LoadAverage + " canrecv->" + canrecv);
                    if (minLoadAverage > pcInfo.LoadAverage && canrecv) {
                        minLoadAverage = pcInfo.LoadAverage;
                        recomm = key;
                    }
                }
            }
        }

        System.err.println(RecommendedDest.class.getName()
                + ":recommend " + recomm + "->" + minLoadAverage);
        ConsolePanel.autoscroll();
        return recomm;
    }

    public static String randomRecomDest() {
        double d = ThreadLocalRandom.current().nextFloat();
        double sum = 0;
        for (Entry<String, Double> a : Scheduler.getMigratehint().entrySet()) {
            sum += a.getValue();
            if (d < sum) {
                return a.getKey();
            }
        }
        return IPAddress.myIPAddress;
    }

    public static String RecomDest() {
        String address = Scheduler.getMigrateTickets().poll();
        return address != null ? address : IPAddress.myIPAddress;
    }

}