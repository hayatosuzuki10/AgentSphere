package scheduler2022.strategy;

import primula.agent.AbstractAgent;

public interface SchedulerStrategy {
	public void initialize() ;
	
	public void excuteMainLogic();
	
	public void cleanUp();
	
	public boolean shouldMove(AbstractAgent agent);
	
	public String getDestination(AbstractAgent agent);
}
