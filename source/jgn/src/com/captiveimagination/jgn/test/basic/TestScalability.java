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
 * Created: May 11, 2008
 */
package com.captiveimagination.jgn.test.basic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.Updatable;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;

/**
 * Tests the scalability of JGN with multiple servers.
 * 
 * @author Matt Hicks
 */
public class TestScalability {
	protected static final int TOTAL = 1000;
	protected static final int DELAY = 200;
	protected static final int BASE_PORT = 9000;
	
	public TestScalability() throws Exception {
		JGN.register(BasicMessage.class);
		
		Thread.sleep(10000);
		
		for (int i = 0; i < TOTAL; i++) {
			createServer(i);
			
			if (i % 10 == 0) {
				System.out.println("Created: " + i);
			}
			
			Thread.sleep(DELAY);
		}
		
		MessageServer server1 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		TestMessageServer tms1 = new TestMessageServer(1);
		server1.addMessageListener(tms1);
		server1.addConnectionListener(tms1);
		JGN.createThread(server1).start();
		
		MessageServer server2 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		TestMessageServer tms2 = new TestMessageServer(2);
		server2.addMessageListener(tms2);
		server2.addConnectionListener(tms2);
		JGN.createThread(server2).start();
		
		MessageClient client = server2.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		if (client == null) throw new IOException("Connection not established!");
	}
	
	private void createServer(int n) throws UnknownHostException, IOException {
		new ScalabilityInstance(BASE_PORT + n);
	}
	
	public static void main(String[] args) throws Exception {
		new TestScalability();
	}
}

class ScalabilityInstance implements MessageListener, Updatable {
	private int port;
	private MessageServer server;
	private AtomicLong received;
	
	private MessageClient client;
	
	public ScalabilityInstance(int port) throws UnknownHostException, IOException {
		this.port = port;
		
		received = new AtomicLong(-1);
		
		server = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), port));
		server.addMessageListener(this);
		JGN.createThread(server, this).start();
		
		if (port != TestScalability.BASE_PORT) {
			// Connect to previous server
			client = server.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), port - 1), 10000);
			if (client == null) {
				System.err.println("Failed to connect: " + (port - 1));
			} else {
				client.sendMessage(new BasicMessage());
			}
		}
	}

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}

	public void messageReceived(Message message) {
		if (message instanceof BasicMessage) {
			received.set(System.currentTimeMillis());
		}
	}

	public void messageSent(Message message) {
	}

	public boolean isAlive() {
		return true;
	}

	public void update() throws Exception {
		if ((received.get() != -1) && (System.currentTimeMillis() > received.get() + TestScalability.DELAY) && (client != null)) {
			client.sendMessage(new BasicMessage());
		}
	}
}