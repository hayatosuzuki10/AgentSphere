/**
 * @author Mikamiyama
 */

//行列積の計算をさせてメモリ消費させるプログラム。

import primula.agent.AbstractAgent;
import primula.api.core.assh.MainPanel;

public class CalcAgent extends AbstractAgent{
	public void run(){
		int N=7000;     //1500MB
		double[][] x=new double[N][N];
		double[][] y=new double[N][N];
		double[][] ans=new double[N][N];

		while(true){
			for(int i=0;i<N;i++){
				for(int j=0;j<N;j++){
					x[i][j]=j;
					y[i][j]=j+1;
				}
			}

			ans=multiple(x,y,N);

			System.out.println("ans="+ans);
			MainPanel.autoscroll();
			try{
				Thread.sleep(3000);
			}catch(InterruptedException e){
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}

	double[][] multiple(double[][] x,double[][] y,int N){
		double[][] ans=new double[N][N];

		for(int i=0;i<N;i++){
			System.out.println(i);
			if(i==10) {
				this.migrate();
			}
			MainPanel.autoscroll();
			for(int j=0;j<N;j++){
				double sum=0;

				for(int k=0;k<N;k++){
					sum+=x[i][k]*y[k][j];
				}

				ans[i][j]=sum;
			}
		}

		return ans;
	}

	@Override
	public void requestStop(){
		// TODO 自動生成されたメソッド・スタブ
	}
}