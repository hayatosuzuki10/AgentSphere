package primula.api.core.assh.operator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import primula.api.core.assh.ClassTable;

public class MethodOperator {

	public static void showMethod(Object instance) {
		Class clazz = (Class) (ClassTable.isClass(((Class) instance).getSimpleName()) ? instance : instance.getClass());
		Method[] methods = null;
		methods = clazz.getDeclaredMethods();
		for(Method m : methods) {
			System.out.println(m);
		}
	}

	public static Object invokeMethod(Object instance, Class clazz, String methodName, List<Object> argList) {
		Method method = null;
		//System.out.println(instance + " ***** " + clazz + " ***** " + methodName + " ***** " + argList.get(0));/////////////////////
		method = findMethodFromMethodName(instance, clazz, methodName, argList);
		//System.out.println(method);///////////////////////////////////////

		Object result = null;
		try {
			//System.out.println(argList);///////////////////////////////////////////
			Object[] args = new Object[argList.size()];
			for(int i=0; i<args.length; i++) args[i] = argList.get(i);
			result = method.invoke(instance, args);
			//System.out.println(result);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return result;
	}

	// 指定したメソッドの名前から実行するメソッドを見つける
	private static Method findMethodFromMethodName(Object instance, Class clazz, String methodName, List<Object> argList) {
		if(instance != null) clazz = instance.getClass();
		boolean flag = false;
		boolean onemore = true;
		Method[] methods = null;
		Method returnMethod = null;
		methods = clazz.getMethods();
		for(Method method : methods) {
			if(method.getName().equals(methodName)) {
				// メソッドを取得したがそのパラメータに指定した引数の上位クラスがあった場合、もう一度さらにマッチするメソッドがないか確かめる
				if(returnMethod != null) {
					Class[] c = returnMethod.getParameterTypes();
					for(int i=0; i<c.length; i++) {
						if(c[i].isAssignableFrom(argList.get(i).getClass()) && !c[i].equals(argList.get(i).getClass())) {
							onemore = true;
						}
					}
				}
				//System.out.println(method);/////////////////////////////////////
				if(onemore) {
					flag = true;
					if(argList==null || argList.isEmpty()) {
						if(method.getParameterTypes().length != 0) {
							flag = false;
						}
					} else {
						List<Class> argTypes = new ArrayList();
						for(int i=0; i<argList.size(); i++) {
							argTypes.add(argList.get(i).getClass());
						}
						//System.out.println(argTypes);/////////////////////////////////
						flag = isExactMethod(method, argTypes);
					}
					if(flag) {
						returnMethod = method;
						onemore = false;
					}
				}
			}
		}
		return returnMethod;
	}

	// 指定した引数から、見つけたメソッドが正しいかどうか判断する
	private static boolean isExactMethod(Method method, List<Class> argTypes) {
		boolean flag = true;
		Class[] parameterTypes = method.getParameterTypes();
		if(parameterTypes.length == argTypes.size()) {
			for(int i=0; i<parameterTypes.length; i++) {
				Class paraType = null;
				Class argType = null;
				paraType = parameterTypes[i].isPrimitive() ? Converter.boxing(parameterTypes[i]) : parameterTypes[i];
				argType = argTypes.get(i).isPrimitive() ? Converter.boxing(argTypes.get(i)) : argTypes.get(i);
				if(!paraType.isAssignableFrom(argType)) {
					flag = false;
				}
			}
		} else {
			flag = false;
		}
		return flag;
	}
}
