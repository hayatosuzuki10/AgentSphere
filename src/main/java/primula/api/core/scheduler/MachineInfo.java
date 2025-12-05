package primula.api.core.scheduler;

import java.io.Serializable;

import primula.api.AgentAPI;
import primula.api.core.network.dthmodule2.address.Address;


public class MachineInfo implements Serializable {

	private String agentSphereId; // AgentSphereID
	//private InetAddress ip;       // IPアドレス
	private Address ip;
	private byte[] mac;           // mac値
	private long performance;     // 性能値
	private long CPUperfo;  // エージェントの平均CPU使用率
	private long VMperfo; // 仮想メモリ残量 14.11.18

	public MachineInfo(String ID, Address ip) {
		this.agentSphereId = ID;
		this.ip = ip;
		this.mac = null;
		this.performance = 0;
		this.CPUperfo = 0;
	}

	public MachineInfo(String ID, Address ip, byte[] mac, long performance) {
		this.agentSphereId = ID;
		this.ip = ip;
		this.mac = mac;
		this.performance = performance;
	}

	// AgentSphereIDのセッター
	public void setAgentSphereId(String agentSphereId) {
		this.agentSphereId = agentSphereId;
	}

	// AgentSphereIDのゲッター
	public String getAgentSphereId() {
		return this.agentSphereId;
	}

	// IPアドレスのセッター
	public void setIp(Address ip) {
		this.ip = ip;
	}

	// IPアドレスのゲッター
	public Address getIp() {
		return this.ip;
	}

	// マシンの性能値のセッター
	public void setPerformance(long performance) {
		this.performance = performance;
	}

	// マシンの性能値のゲッター
	public long getPerformance() {
		return this.performance;
	}
	
	// エージェントの平均CPU使用率のセッター
	public void setCPUperfo(long CPUperfo) {
		this.CPUperfo = CPUperfo;
	}

	// エージェントの平均CPU使用率のゲッター
	public long getCPUperfo() {
		return this.CPUperfo;
	}
	
	/**
	 *  仮想メモリ残量のセッター 14.11.18
	 * @param VMperfo
	 */
	public void setVMperfo(long VMperfo){
		this.VMperfo = VMperfo;
	}
	
	/**
	 *  仮想メモリ残量のゲッター 14.11.18
	 * @return
	 */
	public long getVMperfo(){
		return this.VMperfo;
	}
	

	// 自マシン内に存在するエージェントの数のゲッター
	public int getAgentQuantity() {
		return AgentAPI.getAgentInfos().get(agentSphereId).size();
	}
}
