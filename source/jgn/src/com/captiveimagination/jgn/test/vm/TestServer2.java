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
 * Created: Jul 10, 2006
 */
package com.captiveimagination.jgn.test.vm;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.test.basic.*;

/**
 * @author Matthew D. Hicks
 */
public class TestServer2 {
	public static void main(String[] args) throws Exception {
		// We need to register the message we want to use,
		// This will let the message server know to tell other
		// connections about the message when we connect.
		JGN.register(BasicMessage.class);
		
		// We create our MessageServer. This can be a TCPMessageServer or UDPMessageServer
		// The InetAddress can be specifically bound, set to localhost, or null to accept all
		// ports. The port number if specified as 0 will be automatically assigned.
		MessageServer server = new TCPMessageServer(new InetSocketAddress((InetAddress)null, 0));
		// We re-use TestMessageServer which implements MessageListener and ConnectionListener
		TestMessageServer tms = new TestMessageServer(1);
		// We add it to this server as a message listener
		server.addMessageListener(tms);
		// We add it to this server as a connection listener
		server.addConnectionListener(tms);
		// If you have an update thread of your own this is not necessary as you can simply
		// call server.update(), but this is a convenience method for multithreading.
		JGN.createThread(server).start();
		
		// Since the purpose of this server is to connect to TestServer1, we need to establish a
		// connection client. This method will attempt to connection and negotiate before returning.
		// If there is a problem that causes it not to connect within the time alloted the attempt
		// will disconnect and return null.
		MessageClient client = server.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		// We need to let the user know that there was a problem connecting.
		if (client == null) throw new IOException("Connection not established!");
		
		System.out.println("Connection established!");
	}
}