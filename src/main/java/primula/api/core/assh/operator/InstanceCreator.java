package primula.api.core.assh.operator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class InstanceCreator {

	public static Object createNewInstance(String className, List<Object> argList) {
		Object newInstance = null;
		Class clazz = null;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if(argList==null || argList.isEmpty()) {
			try {
				newInstance = clazz.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			List<Class> argTypes = new ArrayList();
			for(int i=0; i<argList.size(); i++) {
				argTypes.add(argList.get(i).getClass());
			}
			Constructor constructor = findConstructor(clazz, argTypes);
			try {
				newInstance = constructor.newInstance(argList);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return newInstance;
	}

	private static Constructor findConstructor(Class clazz, List<Class> argTypes) {
		Constructor returnConstructor = null;
		boolean flag = true;
		boolean onemore = true;
		Constructor[] constructors = null;
		constructors = clazz.getDeclaredConstructors();
		for(Constructor cons : constructors) {
			if(returnConstructor != null) {
				Class[] c = returnConstructor.getParameterTypes();
				for(int i=0; i<c.length; i++) {
					if(c[i].isAssignableFrom(argTypes.get(i)) && !c[i].equals(argTypes.get(i))) {
						onemore = true;
					}
				}
			}
			if(onemore) {
				flag = isExactConstructor(cons, argTypes);
				if(flag) {
					returnConstructor = cons;
					onemore = false;
				}
			}
		}
		return returnConstructor;
	}

	private static boolean isExactConstructor(Constructor constructor, List<Class> argTypes) {
		boolean flag = true;
		Class[] parameterTypes = constructor.getParameterTypes();
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
		}
		return flag;
	}
}
