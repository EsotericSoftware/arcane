
package com.captiveimagination.jgn.convert;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;

import com.captiveimagination.jgn.MessageClient;

/**
 * Serializes objects that implement the {@link Collection} interface.
 * <p>
 * A list requires a 2-5 byte header (depending on list size). If each element in the collection is of the same class then the
 * header is an additional 2 bytes. Otherwise an extra 2 bytes is written for <b>each</b> element in the collection.
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class CollectionConverter extends Converter {
	public void writeObjectData (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException {
		Collection collection = (Collection)object;
		int length = collection.size();
		BufferUtil.writeInt(buffer, length);
		if (length == 0) return;
		// Determine if elements are the same type.
		Iterator iter = collection.iterator();
		Class elementClass = iter.next().getClass();
		while (iter.hasNext()) {
			if (iter.next().getClass() != elementClass) {
				elementClass = null;
				break;
			}
		}
		if (elementClass != null) {
			buffer.put((byte)1);
			Converter.writeClass(client, elementClass, buffer);
			Converter converter = Converter.getConverter(elementClass);
			for (Object element : collection)
				converter.writeObject(client, element, buffer);
			// NOTE - If we knew the list had no nulls, we could use writeObjectData instead of writeObject to avoid the extra byte
			// per element for a null object. This could be applied to ArrayConverter, MapConverter, etc.
		} else {
			buffer.put((byte)0);
			for (Object element : collection)
				Converter.writeClassAndObject(client, element, buffer);
		}
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> c) throws ConversionException {
		int length = BufferUtil.readInt(buffer);
		Collection collection = (Collection)newInstance(c);
		if (length == 0) return (T)collection;
		if (buffer.get() == 1) {
			Class elementClass = Converter.readClass(buffer);
			Converter converter = Converter.getConverter(elementClass);
			for (int i = 0; i < length; i++)
				collection.add(converter.readObject(buffer, elementClass));
		} else {
			for (int i = 0; i < length; i++)
				collection.add(Converter.readClassAndObject(buffer));
		}
		return (T)collection;
	}
	
}
