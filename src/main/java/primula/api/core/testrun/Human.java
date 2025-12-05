package primula.api.core.testrun;

import java.util.ArrayList;
import java.util.List;

public class Human extends Thread {
	private List<String> food;

	public Human() {
		food = new ArrayList();
		food.add("choco");
		food.add("apple");
	}

	public void run() {
		int time = 0;
		while(!food.isEmpty()) {
			System.out.println(time++);
			try {
				Thread.sleep(15000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(time%2 == 0) {
			System.out.println("give up!");
		} else {
			System.out.println("finish!");
		}
	}

	public void eat() {
		System.out.println(food.remove(0));
	}

	public void eat(String want) {
		if(food.contains(want)) System.out.println(food.remove(want));
	}
}
