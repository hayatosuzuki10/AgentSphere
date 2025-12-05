package primula.api.core.assh.operator;

import java.lang.reflect.Field;

public class FieldOperator {

	public static void showField(Object instance) {
		//Class clazz = (Class) (ClassTable.isClass(((Class) instance).getSimpleName()) ? instance : instance.getClass());
		Class clazz = Converter.instanceToClass(instance);
		Field[] fields = null;
		fields = clazz.getDeclaredFields();
		for(Field f : fields) {
			System.out.println(f);
		}
	}

	public static void showFieldValue(Object instance) {
		//Class clazz = (Class) (ClassTable.isClass(((Class) instance).getSimpleName()) ? instance : instance.getClass());
		Class clazz = Converter.instanceToClass(instance);
		Field[] fields = null;
		fields = clazz.getDeclaredFields();
		for(Field f : fields) {
			f.setAccessible(true);
			try {
				System.out.println(f.getName() + " : " + f.get(instance));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	public static Object getFieldValue(Object instance, Class clazz, String arg) {
		Object value = null;
		if(instance != null) clazz = instance.getClass();
		Field field = null;
		try {
			field = clazz.getDeclaredField(arg);
		} catch (NoSuchFieldException e1) {
			e1.printStackTrace();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}
		field.setAccessible(true);
		try {
			value = field.get(instance);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		//System.out.println(value + "=====================");///////////////////////////////////
		return value;
	}

	public static void changeFieldValue(Object instance, Class staticClass, String arg, Object newVal) {
		//System.out.println(instance + " " + arg + " " + newVal);/////////////////////////
		Class clazz = instance == null ? staticClass : instance.getClass();
		Field field = null;
		try {
			field = clazz.getDeclaredField(arg);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		field.setAccessible(true);
		try {
			field.set(instance, newVal);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
