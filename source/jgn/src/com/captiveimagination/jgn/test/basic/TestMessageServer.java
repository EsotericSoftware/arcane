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
package com.captiveimagination.jgn.test.basic;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.ping.Ping;

/**
 * @author Matthew D. Hicks
 */
public class TestMessageServer implements MessageListener, ConnectionListener {
	private int id;
	
	public TestMessageServer(int id) {
		this.id = id;
	}
	
	public static void main(String[] args) throws Exception {
		JGN.register(BasicMessage.class);
		
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
		
		System.out.println("Connection established!");
		System.out.println("Ping: " + Ping.pingAndWait(client, TimeUnit.MILLISECONDS));
	}

	public void messageCertified(Message message) {
		System.out.println("MessageCertified(" + id + "): " + message);
	}

	public void messageFailed(Message message) {
		System.out.println("MessageFailed(" + id + "): " + message);
	}

	public void messageReceived(Message message) {
		System.out.println("MessageReceived(" + id + "): " + message);
	}

	public void messageSent(Message message) {
		System.out.println("MessageSent(" + id + "): " + message);
	}

	public void connected(MessageClient client) {
		System.out.println("Connected(" + id + "): " + ((InetSocketAddress)client.getAddress()).getPort());
	}

	public void disconnected(MessageClient client) {
		System.out.println("Disconnected(" + id + "): " + ((InetSocketAddress)client.getAddress()).getPort());
	}

	public void negotiationComplete(MessageClient client) {
		System.out.println("Negotiated(" + id + "): " + ((InetSocketAddress)client.getAddress()).getPort());
	}
	
	public void kicked(MessageClient client, String reason) {
		System.out.println("Kicked(" + id + "):" + ((InetSocketAddress)client.getAddress()).getPort());
	}
}
