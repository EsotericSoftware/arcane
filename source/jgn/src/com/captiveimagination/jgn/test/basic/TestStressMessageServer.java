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
 * Created: Jun 10, 2006
 */
package com.captiveimagination.jgn.test.basic;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.event.ConnectionListener;
import com.captiveimagination.jgn.event.MessageAdapter;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.queue.QueueFullException;
import com.captiveimagination.jgn.util.ClientMonitor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Matthew D. Hicks
 */
public class TestStressMessageServer {
	private static final int MAX = 1000000; //2000000000;
	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.test.basic.TestStressMessageSever");

	public static int receiveCount = 0;
	public static MessageClient client1;
	public static MessageClient client2;

	public static void main(String[] args) throws Exception {
		JGN.register(BasicMessage.class);
		final MessageServer server1 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		server1.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
//				System.out.println("S1> Connected: " + client);
				LOG.log(Level.FINE, "S1> Connected: {0}", client);
				client1 = client;
				ClientMonitor m1 = new ClientMonitor("C1>", client1, 1000);
				JGN.createThread(m1).start();
			}

			public void negotiationComplete(MessageClient client) {
				LOG.log(Level.FINE, "S1> Negotiation completed with: {0}", client);
//				System.out.println("S1> Negotiation completed successfully with: " + client);
			}

			public void disconnected(MessageClient client) {
				LOG.log(Level.FINE, "S1> Disconnected: {0}", client);
//				System.out.println("S1> Disconnected: " + client);
			}


			public void kicked(MessageClient client, String reason) {
			}
		});
		server1.addMessageListener(new MessageAdapter() {
			private long time;

			public void messageReceived(Message message) {
				if (message instanceof BasicMessage) {
					if (receiveCount == 0) time = System.currentTimeMillis();
					receiveCount++;
					//System.out.println("Count: " + receiveCount + ", " + ((BasicMessage)message).getValue());
					//if (receiveCount > 2000) System.out.println("Receive Count: " + receiveCount);
					if (receiveCount >= MAX) {
						LOG.log(Level.INFO, "S1> Completed in: {0} ms", (System.currentTimeMillis() - time));
//						System.out.println("Completed in: " + (System.currentTimeMillis() - time) + "ms");
						System.exit(0);
					}
				}
			}

			public void messageSent(Message message) {
				LOG.log(Level.FINE, "S1> Message sent: {0}", message);
//				System.out.println("S1> Message Sent: " + message);
			}

		});

		final MessageServer server2 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));

		JGN.createThread(server1, server2).start();

		server2.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				LOG.log(Level.FINE, "S2> Connected: {0}", client);
//        System.out.println("S2> Connected: " + client);
				client2 = client;
				ClientMonitor m2 = new ClientMonitor("C2>", client2, 1000);
				JGN.createThread(m2).start();
			}

			public void negotiationComplete(MessageClient client) {
				LOG.log(Level.FINE, "S2> Negotiation completed with: {0}", client);
//				System.out.println("S2> Negotiation completed successfully with: " + client);
			}

			public void disconnected(MessageClient client) {
				LOG.log(Level.FINE, "S2> Disconnected: {0}", client);
//				System.out.println("S2> Disconnected: " + client);
			}


			public void kicked(MessageClient client, String reason) {
			}
		});
		MessageClient client = server2.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		if (client != null) {
			LOG.log(Level.INFO, "Connection S2 --> S1 established! using client: {0}", client);
//			System.out.println("Connection established!");
			BasicMessage message = new BasicMessage();
			long time = System.currentTimeMillis();
			for (int i = 0; i < MAX; i++) {
				message.setValue(i);
				try {
					client.sendMessage(message);
				} catch (QueueFullException exc) {
					i--;
					try {
						Thread.sleep(1);
					} catch (InterruptedException ie) {//aha
					}
				}
			}
			LOG.log(Level.INFO, "S2> Enqueued in: {0} ms", (System.currentTimeMillis() - time));
//			System.out.println("Enqueued in: " + (System.currentTimeMillis() - time) + "ms");
		} else {
			LOG.warning("Connection timed out!");
//      System.out.println("Connection timed out!");
		}
	}
}
