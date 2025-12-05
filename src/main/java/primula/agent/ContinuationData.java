package primula.agent;

import org.apache.commons.javaflow.api.Continuation;

public class ContinuationData {
	Continuation data = null;
	boolean flag = false;
	AbstractAgent agent = null;
	boolean null_flag = false;
	boolean status_flag = false;

	public Continuation get_data(){
		return data;
	}

	public boolean get_flag(){
		return flag;
	}

	public boolean get_nullflag(){
		return null_flag;
	}

	public AbstractAgent get_agent(){
		return agent;
	}

	public boolean status_flag(){
		return status_flag;
	}
}
