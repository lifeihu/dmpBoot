package org.springframework.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;
import org.codehaus.jackson.map.type.TypeFactory;
import org.springframework.jackson.CustomDateFormat;
import org.springframework.jackson.ExtendBeanDeserializerFactory;

public class JsonUtil {
	private static final String JSON_NULL = "null";
	private static final String JSON_UNDEFINED = "undefined";
	private static final String JSON_EMPTY = "{}";

	private static final ObjectMapper mapper = new ObjectMapper();
	private static TypeFactory typeFactory = mapper.getTypeFactory();

	static {
		mapper.setDateFormat(CustomDateFormat.getDateFormat());
		mapper.getJsonFactory().configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

		StdDeserializerProvider deserializerProvider = new StdDeserializerProvider(new ExtendBeanDeserializerFactory());
		mapper.setDeserializerProvider(deserializerProvider);
	}

	/**
	 * 将指定对象转化为JSON串
	 * 
	 * @param object
	 * @return
	 */
	public static String encode(Object object) {
		if (object == null)
			return null;

		try {
			return mapper.writeValueAsString(object);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 将JSON串转化为指定的类
	 * 
	 * @param content
	 * @param valueType
	 * @return
	 */
	public static <T> T decode(String content, Class<T> valueType) {
		if (!StringUtils.hasText(content))
			return null;

		content = prepareContent(content);

		try {
			return (T) mapper.readValue(content, valueType);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static <T> T decode(Object content, Class<T> valueType) {
		return (T) decode(encode(content), valueType);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T decode(String content, Class<? extends Collection> collectionType, Class<?> elementType) {
		Collection empty = new ArrayList();

		if (!StringUtils.hasText(content))
			return (T) empty;

		content = prepareContent(content);

		try {
			return (T) mapper.readValue(content, typeFactory.constructCollectionType(collectionType, elementType));
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return (T) empty;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T decode(String content, Class<? extends Map> mapType, Class<?> keyType, Class<?> valueType) {
		Map empty = new HashMap();

		if (!StringUtils.hasText(content))
			return (T) empty;

		content = prepareContent(content);

		try {
			return (T) mapper.readValue(content, mapper.getTypeFactory().constructMapType(mapType, keyType, valueType));
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return (T) empty;
	}

	public static <T> T decode(String content, String property, Class<T> valueType) {
		String json = parser(content, property);
		return (T) decode(json, valueType);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T decode(String content, String property, Class<? extends Collection> collectionType, Class<?> elementType) {
		String json = parser(content, property);
		return (T) decode(json, collectionType, elementType);
	}

	/**
	 * 在JSON串中解析指定的属性值
	 * 
	 * @param content
	 * @param property
	 * @return
	 */
	public static String parser(String content, String property) {
		String[] values = parser(content, new String[] { property });
		return (values != null && values.length >= 0) ? values[0] : null;
	}

	/**
	 * 在JSON串中解析指定的属性值
	 * 
	 * @param content
	 * @param property
	 * @return
	 */
	public static String[] parser(String content, String... properties) {
		if (!StringUtils.hasText(content) || properties == null || properties.length == 0)
			return null;

		String[] values = new String[properties.length];
		Map<String, Object> map = null;

		try {
			map = mapper.readValue(content, typeFactory.constructMapType(HashMap.class, String.class, Object.class));

			for (int i = 0; i < properties.length; i++) {
				String property = properties[i];
				Object value = PropertyUtils.getProperty(map, property);
				if (value instanceof Map || value instanceof Collection || value instanceof Set)
					value = encode(value);

				values[i] = (String) value;
			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		return values;
	}

	public static String prepareContent(String content) {
		if (StringUtils.hasText(content) && content.indexOf(":\"null\"") > -1)
			content = content.replaceAll(":\"null\"", ":null");

		if (StringUtils.hasText(content) && content.indexOf(":\"undefined\"") > -1)
			content = content.replaceAll(":\"undefined\"", ":null");

		return content;
	}

	public static ObjectMapper getObjectMapper() {
		return mapper;
	}

	/**
	 * 是否为空的JSON串
	 * 
	 * @param content
	 * @return
	 */
	public static boolean isBlank(String content) {
		return !StringUtils.hasText(content) || JSON_NULL.equalsIgnoreCase(content) || JSON_EMPTY.equalsIgnoreCase(content) || JSON_UNDEFINED.equals(content);
	}

	public static boolean isJson(String content) {
		if (!StringUtils.hasText(content))
			return false;

		String first = String.valueOf(content.charAt(0));
		return JsonToken.START_OBJECT.asString().equals(first) || JsonToken.START_ARRAY.asString().equals(first);
	}

	public static boolean isArrayJson(String content) {
		if (!StringUtils.hasText(content))
			return false;

		String first = String.valueOf(content.charAt(0));
		return JsonToken.START_ARRAY.asString().equals(first);
	}

	public static boolean isObjectJson(String content) {
		if (!StringUtils.hasText(content))
			return false;

		String first = String.valueOf(content.charAt(0));
		return JsonToken.START_OBJECT.asString().equals(first);
	}
}
