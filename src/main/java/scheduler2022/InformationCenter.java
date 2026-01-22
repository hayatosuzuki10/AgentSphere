package scheduler2022;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo.Agent;

/**
 * クラスタ内のノード・エージェント情報を集約するセンタークラス。
 * <p>
 * - 自ノードの DPI/SPI
 * - 他ノードの DPI/SPI
 * - IP / Agent ID セット
 *
 * を静的フィールドとして保持し、各 getter では
 * 可能な限り null を返さず「空集合／空マップ」を返すようにしている。
 */
public class InformationCenter {

    /** 自ノードの IP アドレス（起動時に確定） */
    private static final String myIP = IPAddress.myIPAddress;

    /** 自ノードの動的情報（null の可能性あり） */
    private static DynamicPCInfo myDPI;

    /** 自ノードの静的情報（null の可能性あり） */
    private static StaticPCInfo mySPI;

    /** 他ノードの IP セット（null の可能性あり） */
    private static Set<String> otherIPs;

    /** 他ノードの DPI マップ（ip → DPI）（null の可能性あり） */
    private static Map<String, DynamicPCInfo> otherDPIs;

    /** 他ノードの SPI マップ（ip → SPI）（null の可能性あり） */
    private static Map<String, StaticPCInfo> otherSPIs;

    /* ====================== DPI / SPI getter ====================== */

    /** 自ノードの DynamicPCInfo をディープコピーして返す（未設定なら null） */
    public static DynamicPCInfo getMyDPI() {
        if (myDPI == null) {
            return null;
        }
        return myDPI.deepCopy();
    }

    /** 自ノードの StaticPCInfo をディープコピーして返す（未設定なら null） */
    public static StaticPCInfo getMySPI() {
        if (mySPI == null) {
            return null;
        }
        return mySPI.deepCopy();
    }

    /* ====================== Agent ID 系 ====================== */

