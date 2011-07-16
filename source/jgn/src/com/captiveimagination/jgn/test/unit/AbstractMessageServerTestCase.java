package com.captiveimagination.jgn.test.unit;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;

import junit.framework.*;

public class AbstractMessageServerTestCase extends TestCase {
	protected static boolean tcp = true;
	protected static boolean debug = false;
	
	protected InetSocketAddress serverAddress1;
	protected MessageServer server1;
	protected MessageClient client1to2;
	protected boolean client1Disconnected;
	protected InetSocketAddress serverAddress2;
	protected MessageServer server2;
	protected MessageClient client2to1;
	protected boolean client2Disconnected;
	
	protected void setUp() throws IOException, InterruptedException {
		JGN.register(MyCertifiedMessage.class);
		JGN.register(MyRealtimeMessage.class);
		JGN.register(MyUniqueMessage.class);
		JGN.register(MySerializableMessage.class);
		
		// Create first MessageServer
		serverAddress1 = new InetSocketAddress(InetAddress.getLocalHost(), 1000);
		if (tcp) {
			server1 = new TCPMessageServer(serverAddress1);
		} else {
			server1 = new UDPMessageServer(serverAddress1);
		}
		if (debug) {
			server1.addMessageListener(new DebugListener("Server1"));
			server1.addConnectionListener(new DebugListener("Server1"));
		}
		server1.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				client1Disconnected = false;
			}

			public void negotiationComplete(MessageClient client) {
				client1to2 = client;
			}

			public void disconnected(MessageClient client) {
				System.out.println("Disconnected1");
				client1Disconnected = true;
			}

			
			public void kicked(MessageClient client, String reason) {
			}
		});
		JGN.createThread(server1).start();
		
		// Create second MessageServer
		serverAddress2 = new InetSocketAddress(InetAddress.getLocalHost(), 2000);
		if (tcp) {
			server2 = new TCPMessageServer(serverAddress2);
		} else {
			server2 = new UDPMessageServer(serverAddress2);
		}
		if (debug) {
			server2.addMessageListener(new DebugListener("Server2"));
			server2.addConnectionListener(new DebugListener("Server1"));
		}
		server2.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				client2Disconnected = false;
			}

			public void negotiationComplete(MessageClient client) {
				client2to1 = client;
			}

			public void disconnected(MessageClient client) {
				System.out.println("Disconnected2");
				client2Disconnected = true;
			}

			
			public void kicked(MessageClient client, String reason) {
			}
		});
		JGN.createThread(server2).start();
		
		// Connect server2 to server1
		MessageClient client = server2.connectAndWait(serverAddress1, 5000);
		if (client == null) {
			System.err.println("Unable to establish connection!");
		} else {
			System.out.println("Connection established successfully");
		}
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + 5000) {
			if ((client1to2 != null) && (client2to1 != null)) break;
			Thread.sleep(1);
		}
		assertTrue(client1to2 != null);
		assertTrue(client2to1 != null);
	}

	protected void tearDown() throws IOException, InterruptedException {
		server1.closeAndWait(5000);
		server2.closeAndWait(5000);
	}
}
