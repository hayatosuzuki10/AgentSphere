package primula.api.core.assh.command.interim;

import java.net.InetAddress;
import java.util.List;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.ScheduleAPI;
import primula.api.core.assh.command.AbstractCommand;
import primula.api.core.scheduler.PerformanceMeasure;
import primula.util.KeyValuePair;

public class vmemory extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance,
			List<String> opt) {
		double vm = PerformanceMeasure.measureVMemory();
		System.out.println("result:"+vm/1000000+" %");

		
		AbstractAgent agent = new AbstractAgent(){

			@Override
			public void requestStop() {
				// TODO 自動生成されたメソッド・スタブ
				
			}
			
		};
		//ここに、仮想メモリ残量を取得して、移動させる命令を書く
		KeyValuePair<InetAddress, Integer> address = new KeyValuePair<InetAddress, Integer>(ScheduleAPI.getHighPerformancedMachine(), 55878);
		AgentAPI.migration(address, agent);
		
		return null;
	}

	
}
