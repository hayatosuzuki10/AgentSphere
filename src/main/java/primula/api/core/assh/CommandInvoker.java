package primula.api.core.assh;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class CommandInvoker {


	public static List<Object> invokeCommand(String cmd, List<String> opt, List<String> fileNames, Object instance) {
		List<Object> returnValue = null;

		Class cmdClass = null;
		try {
			cmdClass = Class.forName("primula.api.core.assh.command." + cmd);
		} catch (ClassNotFoundException e) {
			try {
				cmdClass = Class.forName("primula.api.core.assh.command.interim." + cmd);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}
		}

		Object cmdInstance = null;
		try {
			cmdInstance = cmdClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		Method method = null;
		try {
			method = cmdClass.getDeclaredMethod("runCommand", List.class, Object.class, List.class);
			returnValue = (List<Object>)method.invoke(cmdInstance, fileNames, instance, opt);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return returnValue;
	}
}
