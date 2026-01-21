package scheduler2022;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import primula.api.core.agent.AgentClassInfo;
import primula.util.IPAddress;

public class InformationCenter {

    private static final String myIP = IPAddress.myIPAddress;

    private static DynamicPCInfo myDPI;
    private static StaticPCInfo mySPI;
    private static Set<String> otherIPs;
    private static Map<String, AgentClassInfo> allAgentClassInfo;
    private static Map<String, DynamicPCInfo> otherDPIs;
    private static Map<String, StaticPCInfo> otherSPIs;

    // Locks for thread safety
    private static final Object LOCK_DPI = new Object();
    private static final Object LOCK_SPI = new Object();
    private static final Object LOCK_IPS = new Object();
    private static final Object LOCK_AGENT_INFO = new Object();

    public static DynamicPCInfo getMyDPI() {
        synchronized (LOCK_DPI) {
            return myDPI == null ? null : myDPI.deepCopy();
        }
    }

    public static StaticPCInfo getMySPI() {
        synchronized (LOCK_SPI) {
            return mySPI == null ? null : mySPI.deepCopy();
        }
    }

    public static Set<String> getOthersAgentIDs() {
        synchronized (LOCK_DPI) {
            if (otherDPIs == null || otherDPIs.isEmpty()) return Collections.emptySet();
            Set<String> agentIDs = new HashSet<>();
            for (var e : otherDPIs.entrySet()) {
                DynamicPCInfo dpi = e.getValue();
                if (dpi == null || dpi.Agents == null) continue;
                for (var a : dpi.Agents.values()) {
                    if (a != null && a.ID != null) agentIDs.add(a.ID);
                }
            }
            return agentIDs;
        }
    }

    public static Set<String> getAllAgentIDs() {
        Set<String> agentIDs = new HashSet<>(getOthersAgentIDs());
        synchronized (LOCK_DPI) {
            if (myDPI != null && myDPI.Agents != null) {
                for (var a : myDPI.Agents.values()) {
                    if (a != null && a.ID != null) agentIDs.add(a.ID);
                }
            }
        }
        return agentIDs;
    }

    public static Set<String> getOthersIPs() {
        synchronized (LOCK_IPS) {
            return (otherIPs == null || otherIPs.isEmpty()) ? Collections.emptySet() : new HashSet<>(otherIPs);
        }
    }

    public static Set<String> getAllIPs() {
        Set<String> copySet = new HashSet<>();
        Set<String> others = getOthersIPs();
        if (others != null) {
            copySet.addAll(others);  // ← こちらでコピー
        }
        if (myIP != null) {
            copySet.add(myIP);       // ← これで確実に変更可能な Set に add する
        }
        return copySet;
    }

    public static AgentClassInfo getAgentClassInfo(String agentName) {
        synchronized (LOCK_AGENT_INFO) {
            AgentClassInfo info = allAgentClassInfo.get(agentName);
            return info != null ? info.deepCopy() : null;
        }
    }

    public static Map<String, AgentClassInfo> getAllAgentClassInfo() {
        synchronized (LOCK_AGENT_INFO) {
            return deepCopyMapAgentClassInfos(allAgentClassInfo);
        }
    }

    public static DynamicPCInfo getOtherDPI(String ip) {
        if (ip == null) return null;
        if (ip.equals(myIP)) return getMyDPI();
        synchronized (LOCK_DPI) {
            if (otherDPIs == null) return null;
            DynamicPCInfo dpi = otherDPIs.get(ip);
            return dpi != null ? dpi.deepCopy() : null;
        }
    }

    public static Map<String, DynamicPCInfo> getOthersDPIs() {
        synchronized (LOCK_DPI) {
            if (otherDPIs == null || otherDPIs.isEmpty()) return Collections.emptyMap();
            Map<String, DynamicPCInfo> copyMap = new HashMap<>();
            for (var e : otherDPIs.entrySet()) {
                DynamicPCInfo v = e.getValue();
                copyMap.put(e.getKey(), v != null ? v.deepCopy() : null);
            }
            return copyMap;
        }
    }

