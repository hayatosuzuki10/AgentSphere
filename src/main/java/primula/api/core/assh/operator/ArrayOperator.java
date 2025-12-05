package primula.api.core.assh.operator;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class ArrayOperator {
	public static int number=0;

	//配列を表示
	public static void showArray(Field f, Object instance){
		try{

			int[] arrayLength=getDimensions(f.get(instance));
			printArray(f.get(instance),arrayLength,f.getName());

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	//多次元配列対応
	private static void printArray(Object array,int[] arrayLength,String name) {
		if(array.getClass().isArray()){
			for(int i=0;i<Array.getLength(array);i++){
				printArray(Array.get(array,i),arrayLength,name);
			}
		}
		else{
			/*
			 1の位:x%5
			 10の位:x/5%4
			 100の位:x/5/4%3
			 */
			System.out.print(name);
			for(int i=0;i<arrayLength.length;i++){
				int number2=number;
				for(int j=arrayLength.length-1;j>i;j--){
					number2/=arrayLength[j];
				}
				System.out.print("["+number2%arrayLength[i]+"]");
			}
			System.out.print("="+array+"\t");
			if(number%3==2)System.out.println();
			number++;
		}
	}

	//int[] array=getDimensions(a[3][4][5])だったらarray[0]=3,array[1]=4,array[2]=5が入る
	public static int[] getDimensions(Object array) {
	    if (!array.getClass().isArray()) {
	        return new int[0];
	    }
	    if (array.getClass().getComponentType().isArray()) {
	        int[] subDimensions = getDimensions(Array.get(array, 0));
	        int[] ret = new int[subDimensions.length + 1];
	        System.arraycopy(subDimensions, 0, ret, 1, subDimensions.length);
	        ret[0] = Array.getLength(array);
	        return ret;
	    } else {
	        return new int[] {Array.getLength(array)};
	    }
	}
	
	
}
