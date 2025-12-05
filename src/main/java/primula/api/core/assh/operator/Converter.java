package primula.api.core.assh.operator;

import primula.api.core.assh.ClassTable;

public class Converter {

    public static Class boxing(Class primitive) {
    	Class wrapper = null;
    	if(primitive == char.class) wrapper = Character.class;
    	else if(primitive == byte.class) wrapper = Byte.class;
    	else if(primitive == short.class) wrapper = Short.class;
    	else if(primitive == int.class) wrapper = Integer.class;
    	else if(primitive == long.class) wrapper = Long.class;
    	else if(primitive == float.class) wrapper = Float.class;
    	else if(primitive == double.class) wrapper = Double.class;
    	else if(primitive == boolean.class) wrapper = Boolean.class;
    	return wrapper;
    }

    public static Object valueTypeConverter(String s) {
    	Object newVal = null;
    	if(s.contains("\"")) newVal = s.substring(1, s.length()-1);
    	else if(s.contains("\'")) newVal = s.substring(1, s.length()-1).charAt(0);
    	else if(s.contains("b") ) newVal = Byte.valueOf(s.substring(0, s.length()-1));
    	else if(s.contains("s") ) newVal = Short.valueOf(s.substring(0, s.length()-1));
    	else if(s.contains("l") ) newVal = Long.valueOf(s.substring(0, s.length()-1));
    	else if(s.contains("f") ) newVal = Float.valueOf(s.substring(0, s.length()-1));
    	else if(s.contains("d") ) newVal = Double.valueOf(s.substring(0, s.length()-1));
    	else if(s.contains("true") || s.contains("false")) newVal = Boolean.valueOf(s);
    	else newVal = Integer.valueOf(s);
    	return newVal;
    }

    public static Class instanceToClass(Object instance) {
    	Class clazz = null;
    	if(instance.getClass()==String.class && ClassTable.isClass((String) instance)) clazz = ClassTable.getClass((String) instance);
    	else clazz = instance.getClass();
    	return clazz;
    }
}
