package ranporkTest.tsp;

import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.GlobalConfig;
import primula.agent.AbstractAgent;
import ranporkTest.tsp.util.TspUtil;

public class SingleTspAgent extends AbstractAgent {

	private int[][] tspmap;
	private int min_cost = Integer.MAX_VALUE;
	private int[] min_path;
	private Lock minLock;

	@Override
	public void run() {
		minLock = new ReentrantLock();
		try {
			//tspmap = TspUtil.readMap("./others/tsp.txt", true);
			tspmap = TspUtil.readMap("./src/main/java/ranporkTest/tsp/data/tsp.txt", true);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		BitSet visited = new BitSet(tspmap.length);
		visited.set(0);
		int[] path = new int[tspmap.length];
		min_path = new int[tspmap.length];
		dfs(tspmap, visited, path, tspmap.length, 0, 1);
		for (int i : min_path) {
			System.out.print(i + "->");
		}
		System.out.println();
		System.out.println("mincost:"+min_cost);
	}

	private void dfs(int[][] map, BitSet visited, int[] path_in, int mapSize, int cost_in, int depth) {
		if (depth == mapSize) {//=>葉=>ルート構築完了
			int cost = cost_in + map[path_in[depth - 1]][path_in[0]];
			minLock.lock();
			try {
				if (cost < min_cost) {

					System.out.println(cost);
					min_cost = cost;
					System.arraycopy(path_in, 0, min_path, 0, path_in.length);
					for (int i : min_path) {
						System.out.print(i + "->");
					}
					System.out.println();
				}
			} finally {
				minLock.unlock();
			}
		}

		for (int i = 0; i < mapSize; i++) {
			if (!visited.get(i)) {//未到達
				path_in[depth] = i;
				int cost = cost_in;
				if (depth > 0) {
					cost += map[path_in[depth - 1]][i];
				}

				minLock.lock();
				try {
					if (cost > min_cost) {
						//System.out.println("continue!");
						continue;
					}
				} finally {
					minLock.unlock();
				}
				visited.set(i);
				dfs(map, visited, path_in, mapSize, cost, depth + 1);
				visited.clear(i);
			}
		}
	}

	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ

	}

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		SingleTspAgent agent = new SingleTspAgent();
		Thread loadav = new Thread() {
			public void run() {
				GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LOADAVERAGE, true);
				SystemInfo si = new SystemInfo();
				HardwareAbstractionLayer hal = si.getHardware();
				try {
					sleep(3000);
				} catch (InterruptedException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
				}
				while (true) {
					double[] loadAverage = hal.getProcessor().getSystemLoadAverage(3);
					String[] loadAveragename = { "1 min", "3 min", "15 min" };
					for (int i = 0; i < loadAverage.length; i++) {
						if (loadAverage[i] < 0) {
							System.out.println("CPU load Average " + loadAveragename[i] + ":Non Available.");
						} else {
							System.out.println("CPU load Average " + loadAveragename[i] + ":" + loadAverage[i]);
						}
					}
					try {
						sleep(60*1000);
					} catch (InterruptedException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
				}
			}
		};
		loadav.setDaemon(true);
		loadav.start();
		agent.run();
		long end = System.currentTimeMillis();
		System.out.println("所要時間:" + (end - start) + "ms");
	}

}
