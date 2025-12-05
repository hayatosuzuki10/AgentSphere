import primula.api.core.agent.function.UsableFunctionAgent;


public class ModuleTestAgent2 extends UsableFunctionAgent{
	private int[] randomNum;
	private int[] array;

	@Override
	protected void runMyTask() {
//		int[] num = {5,10,3};
//		useFunction("SortAgent", 3, num);
		
//		System.out.println("サイズを入力→");
//		String line = null;
//		try {
//			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//			line = reader.readLine();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		int size = Integer.parseInt(line);
		
		useFunction("main.GenerateRandomNumAgent", 5);
		useFunction("main.SortAgent", 5, randomNum);
		for(int n : array) {
			System.out.print(n + " ");
		}
		System.out.println();
	}

	@Override
	protected void receivedResult(String moduleAgentName) {
		if(moduleAgentName.equals("main.GenerateRandomNumAgent")) randomNum = (int[]) openContent();
		if(moduleAgentName.equals("main.SortAgent")) array = (int[]) openContent();
	}

}
