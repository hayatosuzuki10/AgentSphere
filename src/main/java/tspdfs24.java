import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;


 
	@SuppressWarnings("serial")
	public class tspdfs24 extends AbstractAgent implements IMessageListener 

    {
    	private int size=24, start;
    	private int best_path[]=new int[size];
		private int best_cost=999999;
		private int Matrix[][]=new int[size][size];
		private int visited[]=new int[size+1];
		private String parentid;
		private int[][] pack=new int[3][size];
		
        public void setattr (int[][] incoming, String parent, int i)
        
        {
        	
        	this.parentid=parent;
        	this.Matrix=incoming;
        	this.start=i;
           // stack = new Stack<Integer>();

        }
        public tspdfs24() {
        	super();
			// TODO Auto-generated constructor stub
		}
		public void setcost(int incoming){
        	best_cost=incoming;
        }
        public void setmatrix(int[][] incoming){
        	Matrix=incoming;
        }
        public int getmatrix(int index1,int index2){
        	return Matrix[index1][index2];
        }
        public int getbest_cost(){
        	return best_cost;
        }
        public void setbest_cost(int value){
        	best_cost=value;        	
        }
        public void setbest_path(int index, int value){
        	best_path[index]=value;       	
        }


     public void dfs(int city, int visited_in[], int path_in[], int path_i_in, int cost_in){
if (cost_in<best_cost){
	int visited[] = new int[size+1], path[] = new int[size+1];
	int path_i=path_i_in, cost=cost_in, i;
	for(i=0;i<size;i++){
		visited[i]=visited_in[i];
		path[i]=path_in[i];
	}
	
	visited[city]=1;
	path[path_i]=city;
	path_i++;
	int leaf=0;
	for(i=0;i<size;i++){
		if(visited[i]==0){
			leaf++;
			dfs(i, visited, path, path_i, cost+Matrix[city][i]);
		}
	}
	
	if (leaf==0){
		
		cost+=Matrix[city][0];
		path[path_i]=0;
		path_i++;
		if(cost <= best_cost){
		System.out.println("Found new best cost:"+cost);
			setbest_cost(cost);
			for(i=0;i<size;i++)
				setbest_path(i,path[i]);
		}
	}
	
}
  
     }

       
		public void run()
//        
        {
			KeyValuePair<InetAddress, Integer> address = null;
			try {
				address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Master),55878);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//this.migrate();
			try {
			MessageAPI.registerMessageListener(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			int maxsize=24;
			
			
			int size=maxsize;
            long lStartTime = System.currentTimeMillis();
                //System.out.println("Enter the number of nodes in the graph");               
           
                try{
                	System.out.println("Waiting for master...");
                		                Thread.sleep(3000); //3000ミリ秒Sleepする

                		                }catch(InterruptedException e){}
              
System.out.println();
          
                
                System.out.println("the citys are visited as follows");

                for (int i=start; i<start+7; i++){
                int visited[]=new int[size+1];
                int path[] = new int[size+1];
                int cost=Matrix[0][1], path_i=1;
                //System.out.println("Cost="+Matrix[0][1]);
                path[0]=0;
                visited[0]=1;

               // for (int i=1; i<maxsize; i++){
                dfs(i, visited, path, path_i, cost);
                pack[0][0]=start;
                pack[1][0]=best_cost;
                pack[2]=best_path;
                System.out.println("Sending...");
                StandardEnvelope envelope = new StandardEnvelope(new AgentAddress(parentid), new StandardContentContainer(pack));
        		MessageAPI.send(address, envelope);
        		System.out.println("Sent");
        		// tspNearestNeighbour.tsp(Matrix);
        		visited=null;
        		path=null;
               }
System.out.println("Final best cost is:"+best_cost+" by "+this.getAgentID());
System.out.print("Best path is:"+best_path[0]);
for (int p=1;p<maxsize;p++){
	System.out.print("->"+best_path[p]);	
}
long lEndTime = System.currentTimeMillis();
long difference = lEndTime - lStartTime;
System.out.println("Elapsed milliseconds: " + difference);
            

        }

		@Override
		public void requestStop() {
			// TODO Auto-generated method stub
			
		}



		@Override
		public String getStrictName() {
			// TODO Auto-generated method stub
			return this.getAgentID();
		}



		@Override
		public String getSimpleName() {
			// TODO Auto-generated method stub
			return this.getAgentName();
		}



		@Override
		public void receivedMessage(AbstractEnvelope envelope) {
			StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
			System.out.println("Received new best cost "+(Integer) cont.getContent()+" from master!");
			setcost((Integer) cont.getContent());
		}



		



		

    }