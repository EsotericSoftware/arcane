/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Jul 5, 2006
 */
package com.captiveimagination.jgn.test.unit.modular;

import java.io.*;
import java.nio.*;

import junit.framework.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.convert.Converter;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.queue.*;
import com.captiveimagination.jgn.test.basic.*;


/**
 * @author Matthew D. Hicks
 */
public class TestPacketCombiner extends TestCase{
	public void testSingleThreaded() {
		BasicMessage message = new BasicMessage();
		createTestThread(1, 4, 1, message);
	}
	
	private static void createTestThread(final int id, final int messageCount, final int totalMessages, final Message message) {
		Thread t = new Thread() {
			public void run() {
				try {
					JGN.register(message.getClass());
					MessageServer server = new TCPMessageServer(null);
					MessageClient client = new MessageClient(null, server);
					client.setStatus(MessageClient.Status.CONNECTED);
					client.register(JGN.getRegisteredClassId(message.getClass()), message.getClass());
					int count = 0;
					while (count < totalMessages) {
						for (int i = 0; i < messageCount; i++) {
							try {
								client.sendMessage(message);
								count++;
							} catch(QueueFullException exc) {
								i--;
							}
						}
						CombinedPacket packet = PacketCombiner.combine(client);
						ByteBuffer buffer = packet.getBuffer();
						
						buffer.position(0);
						int j = 0;
						while (j < messageCount) {
							if (!buffer.hasRemaining()) {
								packet = PacketCombiner.combine(client);
								buffer = packet.getBuffer();
							}
							Message m = (Message)Converter.readClassAndObject(buffer);
							if (m.getClass() == message.getClass()) {
								j++;
							}
						}
						System.out.println("Count(" + id + "): " + count);
						
						Thread.sleep(1);
					}
				} catch(InterruptedException exc) {
					exc.printStackTrace();
				} catch(MessageHandlingException exc) {
					exc.printStackTrace();
				} catch(IOException exc) {
					exc.printStackTrace();
				} catch (ConversionException exc) {
					exc.printStackTrace();
				}
			}
		};
		t.start();
	}
	
	public static void main(String[] args) {
		LocalRegistrationMessage message = new LocalRegistrationMessage();
		JGN.populateRegistrationMessage(message);
		
		createTestThread(1, 1, 1, message);
		//createTestThread(2, 5, 25, message);
		//createTestThread(3, 5, 5, message);
	}
}
