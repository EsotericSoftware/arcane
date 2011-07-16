
package com.captiveimagination.jgn.convert;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.convert.type.BeanSerializable;

/**
 * Serializes Java beans that implement the {@link BeanSerializable} interface using bean accessor methods. This is not quite as
 * fast as {@link FieldConverter} but much faster than Java serialization.
 * <p>
 * BeanConverter does not write header data, only the object data is stored. If the type of a bean property is not final (note
 * primitives are final) then an extra 2 bytes is written for that property.
 * 
 * @see Converter
 * @see JGN#register(Class)
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class BeanConverter extends Converter {
	private final Map<Class, CachedMethod[]> setterMethodCache = new HashMap();
	private final Map<Class, CachedMethod[]> getterMethodCache = new HashMap();

	/**
	 * Stores the getter and setter methods for each bean property in the specified class.
	 */
	public void register (Class c) throws ConversionException {
		BeanInfo info;
		try {
			info = Introspector.getBeanInfo(c);
		} catch (IntrospectionException ex) {
			throw new ConversionException("Error getting bean info.", ex);
		}
		// Methods are sorted by alpha so the order of the data is known and doesn't have to be sent across the wire.
		PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
		Arrays.sort(descriptors, new Comparator<PropertyDescriptor>() {
			public int compare (PropertyDescriptor o1, PropertyDescriptor o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		List<CachedMethod> getterMethods = new ArrayList(descriptors.length);
		List<CachedMethod> setterMethods = new ArrayList(descriptors.length);
		for (PropertyDescriptor property : descriptors) {
			if (property.getName().equals("class")) continue;
			Method getMethod = property.getReadMethod();
			Method setMethod = property.getWriteMethod();
			// Require both a getter and setter.
			if (getMethod == null || setMethod == null) continue;

			// Always use the same converter for this property if the properties's class is final (note: primitives are final).
			Converter converter = null;
			Class returnType = getMethod.getReturnType();
			if (Modifier.isFinal(returnType.getModifiers())) converter = Converter.getConverter(returnType);

			CachedMethod cachedGetMethod = new CachedMethod();
			cachedGetMethod.method = getMethod;
			cachedGetMethod.converter = converter;
			getterMethods.add(cachedGetMethod);

			CachedMethod cachedSetMethod = new CachedMethod();
			cachedSetMethod.method = setMethod;
			cachedSetMethod.converter = converter;
			cachedSetMethod.type = setMethod.getParameterTypes()[0];
			setterMethods.add(cachedSetMethod);
		}
		getterMethodCache.put(c, getterMethods.toArray(new CachedMethod[getterMethods.size()]));
		setterMethodCache.put(c, setterMethods.toArray(new CachedMethod[setterMethods.size()]));
	}

	private CachedMethod[] getGetterMethods (Class c) throws ConversionException {
		CachedMethod[] getterMethods = getterMethodCache.get(c);
		if (getterMethods != null) return getterMethods;
		register(c);
		return getterMethodCache.get(c);
	}

	private CachedMethod[] getSetterMethods (Class c) throws ConversionException {
		CachedMethod[] setterMethods = setterMethodCache.get(c);
		if (setterMethods != null) return setterMethods;
		register(c);
		return setterMethodCache.get(c);
	}

	public void writeObjectData (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException {
		Class c = object.getClass();
		Object[] noArgs = new Object[0];
		try {
			for (CachedMethod cachedMethod : getGetterMethods(c)) {
				Object value = cachedMethod.method.invoke(object, noArgs);
				Converter converter = cachedMethod.converter;
				if (converter != null)
					converter.writeObject(client, value, buffer);
				else
					Converter.writeClassAndObject(client, value, buffer);
			}
		} catch (IllegalAccessException ex) {
			throw new ConversionException("Error accessing getter method in class: " + c.getName(), ex);
		} catch (InvocationTargetException ex) {
			throw new ConversionException("Error invoking getter method in class: " + c.getName(), ex);
		}
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> c) throws ConversionException {
		T object = newInstance(c);
		try {
			for (CachedMethod cachedMethod : getSetterMethods(object.getClass())) {
				Object value;
				Converter converter = cachedMethod.converter;
				if (converter != null)
					value = converter.readObject(buffer, cachedMethod.type);
				else
					value = Converter.readClassAndObject(buffer);
				cachedMethod.method.invoke(object, new Object[] {value});
			}
		} catch (IllegalAccessException ex) {
			throw new ConversionException("Error accessing setter method in class: " + c.getName(), ex);
		} catch (InvocationTargetException ex) {
			throw new ConversionException("Error invoking setter method in class: " + c.getName(), ex);
		}
		return object;
	}

	static private class CachedMethod {
		public Method method;
		public Converter converter;
		public Class type; // Only populated for setter methods.
	}
}
