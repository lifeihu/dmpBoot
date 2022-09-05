package org.springframework.jackson;

import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializerProvider;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.deser.BeanDeserializer;
import org.codehaus.jackson.map.deser.BeanDeserializerFactory;
import org.codehaus.jackson.type.JavaType;

@SuppressWarnings("deprecation")
public class ExtendBeanDeserializerFactory extends BeanDeserializerFactory {

	@Override
	public JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, DeserializerProvider p, JavaType type, BeanProperty property) throws JsonMappingException {
		JsonDeserializer<Object> deserializer = super.createBeanDeserializer(config, p, type, property);

		if (deserializer instanceof BeanDeserializer) {
			return new ExtendBeanDeserializer((BeanDeserializer) deserializer);
		}

		return deserializer;
	}

}
