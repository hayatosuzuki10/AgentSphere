import java.util.List;

import primula.api.core.agent.function.ModuleAgent;


public class SortAgent extends ModuleAgent{
	private int[] array;
	private int size;

	@Override
	protected void doModule() {
		for(int i=0; i<size-1; i++) {
			int min = i;
			for(int j=i+1; j<size; j++) {
				if(array[j] < array[min]) {
					min = j;
				}
			}
			int temp = array[i];
			array[i] = array[min];
			array[min] = temp;
		}
		System.out.println(array.length + " : length");
		
		finishTask(array);
	}

	@Override
	protected void receivedData(List<Object> data) {
//		System.out.println(data + " : length");
        size = (Integer) data.remove(0);
        array = new int[size];
        array = (int[]) data.remove(0);
	}

}
