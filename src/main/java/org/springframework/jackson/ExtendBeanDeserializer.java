package org.springframework.jackson;

import java.io.IOException;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.BeanDeserializer;
import org.codehaus.jackson.map.deser.SettableBeanProperty;
import org.springframework.util.StringUtils;

public class ExtendBeanDeserializer extends BeanDeserializer {

	public ExtendBeanDeserializer(BeanDeserializer src) {
		super(src, true);
	}

	@Override
	public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		if (_nonStandardCreation) {
			if (_unwrappedPropertyHandler != null) {
				return deserializeWithUnwrapped(jp, ctxt);
			}
			if (_externalTypeIdHandler != null) {
				return deserializeWithExternalTypeId(jp, ctxt);
			}
			return deserializeFromObjectUsingNonDefault(jp, ctxt);
		}

		final Object bean = _valueInstantiator.createUsingDefault();
		if (_injectables != null) {
			injectValues(ctxt, bean);
		}
		for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
			String propName = jp.getCurrentName();
			// Skip field name:
			jp.nextToken();

			if (propName.indexOf(".") > -1) {
				String value = jp.getText();

				String[] props = propName.split("\\.");

				try {
					String parent = "";
					for (int i = 0; i < props.length; i++) {
						String prop = parent == "" ? props[i] : parent + "." + props[i];

						if (i < props.length - 1) {
							Object o = PropertyUtils.getProperty(bean, prop);
							if (o == null) {
								o = PropertyUtils.getPropertyType(bean, prop).newInstance();
								PropertyUtils.setProperty(bean, prop, o);
							}

						} else {
							Class<?> type = PropertyUtils.getPropertyType(bean, prop);
							if ((!StringUtils.hasText(value) || "null".equals(value)) && !type.isPrimitive())
								PropertyUtils.setProperty(bean, prop, null);
							else
								PropertyUtils.setProperty(bean, prop, ConvertUtils.convert(value, type));
						}

						parent = prop;
					}

				} catch (NoSuchMethodException e) {
					continue;

				} catch (Exception e) {
					wrapAndThrow(e, bean, propName, ctxt);
				}

				continue;
			}

			SettableBeanProperty prop = _beanProperties.find(propName);
			if (prop != null) { // normal case
				try {
					prop.deserializeAndSet(jp, ctxt, bean);
				} catch (Exception e) {
					wrapAndThrow(e, bean, propName, ctxt);
				}
				continue;
			}
			_handleUnknown(jp, ctxt, bean, propName);
		}
		return bean;
	}

	private final void _handleUnknown(JsonParser jp, DeserializationContext ctxt, Object bean, String propName) throws IOException, JsonProcessingException {
		/* As per [JACKSON-313], things marked as ignorable should not be
		 * passed to any setter
		 */
		if (_ignorableProps != null && _ignorableProps.contains(propName)) {
			jp.skipChildren();
		} else if (_anySetter != null) {
			try {
				_anySetter.deserializeAndSet(jp, ctxt, bean, propName);
			} catch (Exception e) {
				wrapAndThrow(e, bean, propName, ctxt);
			}
		} else {
			// Unknown: let's call handler method
			handleUnknownProperty(jp, ctxt, bean, propName);
		}
	}

}
