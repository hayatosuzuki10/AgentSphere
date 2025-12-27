/**
 *
 */
package primula.api;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;

import primula.agent.AbstractAgent;
import primula.api.core.agent.AgentInstanceInfo;
import primula.api.core.agent.IAgentManager;
import primula.api.core.agent.RunningAgentPoolListener;
import primula.api.core.agent.function.ModuleAgentManager;
import primula.api.core.agent.loader.ClassDataCollectionContainer;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.api.core.backup.BackupStatus;
import primula.api.core.backup.BackupTable;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.SendContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.api.core.resource.ReadyRunAgentPoolListener;
import primula.api.core.resource.ReadySendAgentPoolListener;
import primula.api.core.resource.SystemResource;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import scheduler2022.Scheduler;

/**
 * Agentに関する操作を提供します
 *
 * @author yamamoto
 */
public class AgentAPI {

    private static IAgentManager AgentManager;
    private static SystemResource systemResource = SystemResource.getInstance();
    private static ModuleAgentManager moduleAgentManager = ModuleAgentManager.getInstance();

    private synchronized static IAgentManager getAgentManager() {
        if (AgentManager == null) {
            S2Container factory = SingletonS2ContainerFactory.getContainer();
            AgentManager = (IAgentManager) factory.getComponent("AgentManager");
        }
        return AgentManager;
    }

    /**
     * addressが指すソケットへエージェントの転送を行います
     * <p>
     * あくまでエージェントを直列化して指定ソケットへ転送するだけなので、
     * 強マイグレーションではないので注意
     * <p>
     * 強マイグレーションを行わせたい場合は、Abstract#migrateを仕様してください
     * @param address 転送したいAgentSphereを表すIPとPORT番号
     * @param agent 転送するエージェント
     */
    public synchronized static void migration(KeyValuePair<InetAddress, Integer> address, AbstractAgent agent) {  
    	getAgentManager().migrate(address, agent);
    }
    
//    public synchronized static void migration(KeyValuePair<InetAddress, Integer> address, AbstractAgent agent) {
//        KeyValuePair<String, AbstractAgent> task = new KeyValuePair<String, AbstractAgent>("reply", agent);
//        try {
//            SenderThread.task.put(task);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(AgentAPI.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        KeyValuePair<String, AbstractAgent> result = new KeyValuePair<String, AbstractAgent>();
//        try {
//            while(true){
//                result = ReceiverThread.result.take();
//                if((result.getValue().getAgentID().equals(agent.getAgentID())) == false){
//                    ReceiverThread.result.put(result);
//                }
//                else{
//                    break;
//                }
//            }
//        } catch (InterruptedException ex) {
//            Logger.getLogger(AgentAPI.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        if(result.getKey().equals("success")){
//            getAgentManager().migrate(address, agent);
//        }
//    }
    

    public synchronized static void migration(InetAddress agentSphereInfo, AbstractAgent agent, ClassDataCollectionContainer collectionContainer) {
        throw new UnsupportedOperationException("Not Supported Yes");
    }

    public synchronized static void runAgent(AbstractAgent agent) {
    	new Thread(() -> Scheduler.analyze.analyze(agent.getAgentID()), "Analyze-agent-123").start();
        getAgentManager().runAgent(agent, SystemAPI.getAgentSphereId());
    }

    public synchronized static void runAgent(AbstractAgent agent, String group) {
        getAgentManager().runAgent(agent, group);
    }

    public synchronized static HashMap<String, List<AgentInstanceInfo>> getAgentInfos() {
        return getAgentManager().getAgentInfos();
    }

    public synchronized static void registReadyRunAgentPoolListener(ReadyRunAgentPoolListener listener){
        AgentManager.registReadyRunAgentPoolListener(listener);
    }

    public synchronized static void removeReadyRunAgetPoolListener(ReadyRunAgentPoolListener listener){
        AgentManager.removeReadyRunAgentPoolListener(listener);
    }

    public synchronized static void registReadySendAgentPoolListener(ReadySendAgentPoolListener listener){
        AgentManager.registReadySendAgentPoolListener(listener);
    }

    public synchronized static void removeReadySendAgentPoolListener(ReadySendAgentPoolListener listener){
        AgentManager.removeReadySendAgentPoolListener(listener);
    }

    public synchronized static void registRunningAgentPoolListener(RunningAgentPoolListener listener){
        AgentManager.registRunningAgentPoolListener(listener);
    }

    public synchronized static void removeRunnningAgentPoolListener(RunningAgentPoolListener listener){
        AgentManager.removeRunningAgentPoolListener(listener);
    }

    public synchronized static long getUsedMemory(){
        return systemResource.getUsedMemory();
    }

    public synchronized static long getCommittedMemory(){
        return systemResource.getCommittedMemory();
    }

    public synchronized static int getProcessors(){
        return Runtime.getRuntime().availableProcessors();
    }
    /*
     *
    public synchronized static List<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>> getIPMap(){
        List<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>> list = new ArrayList<KeyValuePair<String, KeyValuePair<InetAddress, Integer>>>();
        for(KeyValuePair<String, KeyValuePair<InetAddress, Integer>> x : NetworkAPI.getAddresses()){
            list.add(x);
        }
        return list;
    }
     */

