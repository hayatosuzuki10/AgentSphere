package primula.api.core.backup;

import java.util.HashMap;
import java.util.Map;

/*
 * マシンの持っているバックアップファイルを格納
 * Key : エージェントID
 * Value : バックアップファイル名
 * @author satoh
 */

public class BackupTable {
	private static Map<String, String> backupTable = new HashMap();

	public static void set(String ID, String fileName) {
		backupTable.put(ID, fileName);
	}

	public static boolean containsKey(String key) {
		return backupTable.containsKey(key);
	}

	public static Map<String, String> getBackupMap() {
		return backupTable;
	}

	public static String getFileName(String ID) {
		return backupTable.get(ID);
	}

	public static void remove(String key){
		backupTable.remove(key);
	}

	// keyとvalueの一覧表示（たぶん使わない）
	public static void print(){
		for (Map.Entry<String, String> e : backupTable.entrySet()) {
			System.out.println("key=" + e.getKey() + ", value=" + e.getValue());
		}
	}
	
	public static String getKey() {
		String buk = null;
		for(Map.Entry<String, String> e : backupTable.entrySet()) {
			buk = e.getKey();
		}
		return buk;
	}
}
