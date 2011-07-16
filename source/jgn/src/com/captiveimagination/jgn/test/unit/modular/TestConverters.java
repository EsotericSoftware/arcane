
package com.captiveimagination.jgn.test.unit.modular;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.convert.ArrayConverter;
import com.captiveimagination.jgn.convert.BeanConverter;
import com.captiveimagination.jgn.convert.BooleanConverter;
import com.captiveimagination.jgn.convert.ByteConverter;
import com.captiveimagination.jgn.convert.CharConverter;
import com.captiveimagination.jgn.convert.CollectionConverter;
import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.convert.Converter;
import com.captiveimagination.jgn.convert.DoubleConverter;
import com.captiveimagination.jgn.convert.EnumConverter;
import com.captiveimagination.jgn.convert.FieldConverter;
import com.captiveimagination.jgn.convert.FloatConverter;
import com.captiveimagination.jgn.convert.IntConverter;
import com.captiveimagination.jgn.convert.LongConverter;
import com.captiveimagination.jgn.convert.MapConverter;
import com.captiveimagination.jgn.convert.SerializableConverter;
import com.captiveimagination.jgn.convert.ShortConverter;
import com.captiveimagination.jgn.convert.StringConverter;
import com.captiveimagination.jgn.convert.type.FieldSerializable;
import com.captiveimagination.jgn.message.LocalRegistrationMessage;
import com.captiveimagination.jgn.test.basic.BasicMessage;
import com.captiveimagination.jgn.test.unit.AbstractMessageServerTestCase;

/**
 * @author Nathan Sweet <misc@n4te.com>
 */
public class TestConverters extends AbstractMessageServerTestCase {
	private ByteBuffer buffer = ByteBuffer.allocateDirect(512 * 1024);
	private Converter arrayConverter = new ArrayConverter();

	protected void setUp () throws IOException, InterruptedException {
		JGN.register(Hide1.class);
		// Purposefully don't regsiter Hide2 to test sending an unregistered object.
		super.setUp();
	}

	public void testConverters () throws ConversionException {
		// Uses a single test method because we need a MessageClient for all tests.
		booleans();
		bytes();
		chars();
		shorts();
		integers();
		floats();
		doubles();
		strings();
		nulls();
		primitiveArrays();
		finalObjectArrays();
		objectArrays();
		enums();
		collections();
		maps();
		fieldConverter();
		messages();
		messageSize();
		beanConverter();
		serializableConverter();
	}

	public void integers () throws ConversionException {
		assertEquals(123, roundTrip(123, new IntConverter(true)));
		assertEquals(new Integer(123), roundTrip(new Integer(123), new IntConverter(false)));
	}

	public void booleans () throws ConversionException {
		assertEquals(true, roundTrip(true, new BooleanConverter(true)));
		assertEquals(Boolean.TRUE, roundTrip(Boolean.TRUE, new BooleanConverter(false)));
		assertEquals(false, roundTrip(false, new BooleanConverter(true)));
		assertEquals(Boolean.FALSE, roundTrip(Boolean.FALSE, new BooleanConverter(false)));
	}

	public void shorts () throws ConversionException {
		assertEquals((short)123, roundTrip((short)123, new ShortConverter(true)));
		assertEquals(new Short((short)123), roundTrip(new Short((short)123), new ShortConverter(false)));
	}

	public void longs () throws ConversionException {
		assertEquals(123l, roundTrip(123l, new LongConverter(true)));
		assertEquals(new Long(123l), roundTrip(new Long(123l), new LongConverter(false)));
	}

	public void doubles () throws ConversionException {
		assertEquals(123d, roundTrip(123d, new DoubleConverter(true)));
		assertEquals(new Double(123d), roundTrip(new Double(123d), new DoubleConverter(false)));
	}

	public void floats () throws ConversionException {
		assertEquals(123f, roundTrip(123f, new FloatConverter(true)));
		assertEquals(new Float(123f), roundTrip(new Float(123f), new FloatConverter(false)));
	}

	public void bytes () throws ConversionException {
		assertEquals((byte)123, roundTrip((byte)123, new ByteConverter(true)));
		assertEquals(new Byte((byte)123f), roundTrip(new Byte((byte)123), new ByteConverter(false)));
	}

	public void chars () throws ConversionException {
		assertEquals('x', roundTrip('x', new CharConverter(true)));
		assertEquals(new Character('x'), roundTrip(new Character('x'), new CharConverter(false)));
	}

	public void strings () throws ConversionException {
		assertEquals("abc123", roundTrip("abc123", new StringConverter()));
		assertEquals("", roundTrip("", new StringConverter()));
	}

