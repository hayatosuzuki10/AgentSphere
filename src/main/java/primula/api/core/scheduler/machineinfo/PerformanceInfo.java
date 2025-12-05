package primula.api.core.scheduler.machineinfo;

import java.io.Serializable;
/*
 * マシンの性能値情報クラス
 */

public class PerformanceInfo implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 8374339383650705092L;
	/**
	 *
	 */
	private int CPUnum;  //CPU数
	private long openningIncrement; //起動時の整数インクリメント
	private long increment;  //0.1秒間の整数インクリメント
	private long totalPMemory;  //物理メモリ合計量
	private long freePMemory;	//物理メモリ残量
	private long totalVMemory;  //取得できる仮想メモリの合計量
	private long freeVMemory;   //取得できる仮想メモリの残量
	private int Agentnum;         //エージェント数

	public PerformanceInfo(){
		this.openningIncrement=0;
		this.CPUnum=0;
		this.increment=0;
		this.totalPMemory=0;
		this.freePMemory=0;
		this.totalVMemory=0;
		this.freeVMemory=0;
		this.Agentnum=0;
	}

	// おそらく最もよく使われるコンストラクタ
	public PerformanceInfo(long openningIncrement){
		this.openningIncrement=openningIncrement;
		this.CPUnum=0;
		this.increment=0;
		this.totalPMemory=0;
		this.freePMemory=0;
		this.totalVMemory=0;
		this.freeVMemory=0;
		this.Agentnum=0;
	}

	public PerformanceInfo(int CPUnum,long oincrement,long increment,long totalPMemory,long freePMemory,long totalVMemory,long freeVMemory,int Agentnum){
		this.openningIncrement=oincrement;
		this.CPUnum=CPUnum;
		this.increment=increment;
		this.totalPMemory=totalPMemory;
		this.freePMemory=freePMemory;
		this.totalVMemory=totalVMemory;
		this.freeVMemory=freeVMemory;
		this.Agentnum=Agentnum;
	}

	public void setPerformanceInfo(int CPUnum,long oincrement,long increment,long totalPMemory,long freePMemory,long totalVMemory,long freeVMemory,int Agentnum){
		this.openningIncrement=oincrement;
		this.CPUnum=CPUnum;
		this.increment=increment;
		this.totalPMemory=totalPMemory;
		this.freePMemory=freePMemory;
		this.totalVMemory=totalVMemory;
		this.freeVMemory=freeVMemory;
		this.Agentnum=Agentnum;
	}
	public void setPerformanceInfo(long increment,long freeVMemory,int Agentnum){
		this.increment=increment;
		this.freeVMemory=freeVMemory;
		this.Agentnum=Agentnum;
	}

	public void setPerformanceInfo(PerformanceInfo performanceinfo){
		this.openningIncrement=performanceinfo.openningIncrement;
		this.CPUnum=performanceinfo.CPUnum;
		this.increment=performanceinfo.increment;
		this.totalPMemory=performanceinfo.totalPMemory;
		this.freePMemory=performanceinfo.freePMemory;
		this.totalVMemory=performanceinfo.totalVMemory;
		this.freeVMemory=performanceinfo.freeVMemory;
		this.Agentnum=performanceinfo.Agentnum;
	}




	public int getCPUnum(){
		return CPUnum;
	}

	public long getopningincrement(){
		return openningIncrement;
	}
	public long getincrement(){
		return increment;
	}


	public long gettotalPMemory(){
		return totalPMemory;
	}

	public long getfreePMemory(){
		return freePMemory;
	}

	public long gettotalVMemory(){
		return totalVMemory;
	}

	public long getfreeVMemory(){
		return freeVMemory;
	}
	public int getAgentnum(){
		return Agentnum;
	}

	public long rateOfincrementdecrease(){
		return 100-(100*increment/openningIncrement);
	}

	public long PMrateOfuse(){
		return 100-(100*freePMemory/totalPMemory);
	}

	public long VMrateOfuse(){
		return 100-(100*freeVMemory/totalVMemory);
	}

	public void Updateincrement(long increment){
		this.increment=increment;
	}

	public void UpdatefreePMemory(long freePMemory){
		this.freePMemory=freePMemory;
	}

	public void UpdatefreeVMemory(long freeVMemory){
		this.freeVMemory=freeVMemory;
	}

	public void UpdateAgentnum(int Agentnum){
		this.Agentnum=Agentnum;
	}
	public String toString(){
		return "[performance]CPU:"+CPUnum+"increment:"+increment+"freePMemory:"+freePMemory+"freeVMemory:"+freeVMemory+"Agentnum:"+Agentnum;
	}


}
