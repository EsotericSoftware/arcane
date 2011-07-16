
package com.captiveimagination.jgn.convert;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.convert.type.BeanSerializable;
import com.captiveimagination.jgn.convert.type.FieldSerializable;
import com.captiveimagination.jgn.message.Message;

/**
 * Serializes objects to bytes and deserializes objects from bytes. Serialization is most efficient with objects that have types
 * that have been registered (see {@link JGN#register(Class)}).
 * <p>
 * By default converters are registered for all primitives (including wrappers) as well as the following classes:
 * <p>
 * {@link String}<br>
 * {@link FieldSerializable}<br>
 * {@link BeanSerializable}<br>
 * {@link Enum}<br>
 * {@link List}<br>
 * {@link Map}<br>
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
abstract public class Converter {
	static private final Logger log = Logger.getLogger("com.captiveimagination.jgn.convert");

	static private final Map<Class, Converter> typeToConverter = new HashMap<Class, Converter>();
	static private final Map<Class, Converter> classToConverter = new HashMap<Class, Converter>();
	static private final Converter fieldConverter = new FieldConverter();
	static private final Converter arrayConverter = new ArrayConverter();
	static private final Converter stringConverter = new StringConverter();
	static private final Converter serializableConverter = new SerializableConverter();

	static {
		// Primitives.
		typeToConverter.put(boolean.class, new BooleanConverter(true));
		typeToConverter.put(byte.class, new ByteConverter(true));
		typeToConverter.put(char.class, new CharConverter(true));
		typeToConverter.put(short.class, new ShortConverter(true));
		typeToConverter.put(int.class, new IntConverter(true));
		typeToConverter.put(long.class, new LongConverter(true));
		typeToConverter.put(float.class, new FloatConverter(true));
		typeToConverter.put(double.class, new DoubleConverter(true));
		// Primitive wrappers.
		typeToConverter.put(Boolean.class, new BooleanConverter(false));
		typeToConverter.put(Byte.class, new ByteConverter(false));
		typeToConverter.put(Character.class, new CharConverter(false));
		typeToConverter.put(Short.class, new ShortConverter(false));
		typeToConverter.put(Integer.class, new IntConverter(false));
		typeToConverter.put(Long.class, new LongConverter(false));
		typeToConverter.put(Float.class, new FloatConverter(false));
		typeToConverter.put(Double.class, new DoubleConverter(false));
		// Other.
		typeToConverter.put(String.class, stringConverter);
		// Converters that match using Class#isAssignableFrom(Class).
		classToConverter.put(FieldSerializable.class, fieldConverter);
		classToConverter.put(BeanSerializable.class, new BeanConverter());
		classToConverter.put(Enum.class, new EnumConverter());
		classToConverter.put(Collection.class, new CollectionConverter());
		classToConverter.put(Map.class, new MapConverter());
	}

	/**
	 * Register a class and a converter to handle objects of that class. Note this is registering a converter and is different than
	 * registering a class for serializing (see {@link JGN#register(Class)}).
	 * @param exactMatch If true, the converter will only be used for objects that are exactly of the specified type. If false, the
	 *           converter will be used for any object that extends or implements the specified type.
	 */
	static public void registerConverter (Class type, Converter converter, boolean exactMatch) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (converter == null) throw new IllegalArgumentException("converter cannot be null.");
		if (exactMatch)
			typeToConverter.put(type, converter);
		else
			classToConverter.put(type, converter);
		log.finest("Registered converter \"" + converter.getClass().getSimpleName() + "\" for type: " + type);
	}

	/**
	 * Returns the converter for the specified type.
	 * @throws ConversionException if no converter is registered to handle the specified type.
	 */
	static public Converter getConverter (Class type) throws ConversionException {
		// Shortcut for messages since they always use FieldConverter and this method is called often.
		if (Message.class.isAssignableFrom(type)) return fieldConverter;

		Converter converter = typeToConverter.get(type);
		if (converter != null) return converter;

		if (type.isArray()) return arrayConverter;

		for (Entry<Class, Converter> entry : classToConverter.entrySet())
			if (entry.getKey().isAssignableFrom(type)) return entry.getValue();

		// Last ditch effort.
		if (Serializable.class.isAssignableFrom(type)) {
			log.finer("Warning: Using Java serialization for class (see FieldConverter): " + type.getName());
			return serializableConverter;
		}

		throw new ConversionException("No converter is registered to handle class: " + type.getName());
	}

	/**
	 * Writes the specified class ID (or name if the class is not registered) to the buffer.
	 * @param c Can be null (writes the class ID {@link JGN#ID_NULL_OBJECT}).
	 * @throws ConversionException if the class could not be written.
	 */
	static public void writeClass (MessageClient client, Class c, ByteBuffer buffer) throws ConversionException {
		Short classID = null;
		if (client != null) classID = client.getRegisteredClassId(c);
		if (classID == null) {
			buffer.putShort(JGN.ID_CLASS_STRING);
			try {
				stringConverter.writeObjectData(client, c.getName(), buffer);
			} catch (ConversionException ex) {
				throw new ConversionException("Error serializing class name: " + c.getName(), ex);
			}
			log.fine("Warning: Serializing unregistered class (see JGN#register(Class)): " + c.getName());
		} else
			buffer.putShort(classID);
	}

	/**
	 * Writes the object class ID (or name if the class is not registered) and then the object to the buffer. This method should be
	 * used when the class will not be known at the time the object is to be read from the buffer. Otherwise
	 * {@link #writeObject(MessageClient, Object, ByteBuffer)} should be used.
	 * @param client The client for the server that the object will be sent to. This is used to write the server's class ID to the
	 *           buffer. If null or the server does not have an ID registered for the class, then the class name String will be
	 *           written to the buffer. Note that this takes more bandwidth and should be avoided by registering classes that will
	 *           be sent (see {@link JGN#register(Class)}).
	 * @param object Can be null (writes the class ID {@link JGN#ID_NULL_OBJECT} and no object data).
	 * @throws ConversionException if the object could not be written.
	 */
	static public void writeClassAndObject (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException {
		if (object == null) {
			buffer.putShort(JGN.ID_NULL_OBJECT);
			return;
		}
		Class c = object.getClass();
		writeClass(client, c, buffer);
		try {
			getConverter(c).writeObjectData(client, object, buffer);
		} catch (ConversionException ex) {
			throw new ConversionException("Error serializing instance of class: " + object.getClass().getName(), ex);
		}
	}

	/**
	 * Reads the class ID or name from the buffer and returns it as a class object.
	 * @return The class, or null if the class read from the buffer represented a null object.
	 * @throws ConversionException if the class could not be read.
	 */
	static public Class readClass (ByteBuffer buffer) throws ConversionException {
		Class c;
		short classID = buffer.getShort();
		switch (classID) {
		case JGN.ID_NULL_OBJECT:
			return null;
		case JGN.ID_CLASS_STRING:
			String className;
			try {
				className = (String)stringConverter.readObjectData(buffer, null);
			} catch (ConversionException ex) {
				throw new ConversionException("Error deserializing class name.", ex);
			}
			try {
				c = Class.forName(className);
			} catch (ClassNotFoundException ex) {
				throw new ConversionException("Unable to find class: " + className, ex);
			}
			break;
		default:
			c = JGN.getRegisteredClass(classID);
			if (c == null) throw new ConversionException("Encountered unregistered class ID: " + classID);
		}
		return c;
	}

	/**
	 * Reads the object class ID or name from the buffer and uses the converter for that class to read the object.
	 * @return The deserialized object, or null if the object read from the buffer should be null.
	 * @throws ConversionException if the object could not be read.
	 */
	static public Object readClassAndObject (ByteBuffer buffer) throws ConversionException {
		Class c = readClass(buffer);
		if (c == null) return null;
		try {
			return getConverter(c).readObjectData(buffer, c);
		} catch (ConversionException ex) {
			throw new ConversionException("Error deserializing instance of class: " + c.getName(), ex);
		}
	}

	boolean isPrimitive;

	/**
	 * Writes the object to the buffer. If the object is not a primitive, first a byte is written to denote if the object is null.
	 * This method should be used when the class will be known at the time the object is to be read from the buffer. Otherwise
	 * {@link #writeClassAndObject(MessageClient, Object, ByteBuffer)} should be used.
	 * @param client See {@link #writeClassAndObject(MessageClient, Object, ByteBuffer)}.
	 * @throws ConversionException if the object could not be written.
	 */
	public final void writeObject (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException {
		if (!isPrimitive) {
			// Write 0 if null.
			if (object == null) {
				buffer.put((byte)0);
				return;
			}
			buffer.put((byte)1);
		}
		try {
			writeObjectData(client, object, buffer);
		} catch (ConversionException ex) {
			throw new ConversionException("Error serializing instance of class: " + object.getClass().getName(), ex);
		}
	}

	/**
	 * Writes the data for the object to the buffer. This should be used only when the object to be written cannot be null.
	 * Otherwise use {@link #writeObject(MessageClient, Object, ByteBuffer)}.
	 * @param client See {@link #writeClassAndObject(MessageClient, Object, ByteBuffer)}.
	 * @param object Guaranteed to be non-null.
	 */
	public abstract void writeObjectData (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException;

	/**
	 * Reads an object from the buffer. If the object is not a primitive, first a byte is read to denote if the object is null.
	 * @param type The type of the object to be read.
	 */
	public final <T> T readObject (ByteBuffer buffer, Class<T> type) throws ConversionException {
		if (!isPrimitive && buffer.get() == 0) return null;
		try {
			return readObjectData(buffer, type);
		} catch (ConversionException ex) {
			throw new ConversionException("Error deserializing instance of class: " + type.getName(), ex);
		}
	}

	/**
	 * Reads the data for the object from the buffer. This should be used only when the object to be read cannot be null. Otherwise
	 * use {@link #readObject(ByteBuffer, Class)}.
	 * @param buffer Guaranteed to contain data for a non-null object.
	 * @param type The type of the object to be read.
	 */
	abstract public <T> T readObjectData (ByteBuffer buffer, Class<T> type) throws ConversionException;

	/**
	 * Returns an instance of the specified class.
	 * @throws ConversionException if the class could not be constructed.
	 */
	protected <T> T newInstance (Class<T> c) throws ConversionException {
		try {
			return c.newInstance();
		} catch (Exception ex) {
			throw new ConversionException("Error constructing instance of class (check constructors): " + c.getName(), ex);
		}
	}

	/**
	 * This method is invoked when a type is registered (see {@link JGN#register(Class)}). This allows the converter to do any
	 * necessary caching up front.
	 * @throws ConversionException if an error occured during registration.
	 */
	public void register (Class type) throws ConversionException {
	}
}
