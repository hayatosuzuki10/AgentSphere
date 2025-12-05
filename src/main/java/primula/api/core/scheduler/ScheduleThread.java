/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.scheduler;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.network.ExitAgent;
import primula.api.core.scheduler.cpu.CPUPerformaceMeasure;
import primula.api.core.scheduler.vmemory.VMemoryPerformanceMeasure;
import primula.api.core.scheduler.vmemory.VMemoryViewer;

/**
 *
 * @author kurosaki
 */
public class ScheduleThread extends Thread {
	private static MachineInfo myMachineInfo;      // 自分のマシンの情報
	private static List<MachineInfo> machineList;  // 他のマシンの情報
	private static boolean exit = false;           // AgentSphereの停止処理を行うとtrueになる
	private static boolean serverFlag;
	
	// performance測定用変数
	private static PerformanceMeasure performance; // 定期的に0.1秒間のカウントを行う 2014/01/20
	private static CPUPerformaceMeasure cpuPerformance;  // 定期的にエージェントの平均CPU使用率を測定する 2014/01/20
	private static VMemoryPerformanceMeasure vmemoryPerformance; // 定期的にエージェントの仮想メモリ残量を測定する 2014/11/18
	
	private static VMemoryViewer viewer; // debug 2014//11/18
	
	public ScheduleThread(MachineInfo info, PerformanceMeasure performance, boolean isServer) {
		ScheduleThread.myMachineInfo = info;
		ScheduleThread.machineList = new ArrayList();
		ScheduleThread.serverFlag = isServer;
		
		ScheduleThread.performance = performance; // 2014/01/20
		ScheduleThread.cpuPerformance = new CPUPerformaceMeasure(); // 2014/01/20
		ScheduleThread.vmemoryPerformance = new VMemoryPerformanceMeasure(); // 2014/11/18
	}
	
	public ScheduleThread(MachineInfo info, PerformanceMeasure performance, boolean isServer, VMemoryViewer viewer){
		this(info, performance, isServer);
		ScheduleThread.viewer = viewer;
	}

	@Override
	public void run() {
		while(!exit) {
			try {
				Thread.sleep(500);
//				myMachineInfo.setPerformance(performance.measurePerfo());//定期性能測定
				myMachineInfo.setCPUperfo(cpuPerformance.measureCPUperfo()); //定期CPU使用率測定 2014/01/20
				myMachineInfo.setVMperfo(vmemoryPerformance.measureVMemoryPerfo()); //定期仮想メモリ残量測定 2014/11/18
//				System.out.println("性能値：" + myMachineInfo.getPerformance()); // debug
//				System.out.println("CPU性能値：" + myMachineInfo.getCPUperfo()); // debug
//				System.out.println("仮想メモリ残量：" + myMachineInfo.getVMperfo()); // debug 2014/11/18
				viewer.setLabel("仮想メモリ使用率：" + (double)myMachineInfo.getVMperfo()/1000000 + " %");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for(MachineInfo info : machineList) {
				CommunicateAgent communicateAgent = new CommunicateAgent(myMachineInfo, info.getIp().getAddress());
				AgentAPI.runAgent(communicateAgent);
			}
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for(MachineInfo info : machineList) {
			ExitAgent exitAgent = new ExitAgent(myMachineInfo, info.getIp().getAddress());
			AgentAPI.runAgent(exitAgent);
		}
		System.out.println("Scheduler is finished！");
	}
	
	// エージェントがマシンを移動する際に、どのマシンに行くのがよいかをアドバイスする（現状は性能値の高いマシンをアドバイスしている）
	public static InetAddress adviseWhereAgentShouldMigrate() {
		int goodMachine = 0;
		System.out.println("machineList"+machineList);
		long highSpec = machineList.get(0).getPerformance();
		for(int i=1; i<machineList.size(); i++) {
			MachineInfo temp = machineList.get(i);
			if(highSpec < temp.getPerformance() && temp.getVMperfo() < 1*1000000) {
				highSpec = temp.getPerformance();
				goodMachine = i;
			}
		}
		return machineList.get(goodMachine).getIp().getAddress();
	}
	
	/**
	 * 仮想メモリ残量から最適なマシンのアドレスをアドバイスする 2014/11/18
	 * @param newMachineInfo
	 */
	// エージェントがマシンを移動する際に、どのマシンに行くのがよいかをアドバイスする（仮想メモリが30％以上残っていて、性能値の高いマシンをアドバイスしている）
	/*
	public static InetAddress adviseWhereAgentShouldMigrate() {
		int goodMachine = 0;
		System.out.println("machineList"+machineList);
		long highSpec = machineList.get(0).getPerformance();
		for(int i=1; i<machineList.size(); i++) {
			if((double)myMachineInfo.getVMperfo()/1000000 <30 && highSpec < machineList.get(i).getPerformance()) {
				highSpec = machineList.get(i).getPerformance();
				goodMachine = i;
			}
		}
		return machineList.get(goodMachine).getIp().getAddress();
	}
	*/

	// 新しく立ち上がったAgentSphere(マシン)の情報をmachineListに追加する
	public static void addMachineInfo(MachineInfo newMachineInfo) {
		if(serverFlag) {
			for(MachineInfo eachInfo : machineList) {
				SendNewNodeInfoAgent agent = new SendNewNodeInfoAgent(newMachineInfo, eachInfo.getIp().getAddress());
				AgentAPI.runAgent(agent);
			}
		}
		machineList.add(newMachineInfo);
	}

	// すでにあるマシンの性能値を更新する
	public static void resetMachineInfo(MachineInfo info) {
		for(MachineInfo eachInfo : machineList) {
			if(eachInfo.getAgentSphereId().equals(info.getAgentSphereId())) {
				eachInfo.setPerformance(info.getPerformance());
			}
		}
	}

	public static boolean contains(String agentSphereId) {
		for(MachineInfo eachInfo : machineList) {
			if(eachInfo.getAgentSphereId().equals(agentSphereId)) {
				return true;
			}
		}
		return false;
	}

	// 停止処理が行われたAgentSphere(マシン)の情報をmachineListから削除する
	public static void removeMachineInfo(String agentSphereId) {
		for(int i=0; i<machineList.size(); i++) {
			if(machineList.get(i).getAgentSphereId().equals(agentSphereId)) {
				machineList.remove(i);
			}
		}
	}

	// 自分のマシンの情報のセッター
	public static void setMyMachineInfo(MachineInfo myInfo) {
		myMachineInfo = myInfo;
	}

	// 自分のマシンの情報のゲッター
	public static MachineInfo getMyMachineInfo() {
		return myMachineInfo;
	}

	// 他のマシンの情報のセッター
	public static void setMachineList(List<MachineInfo> list) {
		machineList = list;
	}

	// 他のマシンの情報のゲッター
	public static List<MachineInfo> getMachineList() {
		return machineList;
	}

	// AgentSphereの停止処理を行う
	public static void requestStop() {
		exit = true;
	}
}

