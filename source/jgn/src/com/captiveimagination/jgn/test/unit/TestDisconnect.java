package com.captiveimagination.jgn.test.unit;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

public class TestDisconnect extends AbstractMessageServerTestCase {
	public void testDisconnect1() throws Exception {
		Thread.sleep(1000);		// TODO this is necessary or messages stop getting certified too soon - fix this
		client1to2.disconnect();
		long time = System.currentTimeMillis();
		long timeout = 5000;
		while (System.currentTimeMillis() <= timeout + time) {
			if ((client1to2.getStatus() == MessageClient.Status.DISCONNECTED) && (client2to1.getStatus() == MessageClient.Status.DISCONNECTED)) {
				break;
			}
			Thread.sleep(1);
		}
		Thread.sleep(1000);
		System.out.println("Status: " + client1to2.getStatus() + ", " + client2to1.getStatus());
		assertTrue(client1to2.getStatus() == MessageClient.Status.DISCONNECTED);
		assertTrue(client2to1.getStatus() == MessageClient.Status.DISCONNECTED);
		assertTrue(client1Disconnected);
		assertTrue(client2Disconnected);
		System.out.println("Disconnection took: " + (System.currentTimeMillis() - time) + "ms");
	}
	
	public void testDisconnect2() throws Exception {
		client2to1.disconnect();
		long time = System.currentTimeMillis();
		long timeout = 5000;
		while (System.currentTimeMillis() <= timeout + time) {
			if ((client1to2.getStatus() == MessageClient.Status.DISCONNECTED) && (client2to1.getStatus() == MessageClient.Status.DISCONNECTED)) {
				break;
			}
			Thread.sleep(1);
		}
		Thread.sleep(1000);
		assertTrue(client1to2.getStatus() == MessageClient.Status.DISCONNECTED);
		assertTrue(client2to1.getStatus() == MessageClient.Status.DISCONNECTED);
		assertTrue(client1Disconnected);
		assertTrue(client2Disconnected);
		System.out.println("Disconnection took: " + (System.currentTimeMillis() - time) + "ms");
	}
	
	public void testTimeout1() throws Exception {
		DisconnectMessage message = new DisconnectMessage();
		client1to2.sendMessage(message);
		long time = System.currentTimeMillis();
		while (client1to2.isConnected()) {
			if (System.currentTimeMillis() > time + MessageServer.DEFAULT_TIMEOUT + 5000) break;
		}
		Thread.sleep(1000);
		System.out.println("Elapsed: " + (System.currentTimeMillis() - time) + "ms");
		assertTrue(client1to2.getStatus() == MessageClient.Status.DISCONNECTED);
		assertTrue(client2to1.getStatus() == MessageClient.Status.DISCONNECTED);
		assertTrue(client1Disconnected);
		assertTrue(client2Disconnected);
	}
	
	public void testTimeout2() throws Exception {
		DisconnectMessage message = new DisconnectMessage();
		client1to2.sendMessage(message);
		client1to2.sendMessage(new NoopMessage());
		client2to1.sendMessage(new NoopMessage());
		long time = System.currentTimeMillis();
		while (client1to2.isConnected()) {
			if (System.currentTimeMillis() > time + MessageServer.DEFAULT_TIMEOUT + 5000) break;
		}
		Thread.sleep(1000);
		System.out.println("Elapsed: " + (System.currentTimeMillis() - time) + "ms");
		assertTrue(client1to2.getStatus() == MessageClient.Status.DISCONNECTED);
		assertTrue(client2to1.getStatus() == MessageClient.Status.DISCONNECTED);
		assertTrue(client1Disconnected);
		assertTrue(client2Disconnected);
	}

	public void testMulticonnect() throws Exception {
		InetSocketAddress address3 = new InetSocketAddress(InetAddress.getLocalHost(), 3000);
		MessageServer server3;
		if (tcp) {
			server3 = new TCPMessageServer(address3);
		} else {
			server3 = new UDPMessageServer(address3);
		}
		server3.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				System.out.println("Server 3 Connected: " + ((InetSocketAddress)client.getAddress()).getPort());
			}

			public void negotiationComplete(MessageClient client) {
				System.out.println("Server 3 Negotiated: " + ((InetSocketAddress)client.getAddress()).getPort());
			}

			public void disconnected(MessageClient client) {
				System.out.println("Server 3 Disconnected: " + ((InetSocketAddress)client.getAddress()).getPort());
			}

			
			public void kicked(MessageClient client, String reason) {
			}
		});
		JGN.createThread(server3).start();
		
		MessageClient client3 = server3.connectAndWait(serverAddress1, 5000);
		if (client3 != null) {
			System.out.println("Client 3 established to server1");
		}
		
		MessageClient client4 = server3.connectAndWait(serverAddress2, 5000);
		if (client4 != null) {
			System.out.println("Client 4 established to server2");
		}
	}
	
	public void testDisconnectMulticonnect() throws Exception {
		client2to1.disconnect();
		
		// We need to wait for a second to make sure it finished closing
		Thread.sleep(2000);
		
		// Now lets try to establish a connection to the server1
		testMulticonnect();
	}

	public static void main(String[] args) throws Exception {
		TestDisconnect test = new TestDisconnect();
		test.setUp();
		test.testDisconnectMulticonnect();
		test.tearDown();
	}
}
