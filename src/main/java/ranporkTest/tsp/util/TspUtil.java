package ranporkTest.tsp.util;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class TspUtil {
	
	public static final String SOURCEDIR="./src/main/java/ranporkTest/tsp";
	
	public static int[][] readMap(String filename, boolean sysout) throws IOException {
		if (sysout)
			System.out.println("Enter the number of nodes in the graph");
		File file = new File(filename);
		Scanner scanner = new Scanner(file);

		int number_of_nodes = scanner.nextInt();

		int Matrix[][] = new int[number_of_nodes][number_of_nodes];
		if (sysout)
			System.out.println("Enter the adjacency matrix");

		for (int i = 0; i < number_of_nodes; i++) {
			for (int j = 0; j < i; j++) {
				Matrix[j][i] = scanner.nextInt();
				Matrix[i][j] = Matrix[j][i];
			}
		}
		//入力内容プロンプト
		if (sysout) {
			for (int i = 0; i < number_of_nodes; i++) {
				for (int j = 0; j < number_of_nodes; j++) {

					//Matrix[i][j] = scanner.nextInt();
					System.out.print(Matrix[i][j] + " ");
				}
				System.out.print("\n");

			}
		}
		scanner.close();
		return Matrix;

	}

	public static int[][] readMap() throws IOException {
		return readMap("./src/ranporkTest/tsp/data/tsp.txt", false);
	}
	public static int[][] readMap(boolean sysout) throws IOException {
		return readMap("./src/ranporkTest/tsp/data/tsp.txt", sysout);
	}
}
