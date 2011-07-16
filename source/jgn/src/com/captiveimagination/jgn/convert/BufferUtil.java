
package com.captiveimagination.jgn.convert;

import java.nio.ByteBuffer;

/**
 * Byte manipulating utility methods.
 */
public class BufferUtil {
	static private final byte BYTE = 1;
	static private final byte SHORT = 2;
	static private final byte INT = 3;

	/**
	 * Writes the specified int to the buffer as a byte, short, or int depending on the size of the number.
	 */
	static public void writeInt (ByteBuffer buffer, int value) {
		if (value <= Byte.MAX_VALUE) {
			buffer.put(BYTE);
			buffer.put((byte)value);
		} else if (value <= Short.MAX_VALUE) {
			buffer.put(SHORT);
			buffer.putShort((short)value);
		} else {
			buffer.put(INT);
			buffer.putInt(value);
		}
	}

	/**
	 * Reads an int from the buffer that was written with {@link #writeInt(ByteBuffer, int)}.
	 */
	static public int readInt (ByteBuffer buffer) {
		byte flag = buffer.get();
		switch (flag) {
		case BYTE:
			return buffer.get();
		case SHORT:
			return buffer.getShort();
		case INT:
			return buffer.getInt();
		default:
			throw new RuntimeException("Invalid identifying byte flag: " + flag);
		}
	}
}