    /**
     * 他ノードに存在するエージェントID集合を返す。
     * <p>
     * 他ノード DPI が未設定の場合は空集合。
     */
    public static Set<String> getOthersAgentIDs() {
        if (otherDPIs == null || otherDPIs.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> agentIDs = new HashSet<>();
        for (var e : otherDPIs.entrySet()) {
            DynamicPCInfo dpi = e.getValue();
            if (dpi == null || dpi.Agents == null) continue;

            Map<String, Agent> agents = dpi.Agents;
            for (var e2 : agents.entrySet()) {
                Agent a = e2.getValue();
                if (a != null && a.ID != null) {
                    agentIDs.add(a.ID);
                }
            }
        }
        return agentIDs;
    }

    /**
     * 自ノード + 他ノードに存在する全エージェントID集合を返す。
     * <p>
     * DPI がどこにもない場合は空集合。
     */
    public static Set<String> getAllAgentIDs() {
        Set<String> agentIDs = new HashSet<>(getOthersAgentIDs());

        if (myDPI != null && myDPI.Agents != null) {
            Map<String, Agent> agents = myDPI.Agents;
            for (var e2 : agents.entrySet()) {
                Agent a = e2.getValue();
                if (a != null && a.ID != null) {
                    agentIDs.add(a.ID);
                }
            }
        }
        return agentIDs;
    }

    /* ====================== IP 系 ====================== */

    /**
     * 他ノードの IP セットをコピーして返す。
     * <p>
     * 未設定の場合は空集合。
     */
    public static Set<String> getOthersIPs() {
        if (otherIPs == null || otherIPs.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(otherIPs);
    }

    /**
     * 自ノード + 他ノードを含む IP セットを返す。
     * <p>
     * 他が何もなくても、自分の IP は最低1件入る。
     */
    public static Set<String> getAllIPs() {
        Set<String> copySet = new HashSet<>(getOthersIPs());
        if (myIP != null) {
            copySet.add(myIP);
        }
        return copySet;
    }

    /* ====================== DPI 全体 ====================== */

    /**
     * 指定 IP の DPI を取得する。
     * <p>
     * - IP が自分自身なら myDPI の deepCopy
     * - それ以外なら otherDPIs から該当 DPI の deepCopy
     * - 見つからない場合は null
     */
    public static DynamicPCInfo getOtherDPI(String ip) {
        if (ip == null) {
            return null;
        }
        if (ip.equals(myIP)) {
            return getMyDPI();
        }
        if (otherDPIs == null) {
            return null;
        }
        DynamicPCInfo dpi = otherDPIs.get(ip);
        return dpi != null ? dpi.deepCopy() : null;
    }

    /**
     * 他ノード分の DPI マップ (ip → deepCopy(DPI)) を返す。
     * <p>
     * 未設定の場合は空マップ。
     */
    public static Map<String, DynamicPCInfo> getOthersDPIs() {
        if (otherDPIs == null || otherDPIs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, DynamicPCInfo> copyMap = new HashMap<>();
        for (var e : otherDPIs.entrySet()) {
            DynamicPCInfo v = e.getValue();
            copyMap.put(e.getKey(), v != null ? v.deepCopy() : null);
        }
        return copyMap;
    }

    /**
     * 自ノード + 他ノードの DPI マップ (ip → DPI) を返す。
     * <p>
     * DPI がどこにもない場合は空マップ。
     */
    public static Map<String, DynamicPCInfo> getAllDPIs() {
        Map<String, DynamicPCInfo> copyMap = new HashMap<>(getOthersDPIs());
        if (myIP != null && myDPI != null) {
            copyMap.put(myIP, myDPI.deepCopy());
        }
        return copyMap;
    }

    /* ====================== SPI 全体 ====================== */

    /**
     * 指定 IP の SPI を取得する。
     * <p>
     * - IP が自分自身なら mySPI の deepCopy
     * - それ以外なら otherSPIs から該当 SPI の deepCopy
     * - 見つからない場合は null
     */
    public static StaticPCInfo getOtherSPI(String ip) {
        if (ip == null) {
            return null;
        }
        if (ip.equals(myIP)) {
            return getMySPI();
        }
        if (otherSPIs == null) {
            return null;
        }
        StaticPCInfo spi = otherSPIs.get(ip);
        return spi != null ? spi.deepCopy() : null;
    }

    /**
     * 他ノード分の SPI マップ (ip → deepCopy(SPI)) を返す。
     * <p>
     * 未設定の場合は空マップ。
     */
    public static Map<String, StaticPCInfo> getOthersSPIs() {
        if (otherSPIs == null || otherSPIs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, StaticPCInfo> copyMap = new HashMap<>();
        for (var e : otherSPIs.entrySet()) {
            StaticPCInfo v = e.getValue();
            copyMap.put(e.getKey(), v != null ? v.deepCopy() : null);
        }
        return copyMap;
    }

    /**
     * 自ノード + 他ノードの SPI マップ (ip → SPI) を返す。
     * <p>
     * SPI がどこにもない場合は空マップ。
     */
    public static Map<String, StaticPCInfo> getAllSPIs() {
        Map<String, StaticPCInfo> copyMap = new HashMap<>(getOthersSPIs());
        if (myIP != null && mySPI != null) {
            copyMap.put(myIP, mySPI.deepCopy());
        }
        return copyMap;
    }

    /* ====================== setter 群 ====================== */

    public static void setMyDPI(DynamicPCInfo newDPI) {
        myDPI = (newDPI != null ? newDPI.deepCopy() : null);
    }

    public static void setMySPI(StaticPCInfo newSPI) {
        mySPI = (newSPI != null ? newSPI.deepCopy() : null);
    }

    /**
     * 他ノード IP 一覧（自分以外）をセットする。
     * <p>
     * null が渡された場合は空集合として扱う。
     */
    public static void setAllIPs(Set<String> newAllIPs) {
        if (newAllIPs == null || newAllIPs.isEmpty()) {
            otherIPs = Collections.emptySet();
        } else {
            otherIPs = new HashSet<>(newAllIPs);
            // 念のため自分自身を除外しておきたいなら:
            otherIPs.remove(myIP);
        }
    }

    public static void setOtherDPIs(Map<String, DynamicPCInfo> newDPIs) {
        otherDPIs = deepCopyMapDPI(newDPIs);
    }

    public static void setOtherSPIs(Map<String, StaticPCInfo> newSPIs) {
        otherSPIs = deepCopyMapSPI(newSPIs);
    }

    /* ====================== deepCopy ユーティリティ ====================== */

    private static Map<String, DynamicPCInfo> deepCopyMapDPI(Map<String, DynamicPCInfo> original) {
        if (original == null || original.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, DynamicPCInfo> copy = new HashMap<>();
        for (var e : original.entrySet()) {
            DynamicPCInfo v = e.getValue();
            copy.put(e.getKey(), v != null ? v.deepCopy() : null);
        }
        return copy;
    }

    private static Map<String, StaticPCInfo> deepCopyMapSPI(Map<String, StaticPCInfo> original) {
        if (original == null || original.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, StaticPCInfo> copy = new HashMap<>();
        for (var e : original.entrySet()) {
            StaticPCInfo v = e.getValue();
            copy.put(e.getKey(), v != null ? v.deepCopy() : null);
        }
        return copy;
    }
}