    public synchronized static void messageSendConf (IMessageListener listener, String replyAgentName, String runAgentName){
        try {
            MessageAPI.registerMessageListener(listener);
            KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress,Integer>(Inet4Address.getByName("192.168.114.12"),55878);
            KeyValuePair<String, String> content = new KeyValuePair<String, String>(replyAgentName, runAgentName);
            MessageAPI.send(address, new StandardEnvelope(new AgentAddress("primula.api.core.interim.testagent.MessageQueueAgent"), new SendContentContainer(content)));
        } catch (Exception ex) {
            Logger.getLogger(AgentAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized static void requestStop(AbstractAgent agent) {
        getAgentManager().requestStop(agent);
    }









    /**
	 * @author satoh
	 * @param backup バックアップするための番号
	 * @param continueFlg バックアップを途中に記述した場合はtrue、最後の場合はfalse	 */
/*
	public synchronized static void backup(AbstractAgent o, int backup, boolean continueFlg){
			try {
				byte[] binary;
			//	File file = new File(o.getAgentName()+"_"+backup+".bak");
				ObjectIO oio = new ObjectIO();
				binary = oio.getBinary(o);

//				FileOutputStream fos = new FileOutputStream(file);
//				fos.write(binary);
//				fos.close();

				BackupAgent ba=new BackupAgent(o,binary,backup,continueFlg);
				ba.runAgent();

			} catch (IOException e) {
				// TODO 閾ｪ蜍慕函謌舌＆繧後◆ catch 繝悶Ο繝・け
				e.printStackTrace();
			}

	}
*/

	public synchronized static boolean backup(AbstractAgent agent, int backupnum, boolean continueFlag){

		// BackupStatusの中のKillFlagを調べ、trueだったらkillflagをtrueにする
		// killflag==true かつ、BackupStatusの中のAgentIDとこのオブジェクトのIDが同じだったら移動が行われたとみなす
		boolean killflag = BackupStatus.getKillFlag();

		if(killflag && BackupStatus.getFirstFlag() && BackupStatus.getID().equals(agent.getAgentID())){ // バックアップがないかつIDが一致する場合
			KeyValuePair<InetAddress, Integer> address = null;
			try {
				address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(BackupStatus.getIP()), 55878);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			BackupStatus.clear();
			AgentAPI.migration(address, (AbstractAgent)agent);
			return true;
		}

		if(killflag && agent.getAgentID().equals(BackupStatus.getID())){  // moveで入力されたIDとエージェントIDが一致する場合
			BackupStatus.clear();
//			System.out.println("clear"); //debag用
		}else{
			killflag=false; // IDが違う場合は停止されない
		}


		//	(!continueFlg && !killflg) もう処理が終わりなのでバックアップファイルを消すのみ
		//	(!continueFlg &&  killflg) もう処理が終わりなのでバックアップファイルを消すのみ
		//	( continueFlg && !killflg) 続いているのでバックアップを取り続け、古いバックアップを消す
		//	( continueFlg &&  killflg) 移動
		if(continueFlag){
			if(!killflag){
				File file = new File(agent.getAgentID() + "_" + backupnum + ".bak");
				FileOutputStream fos = null;
				try {
					byte[] binary;
					ObjectIO oio = new ObjectIO();
					binary = oio.getBinary(agent);
					fos = new FileOutputStream(file);
					fos.write(binary);
					fos.close();
//					System.out.println(agent.getAgentID() + "_" + backupnum + ".bakを作成しました");
					BackupTable.set(agent.getAgentID(), agent.getAgentID()+"_"+backupnum+".bak");  //Mapに追加
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else{
			BackupTable.remove(agent.getAgentID());
		}

		// 古いバックアップが存在するか確かめ、あったら削除
		// 0 < tmp だけどバックアップが非常に多くなった時に処理が重くなるかもしれないのでとりあえず最大５個まで調べる(仮)
		for(int tmp=backupnum-1; 0<tmp && backupnum-5<tmp; tmp--){
			File oldfile=new File(agent.getAgentID() + "_" + tmp + ".bak");
			if(oldfile.exists()){
				boolean bool = false;
				while(!bool) {
					bool = oldfile.delete();
					System.out.print(" ");
				}
//				System.out.println(oldfile.getName()+"を削除しました");
			}
		}
		return killflag;
	}

	// okubo
	// 2013/9/17
	// objects[0] : 処理を依頼したいエージェントの名前
	// objects[1…] : そのエージェントに渡す引数
	public static void useFunction(String client, Object[] objects) {
		String moduleAgentName = (String) objects[0];
		List<Object> data = new ArrayList<Object>();
//		data.add(new Throwable().getStackTrace()[1].getClassName());
		data.add(client);
		data.add(IPAddress.myIPAddress); // 暫定(マーキュリーが完成したら消す)①
		for(int i=1; i<objects.length; i++) data.add(objects[i]);
		moduleAgentManager.useFunction(moduleAgentName, data);
	}
	
	public static AbstractAgent getAgentByID(String id) {
		AbstractAgent agent = AgentManager.getAgentByID(id);
		return agent;
	}
}


