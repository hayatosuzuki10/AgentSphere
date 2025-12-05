package primula.api.core.assh.command;

// GetInstanceコマンド

// purimula.api.core.agent.RunningAgentPoolクラスにアクセスし、指定したエージェントのインスタンスを取得する.
// ex. $a=gi AgentMonitorAgent

// 引数を指定しなかった場合、すべてのエージェントを１つのシェル変数に格納する.
// ex. $a=gi
// (このとき $a は配列)

import java.util.ArrayList;
import java.util.List;

import primula.api.AgentAPI;
import primula.api.core.agent.AgentInfo;
import primula.api.core.agent.AgentManager;
import primula.api.core.assh.operator.FieldOperator;
import primula.api.core.assh.operator.MethodOperator;
import primula.api.core.resource.SystemResource;

public class gi extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		data = new ArrayList();
		Object agent = null;

		List<Object> args = new ArrayList();
		String AgentSphereID = SystemResource.getInstance().getAgentSphereId();
		args.add(AgentSphereID);


		if(instance == null) {
			List<AgentInfo> agentInfos = AgentAPI.getAgentInfos().get(AgentSphereID);
			for(int i=0; i<agentInfos.size(); i++) {
				List<Object> args2 = new ArrayList();
				args2.add(i);
				AgentManager manager = (AgentManager) FieldOperator.getFieldValue(null, AgentAPI.class, "AgentManager");
				agent = FieldOperator.getFieldValue(MethodOperator.invokeMethod(
						MethodOperator.invokeMethod(
								FieldOperator.getFieldValue(
										FieldOperator.getFieldValue(
												manager, null, "runningAgentPool")
												, null, "agentPool")
												, null, "get", args)
												, null, "get", args2)
												, null, "Agent");
				data.add(agent);
			}
			return data;
		}


		else {
			List<AgentInfo> agentInfos = AgentAPI.getAgentInfos().get(AgentSphereID);
			List<String> agentsName = new ArrayList();
			for(int i=0; i<agentInfos.size(); i++) {
				agentsName.add(agentInfos.get(i).getAgentName());
			}

			List<String> agentsSimpleName = new ArrayList();
			String name = null;
			for(String agentName : agentsName) {
				for(String asn : agentName.split("\\.")) {
					name = asn;
				}
				agentsSimpleName.add(name);
			}
			List<Object> args2 = new ArrayList();
			for(int i=0; i<agentsSimpleName.size(); i++) {
				if(agentsSimpleName.get(i).equals((String)instance)) {
					args2.add(i);
				}
			}

			AgentManager manager = (AgentManager) FieldOperator.getFieldValue(null, AgentAPI.class, "AgentManager");
			agent = FieldOperator.getFieldValue(MethodOperator.invokeMethod(
					                            MethodOperator.invokeMethod(
					                            FieldOperator.getFieldValue(
					                            FieldOperator.getFieldValue(
					                            manager, null, "runningAgentPool")
					                                   , null, "agentPool")
					                                   , null, "get", args)
					                                   , null, "get", args2)
					                                   , null, "Agent");
			data.add(agent);
			return data;
		}
	}
}
