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
 * Created: Jun 26, 2006
 */
package com.captiveimagination.jgn.test.unit;

import java.io.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class TestCertifiedMessage extends AbstractMessageServerTestCase {
	private static long certified1Id;
	private static boolean certified1;
	private static boolean failed1;
	
	public void setUp() throws IOException, InterruptedException {
		super.setUp();
		server1.addMessageListener(new MessageAdapter() {
			public void messageCertified(Message message) {
				System.out.println("S1> Message was certified: " + message.getId());
				if (message instanceof MyCertifiedMessage) {
					assertEquals(message.getId(), certified1Id);
					assertTrue(message.getId() > 0);
					certified1 = true;
				}
			}
			
			public void messageFailed(Message message) {
				System.out.println("S1> Message failed: " + message.getId() + ", " + message);
				failed1 = true;
			}

			public void messageReceived(Message message) {
				if (message instanceof MyCertifiedMessage) {
					System.out.println("S1> Message received: " + message.getId());
				}
			}

			public void messageSent(Message message) {
				if (message instanceof MyCertifiedMessage) {
					assertTrue(message.getId() > 0);
					certified1Id = message.getId();
					System.out.println("S1> Message sent: " + message.getId());
				}
			}
		});
		server2.addMessageListener(new MessageAdapter() {
			public void messageCertified(Message message) {
				System.out.println("S2> Message was certified: " + message.getId());
			}
			
			public void messageFailed(Message message) {
				System.out.println("S2> Message failed: " + message.getId());
			}

			public void messageReceived(Message message) {
				if (message instanceof MyCertifiedMessage) {
					System.out.println("S2> Message received: " + message.getId());
					assertTrue(message.getId() > 0);
					assertEquals(message.getId(), certified1Id);
				}
			}

			public void messageSent(Message message) {
				if (message instanceof MyCertifiedMessage) {
					System.out.println("S2> Message sent: " + message.getId());
				}
			}
		});
	}
	
	public void testCertification() throws Exception {
		client1to2.sendMessage(new MyCertifiedMessage());
		Thread.sleep(1000);
		assertTrue(certified1);
	}

	public void testFailure() throws Exception {
		client1to2.sendMessage(new DisconnectMessage());
		Thread.sleep(1000);
		client1to2.sendMessage(new MyCertifiedMessage());
		Thread.sleep(1000);
		assertTrue(failed1);
	}
}
