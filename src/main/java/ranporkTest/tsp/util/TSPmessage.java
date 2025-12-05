package ranporkTest.tsp.util;

import java.io.Serializable;

public class TSPmessage implements Serializable{
	public int[] path;
	public int cost;
	public boolean finished;
}
