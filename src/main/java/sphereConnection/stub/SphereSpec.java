package sphereConnection.stub;

import java.io.Serializable;

public class SphereSpec implements Serializable{
	public long spec;
	public long time;
	public long memoryused;
	public SphereSpec(long spec,long time,long memory) {
		this.spec=spec;
		this.time=time;
		this.memoryused=memory;
	}
}

//AgentをPCで動かす際に必要なスペックリスト