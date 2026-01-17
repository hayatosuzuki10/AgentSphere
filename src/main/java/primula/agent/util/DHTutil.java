package primula.agent.util;

import java.net.InetAddress;
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

    public static void setAgentIP(String strictName, InetAddress addr) {
        if (!checkUUID(strictName))
            throw new IllegalArgumentException("StrictNameがUUIDではありません");
        
        String key = keyCode + IPCode + strictName;
        DHTChordAPI.put(key, addr);
        System.out.println("[DHTutil] PUT key=" + key + " addr=" + addr.getHostAddress());
    }
    

    public static InetAddress getAgentIP(String strictName) {
        if (!checkUUID(strictName))
            throw new IllegalArgumentException("StrictNameがUUIDではありません");

        String key = keyCode + IPCode + strictName;
        InetAddress addr = (InetAddress) DHTChordAPI.get(key);
        System.out.println("[DHTutil] GET key=" + key + " => " + (addr != null ? addr.getHostAddress() : "null"));
        return addr;
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