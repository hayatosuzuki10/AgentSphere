
import primula.agent.AbstractAgent;

public class ForEvaluation extends AbstractAgent {
	static int count = 0;
	private int mynum;

	public ForEvaluation() {
		count++;
		mynum = count;
	}

	public void run() {
//		System.out.println(Runtime.getRuntime().freeMemory());
		int max = 3000;
//		double[][] a = new double[max][max];
//		double[][] b = new double[max][max];

		Myarray myar=new Myarray(max, max);
		myar.set(1,1,10);
		for(int i=0;i<max;i++) {
			for(int j=0;j<max;j++) {
//				a[i][j] = 10;
//				b[i][j] = 100;
			}
		}

		if(forcedmove) this.migrate();

		for(int k=0;k<1000;k++) {
			for(int i=0;i<max;i++) {
				for(int j=0;j<max;j++) {
//					a[i][j] = a[i][j] * b[i][j];
				}
			}
			if(forcedmove) this.migrate();
//			if(k%100 == 0) System.out.println(k);
		}
		System.out.println("mynum : "+mynum);
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}
}

class Myarray {
	double arr[][];

	public Myarray(int x, int y) {
		arr = new double[x][y];
	}

	void set(int x,int y,int value) {
		arr[x][y]=value;
	}

	double get(int x,int y) {
		return arr[x][y];
	}
}