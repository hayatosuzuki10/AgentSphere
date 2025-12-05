import primula.agent.AbstractAgent;


public class LoadedAgent extends AbstractAgent {

	private boolean flag = true;
	private int count = 30;
	private String word = "owata";

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void runAgent() {
		// TODO 自動生成されたメソッド・スタブ

		System.out.println(word);
		System.out.println("OK!");
		for(int i=0; i<5; i++) {
			System.out.println("更新!!!");
		}


		while(count < 20) {
			if(flag) {
				count+=2;
				if(count == 10) {
					flag = false;
				}
				System.out.println(count);
			} else {
				System.out.println("OK!");
				count+=5;
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("更新できるか？");
		}

	}

}
