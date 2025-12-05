    import primula.agent.AbstractAgent;


    @SuppressWarnings("serial")
	public class tspdfs extends AbstractAgent //implements IMessageListener 

    {	
		
        



		public tspdfs()

        {
        	super();
           // stack = new Stack<Integer>();

        }
        


     public void dfs(int city, int visited_in[], int path_in[], int path_i_in, int cost_in, int best_cost, int size, int[][] Matrix, int[] best_path){
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
			dfs(i, visited, path, path_i, cost+Matrix[city][i], best_cost, size, Matrix, best_path);
		}
	}
	
	if (leaf==0){
		cost+=Matrix[city][0];
		path[path_i]=0;
		path_i++;
		if(cost <= best_cost){
			System.out.println("Found new best cost:"+cost);
			best_cost=cost;
			for(i=0;i<size;i++)
				best_path[i]=path[i];
		}
	}
	
}
  
     }

       
		public void run()
//        
	
        {
			int maxsize=17;
	        int m_column=maxsize, m_row=maxsize, zed=30;
	        int Matrix[][]=new int[maxsize][maxsize];
		    int visited[]=new int[maxsize+1];
			int best_path[]=new int[maxsize];
			int dummytour[];
	        int best_cost=999999;
			int size=maxsize;
			int jobid=1;
			
			
			System.out.println("I am Alive...");
			/*try {
				MessageAPI.registerMessageListener(this);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			 try{
System.out.println("Waiting for master...");
	                Thread.sleep(3000); //3000ミリ秒Sleepする

	                }catch(InterruptedException e){}

        
            	long lStartTime = System.currentTimeMillis();
                System.out.println("Enter the number of nodes in the graph");              
                      
                	
              //  int Matrix[][] = new int[number_of_nodes + 1][number_of_nodes + 1];                      
                
                System.out.println("the citys are visited as follows");

                tspdfs TestofDFS = new tspdfs();
                
                int path[] = new int[size+1];
                int cost=Matrix[0][1], path_i=1;
                System.out.println("Cost="+Matrix[0][1]);
                path[0]=0;
                visited[0]=1;

                
               // for (int i=1; i<maxsize; i++){
                TestofDFS.dfs(jobid, visited, path, path_i, cost, best_cost, size, Matrix, best_path);
               // tspNearestNeighbour.tsp(Matrix);
               // }
                
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



	/*	@Override
		public String getStrictName() {
			// TODO Auto-generated method stub
			return null;
		}



		@Override
		public String getSimpleName() {
			// TODO Auto-generated method stub
			return this.getAgentName();
		}



		@Override
		public void receivedMessage(AbstractEnvelope envelope) {
			// TODO Auto-generated method stub
			StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
			Matrix = (int[][]) cont.getContent();
		}*/



		



		

    }