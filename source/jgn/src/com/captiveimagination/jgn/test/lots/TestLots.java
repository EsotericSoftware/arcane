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
 * Created: Feb 24, 2007
 */
package com.captiveimagination.jgn.test.lots;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class TestLots {
	public static void main(String[] args) throws Exception {
		JGN.register(MyMessage01.class);
		JGN.register(MyMessage02.class);
		JGN.register(MyMessage03.class);
		JGN.register(MyMessage04.class);
		JGN.register(MyMessage05.class);
		JGN.register(MyMessage06.class);
		JGN.register(MyMessage07.class);
		JGN.register(MyMessage08.class);
		JGN.register(MyMessage09.class);
		JGN.register(MyMessage10.class);
		JGN.register(MyMessage11.class);
		JGN.register(MyMessage12.class);
		JGN.register(MyMessage13.class);
		JGN.register(MyMessage14.class);
		JGN.register(MyMessage15.class);
		JGN.register(MyMessage16.class);
		JGN.register(MyMessage17.class);
		JGN.register(MyMessage18.class);
		JGN.register(MyMessage19.class);
		JGN.register(MyMessage20.class);
		JGN.register(MyMessage21.class);
		JGN.register(MyMessage22.class);
		JGN.register(MyMessage23.class);
		JGN.register(MyMessage24.class);
		JGN.register(MyMessage25.class);
		JGN.register(MyMessage26.class);
		JGN.register(MyMessage27.class);
		JGN.register(MyMessage29.class);
		JGN.register(MyMessage30.class);
		JGN.register(MyMessage31.class);
		JGN.register(MyMessage32.class);
		JGN.register(MyMessage33.class);
		JGN.register(MyMessage34.class);
		JGN.register(MyMessage35.class);
		JGN.register(MyMessage36.class);
		JGN.register(MyMessage37.class);
		JGN.register(MyMessage38.class);
		JGN.register(MyMessage39.class);
		JGN.register(MyMessage40.class);
		JGN.register(MyMessage41.class);
		JGN.register(MyMessage42.class);
		JGN.register(MyMessage43.class);
		JGN.register(MyMessage44.class);
		JGN.register(MyMessage45.class);
		JGN.register(MyMessage46.class);
		JGN.register(MyMessage47.class);
		JGN.register(MyMessage48.class);
		JGN.register(MyMessage49.class);
		JGN.register(MyMessage50.class);
		JGN.register(MyMessage51.class);
		JGN.register(MyMessage52.class);
		JGN.register(MyMessage53.class);
		JGN.register(MyMessage54.class);
		JGN.register(MyMessage55.class);
		JGN.register(MyMessage56.class);
		JGN.register(MyMessage57.class);
		JGN.register(MyMessage58.class);
		JGN.register(MyMessage59.class);
		JGN.register(MyMessage60.class);
		JGN.register(MyMessage61.class);
		JGN.register(MyMessage62.class);
		JGN.register(MyMessage63.class);
		JGN.register(MyMessage64.class);
		JGN.register(MyMessage65.class);
		JGN.register(MyMessage66.class);
		JGN.register(MyMessage67.class);
		JGN.register(MyMessage68.class);
		JGN.register(MyMessage69.class);
		JGN.register(MyMessage70.class);
		JGN.register(MyMessage71.class);
		JGN.register(MyMessage72.class);
		JGN.register(MyMessage73.class);
		JGN.register(MyMessage74.class);
		JGN.register(MyMessage75.class);
		JGN.register(MyMessage76.class);
		JGN.register(MyMessage77.class);
		JGN.register(MyMessage78.class);
		JGN.register(MyMessage79.class);
		JGN.register(MyMessage80.class);
		
//		testMessageServer();
		testClientServer();
	}
	
	private static void testMessageServer() throws IOException {
		final MessageServer server1 = new TCPMessageServer(new InetSocketAddress(10000));
		final MessageServer server2 = new TCPMessageServer(new InetSocketAddress(11000));
		
		MessageListener listener = new MessageListener() {
			public void messageCertified(Message message) {
			}

			public void messageFailed(Message message) {
			}

			public void messageReceived(Message message) {
				if (message.getClass().getSimpleName().startsWith("My")) {
					System.out.println("Recieved: " + message.getClass().getSimpleName());
					if (message instanceof MyMessage80) {
						System.out.println("Recieved Last Message, shutting down!");
						try {
							server1.close();
							server2.close();
						} catch(Exception exc) {
							exc.printStackTrace();
						}
					}
				}
			}

			public void messageSent(Message message) {
			}
		};
		server1.addMessageListener(listener);
		server2.addMessageListener(listener);
		
		JGN.createThread(server1, server2).start();
		
		MessageClient client = server2.connectAndWait(new InetSocketAddress(10000), 15000);
		if (client != null) {
			System.out.println("Connected successfully!");
			client.sendMessage(new MyMessage01());
			client.sendMessage(new MyMessage10());
			client.sendMessage(new MyMessage20());
			client.sendMessage(new MyMessage30());
			client.sendMessage(new MyMessage40());
			client.sendMessage(new MyMessage50());
			client.sendMessage(new MyMessage60());
			client.sendMessage(new MyMessage70());
			client.sendMessage(new MyMessage80());
		} else {
			System.err.println("Connection failed!");
		}
	}

	private static void testClientServer() throws Exception {
		final JGNServer server = new JGNServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000), null);
		final JGNClient client = new JGNClient();
		
		MessageListener listener = new MessageListener() {
			public void messageCertified(Message message) {
			}

			public void messageFailed(Message message) {
			}

			public void messageReceived(Message message) {
				if (message.getClass().getSimpleName().startsWith("My")) {
					System.out.println("Recieved: " + message.getClass().getSimpleName());
					if (message instanceof MyMessage80) {
						System.out.println("Recieved Last Message, shutting down!");
						try {
							server.close();
							client.close();
						} catch(Exception exc) {
							exc.printStackTrace();
						}
					}
				}
			}

			public void messageSent(Message message) {
			}
		};
		server.addMessageListener(listener);
		client.addMessageListener(listener);
		
		JGN.createThread(server, client).start();
		
		client.connectAndWait(new InetSocketAddress(1000), null, 15000);
		System.out.println("Connected successfully!");
		client.sendToServer(new MyMessage01());
		client.sendToServer(new MyMessage10());
		client.sendToServer(new MyMessage20());
		client.sendToServer(new MyMessage30());
		client.sendToServer(new MyMessage40());
		client.sendToServer(new MyMessage50());
		client.sendToServer(new MyMessage60());
		client.sendToServer(new MyMessage70());
		client.sendToServer(new MyMessage80());
	}
}
