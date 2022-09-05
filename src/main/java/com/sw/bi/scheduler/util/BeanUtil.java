package com.sw.bi.scheduler.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

import org.springframework.util.StringUtils;

public class BeanUtil extends org.springframework.beans.BeanUtils {
	/**
	 * 获得指定的字段
	 * 
	 * @param clazz
	 * @param name
	 * @return
	 */
	public static Field findDeclaredField(Class<?> clazz, String name) {
		try {
			return clazz.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			if (clazz.getSuperclass() != null) {
				return findDeclaredField(clazz.getSuperclass(), name);
			}

			return null;
		}
	}

	/**
	 * 获得字段声明类型
	 * 
	 * @param clazz
	 * @param name
	 * @return
	 */
	public static Class<?> findFieldType(Class<?> clazz, String name) {
		if (!StringUtils.hasText(name))
			return null;

		String property = name, nativeProperty = null;
		int pos = property.indexOf(".");
		if (pos > -1) {
			property = name.substring(0, pos);
			nativeProperty = name.substring(pos + 1);
		}

		Class<?> typeClazz = null;
		Field fld = findDeclaredField(clazz, property);
		if (fld != null) {
			typeClazz = (Class<?>) fld.getType();

			if (nativeProperty != null) {
				// 是否有定义泛型
				if (ParameterizedType.class.isAssignableFrom(fld.getGenericType().getClass()))
					typeClazz = (Class<?>) ((ParameterizedType) fld.getGenericType()).getActualTypeArguments()[0];

				return findFieldType(typeClazz, nativeProperty);
			}
		}

		return typeClazz;
	}

	public static String convertPropertyName(String name) {
		if (!StringUtils.hasText(name))
			return null;

		String firstLetter = String.valueOf(name.charAt(0)).toLowerCase();
		return firstLetter + name.substring(1);
	}
}
