import java.io.File;
import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.InputMismatchException;
import java.util.Scanner;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;


@SuppressWarnings("serial")
public class tspmaster18 extends AbstractAgent implements IMessageListener{


	private boolean complete=false;

	private int number_of_nodes=0;
	private int best_path[]=new int[number_of_nodes];
	private int best_cost=999999;
	private int[][] pack=new int[number_of_nodes][];
private int count=0;
	private boolean finish=false;

	 tspdfs18 slave1=new tspdfs18();
	 tspdfs18 slave2=new tspdfs18();
	 tspdfs18 slave3=new tspdfs18();
	 tspdfs18 slave4=new tspdfs18();
	 tspdfs18 slave5=new tspdfs18();
	 tspdfs18 slave6=new tspdfs18();
	 tspdfs18 slave7=new tspdfs18();

	 	KeyValuePair<InetAddress, Integer> ToSlave1 = null;
		KeyValuePair<InetAddress, Integer> ToSlave2 = null;
		KeyValuePair<InetAddress, Integer> ToSlave3 = null;
		KeyValuePair<InetAddress, Integer> ToSlave4 = null;
		KeyValuePair<InetAddress, Integer> ToSlave5 = null;
		KeyValuePair<InetAddress, Integer> ToSlave6 = null;
		KeyValuePair<InetAddress, Integer> ToSlave7 = null;
		//KeyValuePair<InetAddress, Integer> ToSlave8 = null;
	public void run(){
		try {
			MessageAPI.registerMessageListener(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Agent Name:" + this.getAgentName());



		try {
			ToSlave1 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave1),55878);
			ToSlave2 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave2),55878);
			ToSlave3 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave3),55878);
			ToSlave4 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave4),55878);
			ToSlave5 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave5),55878);
			ToSlave6 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave6),55878);
			ToSlave7 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave7),55878);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// int number_of_nodes = 0;
		// int adjacency_matrix[][] = null;
         Scanner scanner = null;

         try   {
             System.out.println("Enter the number of nodes in the graph");
             File file=new File("./others/gr18f1.txt");
             try {
					scanner = new Scanner(file);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


        number_of_nodes = scanner.nextInt();

        int Matrix[][]=new int[number_of_nodes][number_of_nodes];
       // int adjacency_matrix[][] = new int[number_of_nodes + 1][number_of_nodes + 1];
     //   adjacency_matrix=adjacency_matrix1;
		  System.out.println("Enter the adjacency matrix");

		  for (int i=0; i<number_of_nodes;i++)
		  {
		  	for (int j=0;j<i;j++){
		  	Matrix[j][i]=scanner.nextInt();
		  	Matrix[i][j]=Matrix[j][i];}
		  }
		                  for (int i = 0; i < number_of_nodes; i++)

		                  {
		                  	 System.out.print( "\n");
		                      for (int j = 0; j < number_of_nodes; j++)

		                      {

		                          //Matrix[i][j] = scanner.nextInt();
		                          System.out.print( Matrix[i][j]+" ");
		                      }

		                  }
		                  slave1.setattr(Matrix, getStrictName(), 1);
		                  slave2.setattr(Matrix, getStrictName(), 4);
		                  slave3.setattr(Matrix, getStrictName(), 7);
		                  slave4.setattr(Matrix, getStrictName(), 10);
		                  slave5.setattr(Matrix, getStrictName(), 13);
		                  slave6.setattr(Matrix, getStrictName(), 16);
		                  slave7.setattr(Matrix, getStrictName(), 17);
		/* tspdfs3 slave1=new tspdfs3(Matrix, getStrictName(), 1);
		 tspdfs3 slave2=new tspdfs3(Matrix, getStrictName(), 7);
		 tspdfs3 slave3=new tspdfs3(Matrix, getStrictName(), 13);*/

		                AgentAPI.migration(ToSlave1, slave1);
		                AgentAPI.migration(ToSlave2, slave2);
		                AgentAPI.migration(ToSlave3, slave3);
		                AgentAPI.migration(ToSlave4, slave4);
		                AgentAPI.migration(ToSlave5, slave5);
		                AgentAPI.migration(ToSlave6, slave6);
		                AgentAPI.migration(ToSlave7, slave7);


  	//	StandardEnvelope envelope = new StandardEnvelope(new AgentAddress(agent.getStrictName()), new StandardContentContainer(Matrix));
  	//	MessageAPI.send(address, envelope);

         } catch (InputMismatchException inputMismatch)

         {

             System.out.println("Wrong Input format");

         }
         while(!finish){
         try{
        	 System.out.println("aaa");
        	  Thread.sleep(10000);
        	}catch (InterruptedException e){
        	}}
         System.out.println("Finished.");
	/*	AgentAPI.migration(address, agent);

		StandardEnvelope envelope = new StandardEnvelope(new AgentAddress(agent.getStrictName()), new StandardContentContainer(adjacency_matrix));
		MessageAPI.send(address, envelope);*/

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
		// TODO Auto-generated method stub
		System.out.println("Received stuff");
		StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
		pack=(int[][]) cont.getContent();
		if(pack[1][0]<best_cost){
		best_cost=pack[1][0];
		best_path=pack[2];

	System.out.println("Received new best result from slave "+pack[0][0]+"\n Best cost found is "+best_cost+"\n best path:");
	for(int i=0; i<number_of_nodes;i++){
		System.out.print(best_path[i]+"->");
	}

	System.out.println("Sending best result to slaves");
	StandardEnvelope env1 = new StandardEnvelope(new AgentAddress(slave1.getStrictName()), new StandardContentContainer(best_cost));
	StandardEnvelope env2 = new StandardEnvelope(new AgentAddress(slave2.getStrictName()), new StandardContentContainer(best_cost));
	StandardEnvelope env3 = new StandardEnvelope(new AgentAddress(slave3.getStrictName()), new StandardContentContainer(best_cost));
	StandardEnvelope env4 = new StandardEnvelope(new AgentAddress(slave4.getStrictName()), new StandardContentContainer(best_cost));
	StandardEnvelope env5 = new StandardEnvelope(new AgentAddress(slave5.getStrictName()), new StandardContentContainer(best_cost));
	StandardEnvelope env6 = new StandardEnvelope(new AgentAddress(slave6.getStrictName()), new StandardContentContainer(best_cost));
	StandardEnvelope env7 = new StandardEnvelope(new AgentAddress(slave7.getStrictName()), new StandardContentContainer(best_cost));
	MessageAPI.send(ToSlave1, env1);
	MessageAPI.send(ToSlave2, env2);
	MessageAPI.send(ToSlave3, env3);
	MessageAPI.send(ToSlave4, env4);
	MessageAPI.send(ToSlave5, env5);
	MessageAPI.send(ToSlave6, env6);
	MessageAPI.send(ToSlave7, env7);

		}
		count++;
		if(count==number_of_nodes){
	finish=true;}
	}
}
