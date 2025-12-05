import primula.agent.AbstractAgent;


public class newagent extends AbstractAgent{

	public void run(){
		System.out.println("Starting Migration\n");

		this.migrate();

	System.out.println("Migrated");

	for(int i=0; i<100; i++){
		int max=100;
		//if(i==50)this.backup();
		for(int j=0; j<max; j++){

		}
		System.out.println(i);
	}
	System.out.println("finish");
		
	}
	
	
	
	
	
	@Override
	public void requestStop() {
		// TODO Auto-generated method stub
		
	}

}
