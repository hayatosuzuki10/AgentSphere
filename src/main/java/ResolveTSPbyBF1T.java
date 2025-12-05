import java.util.Arrays;
import java.util.Random;

import org.apache.commons.javaflow.api.continuable;

import primula.agent.AbstractAgent;

public class ResolveTSPbyBF1T extends AbstractAgent {

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
		
	}
	

    @Override
    public @continuable void run() {

    	        int n = 14; // 🔥 ここを増やすと指数的に重くなる！
    	        double[][] dist = makeRandomMatrix(n, 10, 100);

    	        System.out.println("Start 11-city brute-force TSP...");
    	        long start = System.currentTimeMillis();

    	        Result result = solve(dist);

    	        long end = System.currentTimeMillis();
    	        System.out.println("Done in " + (end - start) + " ms");
    	        System.out.println("Shortest Path: " + Arrays.toString(result.tour));
    	        System.out.println("Min Cost: " + result.cost);
    	   

    	    
    	
    }
 // ====== 距離行列をランダム生成 ======
    private static double[][] makeRandomMatrix(int n, int min, int max) {
        Random rand = new Random(0);
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) dist[i][j] = 0;
                else dist[i][j] = min + rand.nextInt(max - min + 1);
            }
        }
        return dist;
    }

    // ====== TSP全探索 ======
    public static Result solve(double[][] dist) {
        int n = dist.length;
        int[] perm = new int[n - 1];
        for (int i = 0; i < n - 1; i++) perm[i] = i + 1;

        double best = Double.POSITIVE_INFINITY;
        int[] bestPath = null;
        long count = 0;

        do {
            double cost = 0;
            int prev = 0;
            for (int i = 0; i < perm.length; i++) {
                cost += dist[prev][perm[i]];
                prev = perm[i];
            }
            cost += dist[prev][0]; // 戻る
            if (cost < best) {
                best = cost;
                bestPath = buildTour(perm);
            }
            count++;
            if (count % 1_000_000 == 0) {
                System.out.println("Checked " + count + " routes...");
            }
        } while (nextPermutation(perm));

        System.out.println("Total routes checked: " + count);
        return new Result(bestPath, best);
    }

    private static int[] buildTour(int[] perm) {
        int[] tour = new int[perm.length + 2];
        tour[0] = 0;
        System.arraycopy(perm, 0, tour, 1, perm.length);
        tour[tour.length - 1] = 0;
        return tour;
    }

    // ====== next_permutation ======
    private static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = a.length - 1;
        while (a[j] <= a[i]) j--;
        swap(a, i, j);
        reverse(a, i + 1, a.length - 1);
        return true;
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    private static void reverse(int[] a, int i, int j) {
        while (i < j) swap(a, i++, j--);
    }

    // ====== 結果格納 ======
    public static class Result {
        public final int[] tour;
        public final double cost;
        public Result(int[] tour, double cost) { this.tour = tour; this.cost = cost; }
    }

}
