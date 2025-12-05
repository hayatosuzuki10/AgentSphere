/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.SystemAPI;
import primula.api.core.agent.exception.MigrateException;
import primula.api.core.scheduler.MachineInfo;
import primula.api.core.scheduler.ScheduleThread;
import primula.util.IPAddress;
import primula.util.KeyValuePair;

/**
 *
 * @author yamamoto
 */
public class GetNodeAgent extends AbstractAgent {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String myAgentSphereID = SystemAPI.getAgentSphereId();
	private KeyValuePair<InetAddress, Integer> firstAccessAddress;
	private KeyValuePair<InetAddress, Integer> myMachineAddress;
	private boolean returnflg = false;
	private boolean first = true;
	private List<MachineInfo> machineList = new ArrayList<MachineInfo>();
	private MachineInfo myMachineInfo;

	public GetNodeAgent(MachineInfo info) {
		this.myMachineInfo = info;
	}

	@Override
	public void runAgent() {

		try {
			if (first) {// 起動時の処理
				// サーバーノードのIP設定
				try {
					firstAccessAddress = new KeyValuePair<InetAddress, Integer>(
							Inet4Address.getByName(IPAddress.IPAddress), 55878);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				// 帰ってくる用のIP設定
				try {
					myMachineAddress = new KeyValuePair<InetAddress, Integer>(
							Inet4Address.getByName(IPAddress.myIPAddress),
							55878);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				first = false;
				AgentAPI.migration(firstAccessAddress, this);
				throw new MigrateException("!!!");
			} else if (returnflg == false) {// サーバーノードへ移動後の処理
				System.out.println("1");
				for(MachineInfo info : ScheduleThread.getMachineList()) {
					machineList.add(info);
				}
//				machineList = ScheduleThread.getMachineList();
				machineList.add(ScheduleThread.getMyMachineInfo());
				System.out.println("2");
				for(MachineInfo info : ScheduleThread.getMachineList()){
					System.out.println(info.getIp());
				}
				if (!ScheduleThread.getMyMachineInfo().getAgentSphereId().equals(myMachineInfo.getAgentSphereId()) && !ScheduleThread.contains(myMachineInfo.getAgentSphereId())) {
                    ScheduleThread.addMachineInfo(myMachineInfo);                            // 移動先のSchedulerのmachineListに自分の生まれたマシンの情報を追加
                }
				returnflg = true;
				AgentAPI.migration(myMachineAddress, this);
				throw new MigrateException("!!!");
			} else {// 帰還後の処理
				for (MachineInfo info : machineList) {
					if (ScheduleThread.contains(info.getAgentSphereId()) == false && !info.getAgentSphereId().equals(myAgentSphereID)) {
						ScheduleThread.addMachineInfo(info);
					}
				}

			}
		} catch (MigrateException e) {
			System.err.println("GetNodeAgent delete");
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

}
