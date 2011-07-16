/**
 * Copyright (c) 2005-2006 JavaGameNetworking All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met: * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution. * Neither the name of
 * 'JavaGameNetworking' nor the names of its contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Created: Jun 3, 2006
 */

package com.captiveimagination.jgn.convert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.captiveimagination.jgn.MessageClient;

/**
 * Serializes objects using Java's built in serialization mechanism. Note that this is very inefficient and should be avoided if
 * possible.
 * 
 * @see Converter
 * @see FieldConverter
 * @see BeanConverter
 * 
 * @author Matthew D. Hicks
 */
public class SerializableConverter extends Converter {
	static private final Logger log = Logger.getLogger("com.captiveimagination.jgn.convert");

	public Object readObjectData (ByteBuffer buffer, Class c) throws ConversionException {
		int length = buffer.getInt();
		byte[] array = new byte[length];
		for (int i = 0; i < length; i++) {
			array[i] = buffer.get();
		}
		try {
			ByteArrayInputStream byteStream = new ByteArrayInputStream(array);
			ObjectInputStream objectStream = new ObjectInputStream(byteStream);
			return objectStream.readObject();
		} catch (Exception exc) {
			throw new ConversionException("Error during Java deserialization.", exc);
		}
	}

	public void writeObjectData (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
			objectStream.writeObject(object);
			byte[] array = byteStream.toByteArray();
			buffer.putInt(array.length);
			for (byte b : array) {
				buffer.put(b);
			}
		} catch(BufferOverflowException exc) {
			throw exc;
		} catch (Exception exc) {
			throw new ConversionException("Error during Java serialization.", exc);
		}
	}
}
