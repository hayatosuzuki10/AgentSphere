    import java.util.Stack;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;

     

    public class tsp extends AbstractAgent implements IMessageListener

    {

        /**
		 * 
		 */
	
		private int numberOfNodes;
        private Stack<Integer> stack;
        public int adjacency_matrix[][]=null;

        public tsp()

        {
        	super();
            stack = new Stack<Integer>();

        }

     

        public void tsp(int adjacencyMatrix[][])

        {

            numberOfNodes = adjacencyMatrix[1].length - 1;

            int[] visited = new int[numberOfNodes + 1];

            visited[1] = 1;

            stack.push(1);

            int element, dst = 0, i;

            int min = Integer.MAX_VALUE;

            boolean minFlag = false;

            System.out.print(1 + "\t");

     

            while (!stack.isEmpty())

            {

                element = stack.peek();

                i = 1;

                min = Integer.MAX_VALUE;

                while (i <= numberOfNodes)

                {

                    if (adjacencyMatrix[element][i] > 1 && visited[i] == 0)

                    {

                        if (min > adjacencyMatrix[element][i])

                        {

                            min = adjacencyMatrix[element][i];

                            dst = i;

                            minFlag = true;

                        }

                    }

                    i++;

                }

                if (minFlag)

                {

                    visited[dst] = 1;

                    stack.push(dst);

                    System.out.print(dst + "\t");

                    minFlag = false;

                    continue;

                }

                stack.pop();

            }

        }

     

        public void run()
//String... arg
        
        {
        

            System.out.println("I am alive...");

               // number_of_nodes = scanner.nextInt();

        	try {
				MessageAPI.registerMessageListener(this);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


                try{

                Thread.sleep(3000); //3000ミリ秒Sleepする

                }catch(InterruptedException e){}

              
     
                System.out.println("the citys are visited as follows");

                tsp tspNearestNeighbour = new tsp();

                tspNearestNeighbour.tsp(adjacency_matrix);

           

          //  scanner.close();
            MessageAPI.removeMessageListener(this);
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
			System.out.println("Receiving...");
			// TODO Auto-generated method stub
			StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
			adjacency_matrix = (int[][]) cont.getContent();
			
			
			
			
		}



		

    }