    public static Map<String, DynamicPCInfo> getAllDPIs() {
        Map<String, DynamicPCInfo> copyMap = getOthersDPIs();
        synchronized (LOCK_DPI) {
            if (myIP != null && myDPI != null) {
                copyMap.put(myIP, myDPI.deepCopy());
            }
        }
        return copyMap;
    }

    public static StaticPCInfo getOtherSPI(String ip) {
        if (ip == null) return null;
        if (ip.equals(myIP)) return getMySPI();
        synchronized (LOCK_SPI) {
            if (otherSPIs == null) return null;
            StaticPCInfo spi = otherSPIs.get(ip);
            return spi != null ? spi.deepCopy() : null;
        }
    }

    public static Map<String, StaticPCInfo> getOthersSPIs() {
        synchronized (LOCK_SPI) {
            if (otherSPIs == null || otherSPIs.isEmpty()) return Collections.emptyMap();
            Map<String, StaticPCInfo> copyMap = new HashMap<>();
            for (var e : otherSPIs.entrySet()) {
                StaticPCInfo v = e.getValue();
                copyMap.put(e.getKey(), v != null ? v.deepCopy() : null);
            }
            return copyMap;
        }
    }

    public static Map<String, StaticPCInfo> getAllSPIs() {
        Map<String, StaticPCInfo> copyMap = getOthersSPIs();
        synchronized (LOCK_SPI) {
            if (myIP != null && mySPI != null) {
                copyMap.put(myIP, mySPI.deepCopy());
            }
        }
        return copyMap;
    }

    public static void setMyDPI(DynamicPCInfo newDPI) {
        synchronized (LOCK_DPI) {
            myDPI = (newDPI != null ? newDPI.deepCopy() : null);
        }
    }

    public static void setMySPI(StaticPCInfo newSPI) {
        synchronized (LOCK_SPI) {
            mySPI = (newSPI != null ? newSPI.deepCopy() : null);
        }
    }

    public static void setAllIPs(Set<String> newAllIPs) {
        synchronized (LOCK_IPS) {
            if (newAllIPs == null || newAllIPs.isEmpty()) {
                otherIPs = Collections.emptySet();
            } else {
                otherIPs = new HashSet<>(newAllIPs);
                otherIPs.remove(myIP);
            }
        }
    }

    public static void setAllAgentClassInfos(Map<String, AgentClassInfo> newInfos) {
        synchronized (LOCK_AGENT_INFO) {
            allAgentClassInfo = deepCopyMapAgentClassInfos(newInfos);
        }
    }

    public static void setOtherDPIs(Map<String, DynamicPCInfo> newDPIs) {
        synchronized (LOCK_DPI) {
            otherDPIs = deepCopyMapDPI(newDPIs);
        }
    }

    public static void setOtherSPIs(Map<String, StaticPCInfo> newSPIs) {
        synchronized (LOCK_SPI) {
            otherSPIs = deepCopyMapSPI(newSPIs);
        }
    }

    private static Map<String, AgentClassInfo> deepCopyMapAgentClassInfos(Map<String, AgentClassInfo> original) {
        if (original == null || original.isEmpty()) return Collections.emptyMap();
        Map<String, AgentClassInfo> copy = new HashMap<>();
        for (var e : original.entrySet()) {
            AgentClassInfo v = e.getValue();
            copy.put(e.getKey(), v != null ? v.deepCopy() : null);
        }
        return copy;
    }

    private static Map<String, DynamicPCInfo> deepCopyMapDPI(Map<String, DynamicPCInfo> original) {
        if (original == null || original.isEmpty()) return Collections.emptyMap();
        Map<String, DynamicPCInfo> copy = new HashMap<>();
        for (var e : original.entrySet()) {
            DynamicPCInfo v = e.getValue();
            copy.put(e.getKey(), v != null ? v.deepCopy() : null);
        }
        return copy;
    }

    private static Map<String, StaticPCInfo> deepCopyMapSPI(Map<String, StaticPCInfo> original) {
        if (original == null || original.isEmpty()) return Collections.emptyMap();
        Map<String, StaticPCInfo> copy = new HashMap<>();
        for (var e : original.entrySet()) {
            StaticPCInfo v = e.getValue();
            copy.put(e.getKey(), v != null ? v.deepCopy() : null);
        }
        return copy;
    }
}