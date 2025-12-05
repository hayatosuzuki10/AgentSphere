package scheduler2022.server;

import java.io.Serializable;

public class SchedulerConfig implements Serializable {
    public String strategy;
    public int interval;
    public long agentObserveTime;
    public long remigrateProhibitTime;
    public double agentEMAAlpha;
    
    public SchedulerConfig() {
    	
    }
    
    public SchedulerConfig(
    	    String strategy,
    	    int interval,
    	    long agentObserveTime,
    	    long remigrateProhibitTime,
    	   	double agentEMAAlpha) {
    	this.strategy = strategy;
    	this.interval = interval;
    	this.agentObserveTime = agentObserveTime;
    	this.remigrateProhibitTime = remigrateProhibitTime;
    	this.agentEMAAlpha = agentEMAAlpha;
    	
    }
}