	public void nulls () throws ConversionException {
		assertEquals(null, roundTrip(null, new StringConverter()));
		assertEquals(null, roundTrip(null, new FieldConverter()));
		assertEquals(null, roundTrip(null, new BeanConverter()));
		assertEquals(null, roundTrip(null, new SerializableConverter()));
		assertEquals(null, roundTrip(null, new CollectionConverter()));
	}

	public void fieldConverter () throws ConversionException {
		assertEquals(1, ((Hide)roundTrip(new Hide1(), new FieldConverter())).get());
		assertEquals(2, ((Hide)roundTrip(new Hide2(), new FieldConverter())).get());
		Hide1 h1 = new Hide1();
		h1.setA(999);
		h1.setB("weee");
		h1.setC(new Hide2());
		Hide1 h2 = (Hide1)roundTrip(h1, new FieldConverter());
		assertEquals(999, h2.getA());
		assertEquals("weee", h2.b);
		assertEquals(2, h2.c.get());
	}

	public void beanConverter () throws ConversionException {
		assertEquals(1, ((Hide)roundTrip(new Hide1(), new BeanConverter())).get());
		assertEquals(2, ((Hide)roundTrip(new Hide2(), new BeanConverter())).get());
		Hide1 h1 = new Hide1();
		h1.setA(999);
		h1.setB("weee");
		h1.setC(new Hide2());
		Hide1 h2 = (Hide1)roundTrip(h1, new BeanConverter());
		assertEquals(999, h2.getA());
		assertEquals("weee", h2.b);
		assertEquals(2, h2.c.get());
	}

	public void collections () throws ConversionException {
		assertEquals(new ArrayList(), roundTrip(new ArrayList(), new CollectionConverter()));
		ArrayList list = new ArrayList();
		list.add("JGN");
		list.add("freaking");
		list.add("rocks");
		list.add("!!!");
		assertEquals(list, roundTrip(list, new CollectionConverter()));
		list.add(123);
		assertEquals(list, roundTrip(list, new CollectionConverter()));
		Set set = new HashSet();
		set.add("JGN");
		set.add("freaking");
		set.add("rocks");
		assertEquals(set, roundTrip(set, new CollectionConverter()));
		set.add(123);
		assertEquals(set, roundTrip(set, new CollectionConverter()));
	}

	public void maps () throws ConversionException {
		Map map = new HashMap();
		map.put("a", "JGN");
		map.put("b", "freaking");
		map.put("c", "rocks");
		map.put("d", "!!!");
		assertEquals(map, roundTrip(map, new MapConverter()));
		map.put("d", 123);
		assertEquals(map, roundTrip(map, new MapConverter()));
		map.put(123, 123);
		assertEquals(map, roundTrip(map, new MapConverter()));
		map = new Hashtable();
		map.put("a", "JGN");
		map.put("b", "freaking");
		map.put("c", "rocks");
		map.put("d", "!!!");
		assertEquals(map, roundTrip(map, new MapConverter()));
		map.put("d", 123);
		assertEquals(map, roundTrip(map, new MapConverter()));
		map.put(123, 123);
		assertEquals(map, roundTrip(map, new MapConverter()));
	}

	public void messages () throws ConversionException {
		LocalRegistrationMessage message1 = new LocalRegistrationMessage();
		message1.setIds(new short[] {1, 2, 3});
		message1.setRegisteredClasses(new String[] {"a1", "b2", "c3"});
		LocalRegistrationMessage message2 = (LocalRegistrationMessage)roundTrip(message1, new FieldConverter());
		assertEquals(message1.getId(), message2.getId());
		assertEquals(message1.getIds()[1], message2.getIds()[1]);
		assertEquals(message1.getRegisteredClasses()[2], message2.getRegisteredClasses()[2]);
	}

	public void primitiveArrays () throws ConversionException {
		int[][][] intData = { {{123}}, { {456, 56}, {-1, 888}}, {{}}};
		int[][][] intResult = (int[][][])roundTrip(intData, arrayConverter);
		assertEquals(123, intResult[0][0][0]);
		assertEquals(456, intResult[1][0][0]);
		assertEquals(56, intResult[1][0][1]);
		assertEquals(-1, intResult[1][1][0]);
		assertEquals(888, intResult[1][1][1]);
		assertEquals(0, intResult[2][0][0]);
	}

	public void finalObjectArrays () throws ConversionException {
		Integer[][][] integerData = { {{123}}, { {456, 56}, {null, 888}}, {{}}};
		Integer[][][] integerResult = (Integer[][][])roundTrip(integerData, arrayConverter);
		assertEquals(new Integer(123), integerResult[0][0][0]);
		assertEquals(new Integer(456), integerResult[1][0][0]);
		assertEquals(new Integer(56), integerResult[1][0][1]);
		assertNull(integerResult[1][1][0]);
		assertEquals(new Integer(888), integerResult[1][1][1]);
		assertNull(integerResult[2][0][0]);
	}

