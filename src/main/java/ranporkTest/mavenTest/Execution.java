package ranporkTest.mavenTest;

import java.security.SecureRandom;
import java.util.Random;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {
	@Override
	public @continuable void run() {
		Random rnd = new SecureRandom();
		try {

			//こうしないと多次元配列を使用できない説
			MyArrayClass aa = new MyArrayClass();
			aa.set(1, 1, 10);
			System.out.println("MyArrayClass:" + aa.get(1, 1));

			double[][] aaa = new double[2][2];
			//こんな感じに多次元配列にアクセスするコードを書くとJavaFlowのエンハンスでコケる
			//System.out.println(aaa[1][1]);

			//System.out.println(arr);

			//arrsetter(aaa, 1, 1, 1d);//ちなみにここ1(int型)だとエラーになる1d(double)で明示しないといけない)
			//System.err.println(arrgetter(aaa, 1, 1));

			//追記:クラスの多次元配列は問題ない　？？？？？？？？？？？？？？？？？？？？？？？？
			Double[][] aaaa = new Double[2][2];
			System.out.println(aaaa[1][1]);

			Continuation.suspend();
			// LOOP_START
			System.out.println("resumed");

			int r = rnd.nextInt(50);
			if (r != 0) {
				System.out.println("do it again, r=" + r);
				Continuation.again(); // like "GOTO LOOP_START", first statement
										// after closest suspend()
			}

			System.out.println("done");
		} finally {
			// This will be called only once
			System.out.println("Finally is called");
		}
	}

	//こうやってクラスの中に多重配列を隠すとうまくいく
	//なんで？？？？？？
	//英語ガチ勢な方がいればtascalate-javaflow(maven版)のgithubにissue作ってくれるとうれしいな
	class MyArrayClass {
		double a[][];

		public MyArrayClass() {
			a = new double[10][10];
		}

		void set(int a1, int b1, double x) {
			a[a1][b1] = x;
		}

		double get(int a1, int b1) {
			return a[a1][b1];
		}
	}

	//こんなのでもよい
	private static <T> T arrgetter(T arr[][], int x, int y) {
		return arr[x][y];
	}

	//ただしジェネリクス(C++のテンプレートみたいなやつ)はjavaだとクラスでしか使えないので注意
	private static <T> void arrsetter(T arr[][], int x, int y, T value) {
		arr[x][y] = value;
	}
	//double[][] a=new double[10][10];
	//arrsetter(arr,6,4,100);はエラー doubleはプリミティブ型だからクラスじゃない
}