package primula.agent.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import primula.api.DHTChordAPI;

/**
 * Agentが移動した情報をDHTに登録するための便利クラス<br>
 * DHT全体でkeyの衝突を避けるために引数で渡されたkeyとは別に識別符号を付与してDHTにputしたりgetしたりする
 *
 */
public class DHTutil {
    private static final String keyCode = "2022Agent:";
    private static final String IPCode = "IP:";
    private static InetAddress MIGRATING_ADDR;
    static {
        try {
            MIGRATING_ADDR = InetAddress.getByAddress(new byte[] {0, 0, 0, 0}); // 0.0.0.0
        } catch (UnknownHostException e) {
            throw new RuntimeException(e); // ここは絶対起きない想定
        }
    }
    public static void setAgentIP(String strictName, InetAddress addr) {
        if (!checkUUID(strictName))
            throw new IllegalArgumentException("StrictNameがUUIDではありません");
        
        String key = keyCode + IPCode + strictName;
        DHTChordAPI.put(key, addr);
        System.out.println("[DHTutil] PUT key=" + key + " addr=" + addr.getHostAddress());
    }
    
    public static void setMigratingAgentIP(String strictName) {
        if (!checkUUID(strictName))
            throw new IllegalArgumentException("StrictNameがUUIDではありません");

        String key = keyCode + IPCode + strictName;
        DHTChordAPI.put(key, MIGRATING_ADDR);
        System.out.println("[DHTutil] PUT key=" + key + " addr=MIGRATING(0.0.0.0)");
    }

    

    public static InetAddress getAgentIP(String strictName) {
        if (!checkUUID(strictName))
            throw new IllegalArgumentException("StrictNameがUUIDではありません");

        String key = keyCode + IPCode + strictName;

        while (true) {
            InetAddress addr = (InetAddress) DHTChordAPI.get(key);

            if (addr == null) {
                return null;
            }

            if (isMigrating(addr)) {
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                continue;
            }

            System.out.println("[DHTutil] GET key=" + key + " => " + addr.getHostAddress());
            return addr;
        }
    }

    private static boolean isMigrating(InetAddress a) {
        if (a == null) return false;
        return a.isAnyLocalAddress(); // 0.0.0.0 / :: に相当
    }
    
    public static boolean containsAgentIP(String strictName) {
        if (!checkUUID(strictName))
            throw new IllegalArgumentException("StrictNameがUUIDではありません");

        String key = keyCode + IPCode + strictName;
        boolean exists = DHTChordAPI.contains(key);
        System.out.println("[DHTutil] CONTAINS key=" + key + " => " + exists);
        return exists;
    }

    public static void removeAgentIP(String strictName) {
        if (!checkUUID(strictName))
            throw new IllegalArgumentException("StrictNameがUUIDではありません");

        String key = keyCode + IPCode + strictName;
        DHTChordAPI.remove(key);
        System.out.println("[DHTutil] REMOVE key=" + key);
    }

    private static boolean checkUUID(String str) {
        String regex = "[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}";
        Pattern p = Pattern.compile(regex);
        return p.matcher(str).find();
    }
}