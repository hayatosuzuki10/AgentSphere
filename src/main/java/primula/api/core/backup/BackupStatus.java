package primula.api.core.backup;

/*
 * バックアップを取る際に必要なデータ
 * moveコマンドが使用された時にKILLFLAGがtrueに変えられる
 * @author satoh
 */

public class BackupStatus {
	static boolean KILLFLAG = false;
	static String ID = null;
	static String IP = null;
	static boolean firstFlag = false;

	public static void setIP(String s) {
		IP = s;
	}

	public static String getIP() {
		return IP;
	}

	public static void setID(String s) {
		ID = s;
	}

	public static String getID() {
		return ID;
	}

	public static void setKillFlag(boolean b) {
		KILLFLAG = b;
	}

	public static boolean getKillFlag() {
		return KILLFLAG;
	}

	public static void clear() {
		KILLFLAG = false;
		ID = null;
		IP = null;
		firstFlag = false;
	}

	public static void setFirstFlag(boolean b) {
		firstFlag = b;
	}

	public static boolean getFirstFlag() {
		return firstFlag;
	}
}
