
package com.captiveimagination.jgn.convert;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.convert.type.FieldExternalizable;
import com.captiveimagination.jgn.convert.type.FieldSerializable;
import com.captiveimagination.jgn.message.Message;

/**
 * Serializes objects that implement the {@link FieldSerializable} interface using direct field assignment. When the classes are
 * registered (see {@link JGN#register(Class)}), this is the most efficient mechanism for serializing objects. FieldConverter is
 * many times smaller and faster than Java serialization.
 * <p>
 * FieldConverter does not write header data, only the object data is stored. If the type of a field is not final (note primitives
 * are final) then an extra 2 bytes is written for that field.
 * 
 * @see Converter
 * @see JGN#register(Class)
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class FieldConverter extends Converter {
	private final Map<Class, CachedField[]> fieldCache = new HashMap();

	public void register (Class c) throws ConversionException {
		List<Field> allFields = new ArrayList();
		Class nextClass = c;
		while (nextClass != Object.class && nextClass != Message.class) {
			Collections.addAll(allFields, nextClass.getDeclaredFields());
			nextClass = nextClass.getSuperclass();
		}
		List<CachedField> cachedFields = new ArrayList(allFields.size());
		for (Field field : allFields) {
			int modifiers = field.getModifiers();
			if (Modifier.isTransient(modifiers)) continue;
			if (Modifier.isFinal(modifiers)) continue;
			if (Modifier.isStatic(modifiers)) continue;
			if (field.isSynthetic()) continue;
			field.setAccessible(true);

			CachedField cachedField = new CachedField();
			cachedField.field = field;

			// Always use the same converter for this field if the field's class is final (note: primitives are final).
			Class fieldClass = field.getType();
			if (Modifier.isFinal(fieldClass.getModifiers())) cachedField.converter = Converter.getConverter(fieldClass);

			cachedFields.add(cachedField);
		}
		// Fields are sorted by alpha so the order of the data is known and doesn't have to be sent across the wire.
		Collections.sort(cachedFields, new Comparator<CachedField>() {
			public int compare (CachedField o1, CachedField o2) {
				return o1.field.getName().compareTo(o2.field.getName());
			}
		});
		fieldCache.put(c, cachedFields.toArray(new CachedField[cachedFields.size()]));
	}

	private CachedField[] getFields (Class c) throws ConversionException {
		CachedField[] cachedFields = fieldCache.get(c);
		if (cachedFields != null) return cachedFields;
		register(c);
		return fieldCache.get(c);
	}

	public void writeObjectData (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException {
		try {
			for (CachedField cachedField : getFields(object.getClass())) {
				Object value = cachedField.field.get(object);
				Converter converter = cachedField.converter;
				if (converter != null)
					converter.writeObject(client, value, buffer);
				else
					Converter.writeClassAndObject(client, value, buffer);
			}
		} catch (IllegalAccessException ex) {
			throw new ConversionException("Error accessing field in class: " + object.getClass().getName(), ex);
		}
		if (object instanceof FieldExternalizable) ((FieldExternalizable)object).writeObjectData(client, buffer);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> c) throws ConversionException {
		T object = newInstance(c);
		try {
			for (CachedField cachedField : getFields(object.getClass())) {
				Object value;
				Field field = cachedField.field;
				Converter converter = cachedField.converter;
				if (converter != null)
					value = converter.readObject(buffer, field.getType());
				else
					value = Converter.readClassAndObject(buffer);
				field.set(object, value);
			}
		} catch (IllegalAccessException ex) {
			throw new ConversionException("Error accessing field in class: " + c.getName(), ex);
		}
		if (object instanceof FieldExternalizable) ((FieldExternalizable)object).readObjectData(buffer);
		return object;
	}

	static private class CachedField {
		public Field field;
		public Converter converter;
	}
}