	public void objectArrays () throws ConversionException {
		Hide[][][] hideData = { {{new Hide1()}}, { {new Hide2(), new Hide1()}, {null, new Hide2()}}, {{}}};
		Hide[][][] hideResult = (Hide[][][])roundTrip(hideData, arrayConverter);
		assertEquals(1, hideResult[0][0][0].get());
		assertEquals(2, hideResult[1][0][0].get());
		assertEquals(1, hideResult[1][0][1].get());
		assertNull(hideResult[1][1][0]);
		assertEquals(2, hideResult[1][1][1].get());
		assertNull(hideResult[2][0][0]);
	}

	public void enums () throws ConversionException {
		Enum e1 = Enum.two;
		Enum e2 = (Enum)roundTrip(e1, new EnumConverter());
		assertSame(e1, e2);
	}

	public void serializableConverter () throws ConversionException {
		Map map = new HashMap();
		map.put("a", "JGN");
		map.put("b", "freaking");
		map.put("c", "rocks");
		map.put("d", "!!!");
		assertEquals(map, roundTrip(map, new SerializableConverter()));
		Hide2 h = new Hide2();
		h.calendar = new GregorianCalendar();
		assertEquals(h.calendar, ((Hide2)roundTrip(h, new SerializableConverter())).calendar);
	}

	public void messageSize () throws ConversionException {
		BasicMessage message = new BasicMessage();
		message.setValue(321);

		ByteBuffer buffer = ByteBuffer.allocateDirect(512 * 1024);

		FieldConverter fieldConverter = new FieldConverter();
		fieldConverter.writeObjectData(client1to2, message, buffer);
		int converterSize = buffer.position();
		buffer.flip();
		BasicMessage newMessage = (BasicMessage)fieldConverter.readObjectData(buffer, BasicMessage.class);
		assertEquals(message.getValue(), newMessage.getValue());

		buffer.clear();

		SerializableConverter serializableConverter = new SerializableConverter();
		serializableConverter.writeObjectData(client1to2, message, buffer);
		int serializableConverterSize = buffer.position();
		buffer.flip();
		newMessage = (BasicMessage)serializableConverter.readObjectData(buffer, null);
		assertEquals(message.getValue(), newMessage.getValue());

		buffer.clear();

		System.out.println("FieldConverter size in bytes: " + converterSize);
		System.out.println("SerializableConverter size in bytes: " + serializableConverterSize);
		System.out.println("SerializableConverter is " + (serializableConverterSize / (float)converterSize)
			+ " times bigger than FieldConverter.");

		System.out.println();

		int count = 20000;

		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			fieldConverter.writeObjectData(client1to2, message, buffer);
			buffer.flip();
			fieldConverter.readObjectData(buffer, BasicMessage.class);
			buffer.clear();
		}
		long end = System.currentTimeMillis();
		long totalConverter = end - start;
		System.out.println("FieldConverter serialized/deserialized " + count + " messages in: " + totalConverter);

		start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			serializableConverter.writeObjectData(client1to2, message, buffer);
			buffer.flip();
			serializableConverter.readObjectData(buffer, null);
			buffer.clear();
		}
		end = System.currentTimeMillis();
		long totalSerializableConverter = end - start;
		System.out
			.println("SerializableConverter serialized/deserialized " + count + " messages in: " + totalSerializableConverter);

		float times = totalSerializableConverter / (float)totalConverter;
		System.out.println("FieldConverter is " + times + " times faster than SerializableConverter.");
	}

	static public interface Hide extends FieldSerializable {
		public int get ();
	}

	static public class Hide1 implements Hide {
		private int a;
		String b;
		public Hide c;

		public int get () {
			return 1;
		}

		public int getA () {
			return a;
		}

		public void setA (int a) {
			this.a = a;
		}

		public String getB () {
			return b;
		}

		public void setB (String b) {
			this.b = b;
		}

		public Hide getC () {
			return c;
		}

		public void setC (Hide c) {
			this.c = c;
		}
	}

	static public class Hide2 implements Hide, Serializable {
		private Calendar calendar;

		public int get () {
			return 2;
		}
	}

	static public enum Enum {
		one, two, three
	}

	private Object roundTrip (Object input, Converter converter) throws ConversionException {
		converter.writeObject(client1to2, input, buffer);
		buffer.flip();
		Class c = null;
		if (input != null) c = input.getClass();
		Object output = converter.readObject(buffer, c);
		buffer.clear();
		return output;
	}
}
