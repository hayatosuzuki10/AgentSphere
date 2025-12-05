
package primula.api.core.assh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

public class BackupAgent extends AbstractAgent{
	boolean migFlag=false;
	boolean continueFlg;
	AbstractAgent obj;
	byte[] backupfile;
	int backupnum;

	public BackupAgent(AbstractAgent o, byte[] binary, int backup, boolean cf) {
		obj=o;
		backupfile=binary;
		backupnum=backup;
		continueFlg=cf;
	}

	@Override
	public void requestStop() {

	}

	@Override
	public void runAgent() {
		if(!migFlag){
			migFlag=true;
			KeyValuePair<InetAddress, Integer> address = null;
			try {
				address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.IPAddress), 55878);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			AgentAPI.migration(address, this);
		}
		else{
			System.out.println();
			if(continueFlg){
				File file = new File(obj.getAgentName()+"_"+backupnum+".bak");
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(file);
					fos.write(backupfile);
					fos.close();
					System.out.println("バックアップファイル"+obj.getAgentName()+"_"+backupnum+".bakを作成しました");
				} catch (FileNotFoundException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
			//バックアップファイルを更新した時、runAgentが終了した時は古いバックアップファイルを消す
			for(int tmp=backupnum-1;0<tmp;tmp--){
				File oldfile=new File(obj.getAgentName()+"_"+tmp+".bak");
				if(oldfile.exists()){
					oldfile.delete();
					System.out.println(oldfile.getName()+"を削除しました");
				}
			}
		}
	}
}
