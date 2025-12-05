package primula.api.core.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.util.KeyValuePair;

/*
 * エージェントを途中で移動させるためのメソッド
 * AgentAPI.backup()がソース内に含まれるエージェントのみ移動可
 * @author satoh
 * @param agentID UUIDで表されたエージェントID
 * @param IP 送り先IPアドレス
 */

public class AgentMove {

	public synchronized static void move(String agentID, String ip) {
		BackupStatus.setID(agentID);
		BackupStatus.setIP(ip);

		// テーブルに含まれていたらバックアップファイルがあるのでそれを読み取って送る
		// 無かったらまだバックアップファイルが作られてないのでobjをそのまま送る
		if(BackupTable.containsKey(agentID)) {
			ObjectIO oio = new ObjectIO();
			File file = new File(BackupTable.getFileName(agentID));
			FileInputStream fin = null;
			Object object = null;
			KeyValuePair<InetAddress, Integer> address = null;
			try {
				address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(ip), 55878);
			} catch(UnknownHostException e) {
				e.printStackTrace();
			}



			try {
				fin = new FileInputStream(file);
				byte[] b = new byte[(int)file.length()];
				fin.read(b);
				object = oio.getObject(b);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			AgentAPI.migration(address, (AbstractAgent)object);
			BackupStatus.setKillFlag(true);
			System.out.println(agentID + "を" + ip + "に送信しました");
		} else {
			BackupStatus.setKillFlag(true);
			BackupStatus.setFirstFlag(true);
			System.out.println(agentID + "を" + ip + "に送信します");
		}
	}

	public synchronized static void move() {
		String agentID = BackupTable.getKey();
		BackupStatus.setID(agentID);
		BackupStatus.setIP("133.220.114.244");

		// テーブルに含まれていたらバックアップファイルがあるのでそれを読み取って送る
		// 無かったらまだバックアップファイルが作られてないのでobjをそのまま送る
		if(BackupTable.containsKey(agentID)) {
			ObjectIO oio = new ObjectIO();
			File file = new File(BackupTable.getFileName(agentID));
			FileInputStream fin = null;
			Object object = null;
			KeyValuePair<InetAddress, Integer> address = null;
			try {
				address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName("133.220.114.244"), 55878);
			} catch(UnknownHostException e) {
				e.printStackTrace();
			}



			try {
				fin = new FileInputStream(file);
				byte[] b = new byte[(int)file.length()];
				fin.read(b);
				object = oio.getObject(b);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			AgentAPI.migration(address, (AbstractAgent)object);
			BackupStatus.setKillFlag(true);
			System.out.println(agentID + "を" + "133.220.114.244" + "に送信しました");
		} else {
			BackupStatus.setKillFlag(true);
			BackupStatus.setFirstFlag(true);
			System.out.println(agentID + "を" + "133.220.114.244" + "に送信します");
		}
	}
}
