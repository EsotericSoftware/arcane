
package com.captiveimagination.jgn.convert;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.captiveimagination.jgn.MessageClient;

/**
 * Serializes objects that implement the {@link Map} interface.
 * <p>
 * A map requires a 6 byte header. If each key in the map is of the same class then the header is an additional 2 bytes. If each
 * value in the map is of the same class then the header is an additional 2 bytes. If the keys are of various types then an extra
 * 2 bytes is written for <b>each</b> key. If the values are of various types then an extra 2 bytes is written for <b>each</b>
 * value.
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class MapConverter extends Converter {
	public void writeObjectData (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException {
		Map<Object, Object> map = (Map)object;
		int length = map.size();
		BufferUtil.writeInt(buffer, length);
		if (length == 0) return;
		Set<Entry<Object, Object>> entries = map.entrySet();
		// Determine if keys and/or values are the same type.
		Iterator<Entry<Object, Object>> iter = entries.iterator();
		Entry<Object, Object> entry = iter.next();
		Class keyClass = entry.getKey().getClass();
		Class valueClass = entry.getValue().getClass();
		while (iter.hasNext()) {
			entry = iter.next();
			if (keyClass != null && entry.getKey().getClass() != keyClass) {
				keyClass = null;
				if (valueClass == null) break;
			}
			if (valueClass != null && entry.getValue().getClass() != valueClass) {
				valueClass = null;
				if (keyClass == null) break;
			}
		}
		// Write types and get converters.
		Converter keyConverter = null;
		if (keyClass != null) {
			buffer.put((byte)1);
			Converter.writeClass(client, keyClass, buffer);
			keyConverter = Converter.getConverter(keyClass);
		} else
			buffer.put((byte)0);
		Converter valueConverter = null;
		if (valueClass != null) {
			buffer.put((byte)1);
			Converter.writeClass(client, valueClass, buffer);
			valueConverter = Converter.getConverter(valueClass);
		} else
			buffer.put((byte)0);
		// Write data.
		for (iter = entries.iterator(); iter.hasNext();) {
			entry = iter.next();
			if (keyConverter != null)
				keyConverter.writeObject(client, entry.getKey(), buffer);
			else
				Converter.writeClassAndObject(client, entry.getKey(), buffer);
			if (valueConverter != null)
				valueConverter.writeObject(client, entry.getValue(), buffer);
			else
				Converter.writeClassAndObject(client, entry.getValue(), buffer);
		}
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> c) throws ConversionException {
		Map map = (Map)newInstance(c);
		int length = BufferUtil.readInt(buffer);
		if (length == 0) return (T)map;
		// Read element types and get converters.
		Converter keyConverter = null;
		Class keyClass = null;
		if (buffer.get() == 1) {
			keyClass = Converter.readClass(buffer);
			keyConverter = Converter.getConverter(keyClass);
		}
		Converter valueConverter = null;
		Class valueClass = null;
		if (buffer.get() == 1) {
			valueClass = Converter.readClass(buffer);
			valueConverter = Converter.getConverter(valueClass);
		}
		// Read data.
		for (int i = 0; i < length; i++) {
			Object key;
			if (keyConverter != null)
				key = keyConverter.readObject(buffer, keyClass);
			else
				key = Converter.readClassAndObject(buffer);
			Object value;
			if (valueConverter != null)
				value = valueConverter.readObject(buffer, valueClass);
			else
				value = Converter.readClassAndObject(buffer);
			map.put(key, value);
		}
		return (T)map;
	}
}
