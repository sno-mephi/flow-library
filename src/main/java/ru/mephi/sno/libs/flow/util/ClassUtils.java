package ru.mephi.sno.libs.flow.util;

import java.lang.reflect.Proxy;

public class ClassUtils {

	public static String classTransform(Class<?> clazz) {
		return realClass(clazz).getName();
	}

	public static Class<?> realClass(Class<?> clazz) {
		Class<?> currentClass = clazz;
		while (Proxy.isProxyClass(currentClass)) {
			currentClass = currentClass.getSuperclass();
		}
		return currentClass;
	}
}
