import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.agent.loader.multiloader.GhostClassLoader;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;


public class MoveTestAgent extends AbstractAgent implements IMessageListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<Integer, Integer> agentPer;

	@Override
	public void runAgent() {
        try {
			MessageAPI.registerMessageListener(this);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		GhostClassLoader gcl = GhostClassLoader.unique;
        Class<?> cls = null;
		try {
			cls = gcl.loadClass("ThreadTest");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Class<?>[] types = { int.class, String.class };
		Constructor constructor = null;
		try {
			constructor = cls.getConstructor(types);
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		}
		
		for(int len=1; len<10; len++) {
			agentPer = new HashMap();
			AbstractAgent agent[];
			agent = new AbstractAgent[len];

			for(int i=0; i<len; i++) {
				Object[] args = { i, this.getStrictName() };
				try {
					agent[i] = (AbstractAgent) constructor.newInstance(args);
					System.out.println(agent[i].getAgentID());
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				AgentAPI.runAgent(agent[i]);
			}
			long sum = 0;
			for(int i=0; i<agent.length; i++) {
				while(!agentPer.containsKey(i)) {
					;
				}
				System.out.println("エージェント" + i + " : " + agentPer.get(i));
				sum += agentPer.get(i);
//				System.out.println("空きメモリ : " + Runtime.getRuntime().freeMemory()/(1024 * 1024) + "MB");
			}
			System.out.println("thread : " + len + " 合計値 : " + sum);
			agentPer.clear();
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public String getStrictName() {
        return getAgentID();
	}

	@Override
	public String getSimpleName() {
        return getAgentName();
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
        StandardContentContainer content = (StandardContentContainer)envelope.getContent();
        int[] box = (int[]) content.getContent();
        agentPer.put(box[0], box[1]);
	}

}
