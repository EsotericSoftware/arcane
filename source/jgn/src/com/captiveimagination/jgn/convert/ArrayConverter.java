
package com.captiveimagination.jgn.convert;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;

/**
 * Serializes arrays.
 * <p>
 * An array requires a header of 3 bytes plus 4-10 bytes (depending on dimension length) for each dimension beyond the first. If
 * the type of array is not final (note primitives are final) then an extra 2 bytes is written for <b>each</b> element.
 * 
 * @see JGN#register(Class)
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ArrayConverter extends Converter {
	private int[] getDimensions (Object array) {
		int depth = 0;
		Class nextClass = array.getClass().getComponentType();
		while (nextClass != null) {
			depth++;
			nextClass = nextClass.getComponentType();
		}
		int[] dimensions = new int[depth];
		dimensions[0] = Array.getLength(array);
		if (depth > 1) collectDimensions(array, 1, dimensions);
		return dimensions;
	}

	private void collectDimensions (Object array, int dimension, int[] dimensions) {
		boolean elementsAreArrays = dimension < dimensions.length - 1;
		for (int i = 0, s = Array.getLength(array); i < s; i++) {
			Object element = Array.get(array, i);
			if (element == null) continue;
			dimensions[dimension] = Math.max(dimensions[dimension], Array.getLength(element));
			if (elementsAreArrays) collectDimensions(element, dimension + 1, dimensions);
		}
	}

	private Class getElementClass (Class arrayClass) {
		Class elementClass = arrayClass;
		while (elementClass.getComponentType() != null)
			elementClass = elementClass.getComponentType();
		return elementClass;
	}

	public void writeObjectData (MessageClient client, Object array, ByteBuffer buffer) throws ConversionException {
		// Write dimensions.
		int[] dimensions = getDimensions(array);
		buffer.put((byte)dimensions.length);
		for (int dimension : dimensions)
			BufferUtil.writeInt(buffer, dimension);
		// If element class is final (this includes primitives) then all elements are the same type.
		Converter elementConverter = null;
		Class elementClass = getElementClass(array.getClass());
		if (Modifier.isFinal(elementClass.getModifiers())) elementConverter = Converter.getConverter(elementClass);
		// Write array data.
		writeArray(client, elementConverter, buffer, array, 0, dimensions.length);
	}

	private void writeArray (MessageClient client, Converter elementConverter, ByteBuffer buffer, Object array, int dimension,
		int dimensionCount) throws ConversionException {
		int length = Array.getLength(array);
		if (dimension > 0) {
			// Write array length. With Java's "jagged arrays" this could be less than the dimension size.
			BufferUtil.writeInt(buffer, length);
		}
		// Write array data.
		boolean elementsAreArrays = dimension < dimensionCount - 1;
		for (int i = 0; i < length; i++) {
			Object element = Array.get(array, i);
			if (elementsAreArrays) {
				// Nested array.
				if (element != null) writeArray(client, elementConverter, buffer, element, dimension + 1, dimensionCount);
			} else if (elementConverter != null) {
				// Use same converter for all elements.
				elementConverter.writeObject(client, element, buffer);
			} else {
				// Each element could be a different type. Store the class with the object.
				Converter.writeClassAndObject(client, element, buffer);
			}
		}
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> c) throws ConversionException {
		// Get dimensions.
		byte dimensionCount = buffer.get();
		int[] dimensions = new int[dimensionCount];
		for (int i = 0; i < dimensionCount; i++)
			dimensions[i] = BufferUtil.readInt(buffer);
		// Get element converter if all elements are the same type.
		Converter elementConverter = null;
		Class elementClass = getElementClass(c);
		if (Modifier.isFinal(elementClass.getModifiers())) elementConverter = Converter.getConverter(elementClass);
		// Create array and read in the data.
		T array = (T)Array.newInstance(elementClass, dimensions);
		readArray(elementConverter, elementClass, buffer, array, 0, dimensions);
		return array;
	}

	private void readArray (Converter elementConverter, Class elementClass, ByteBuffer buffer, Object array, int dimension,
		int[] dimensions) throws ConversionException {
		boolean elementsAreArrays = dimension < dimensions.length - 1;
		int length;
		if (dimension == 0)
			length = dimensions[0];
		else
			length = BufferUtil.readInt(buffer);
		for (int i = 0; i < length; i++) {
			if (elementsAreArrays) {
				// Nested array.
				Object element = Array.get(array, i);
				if (element != null) readArray(elementConverter, elementClass, buffer, element, dimension + 1, dimensions);
			} else if (elementConverter != null) {
				// Use same converter (and class) for all elements.
				Array.set(array, i, elementConverter.readObject(buffer, elementClass));
			} else {
				// Each element could be a different type. Look up the class with the object.
				Array.set(array, i, Converter.readClassAndObject(buffer));
			}
		}
	}
}
