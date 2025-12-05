import primula.agent.AbstractAgent;


public class UseCPUAgent extends AbstractAgent{
	private int count = 100000;

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
		
	}
	
	public void runAgent() {
		while(count > 0) {
			for(int i=1; i<1000000; i++) {
				i*=2;
				i/=2;
			}
			count*=2;
			count/=2;
			count--;
			if(count >99985)
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
//			System.out.println(count);
		}
	}

}