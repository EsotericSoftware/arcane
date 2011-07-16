
package com.captiveimagination.jgn.convert.type;

import java.nio.ByteBuffer;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.convert.FieldConverter;

/**
 * Allows implementing classes to do additional serialization after the normal serialization done by {@link FieldConverter}. This
 * is useful when an object has a state where parts of it don't need to be serialized. Note that FieldConverter will automatically
 * serialize any non-transient fields.
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public interface FieldExternalizable extends FieldSerializable {
	public void writeObjectData (MessageClient client, ByteBuffer buffer) throws ConversionException;

	public void readObjectData (ByteBuffer buffer) throws ConversionException;
}
