

import primula.agent.AbstractAgent;

public class ForEvaluationInt extends AbstractAgent{
	static int count=0;
	private int mynum;

	public ForEvaluationInt(){
		count++;
		mynum = count;
	}

	public void run() {
		int max = 3000;
		int[][] a = new int[max][max];
		int[][] b = new int[max][max];

		for(int i=0;i<max;i++) {
			for(int j=0;j<max;j++) {
				a[i][j] = 10;
				b[i][j] = 100;
			}
		}

		for(int k=0;k<1000;k++) {
			for(int i=0;i<max;i++) {
				for(int j=0;j<max;j++) {
					a[i][j] = a[i][j] * b[i][j];
				}
			}
//			if(k%100 == 0) System.out.println(k);
		}
		System.out.println("mynum : "+mynum);
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}
}
