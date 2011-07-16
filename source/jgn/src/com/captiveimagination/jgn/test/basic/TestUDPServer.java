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
 * Created: Jul 6, 2006
 */
package com.captiveimagination.jgn.test.basic;

import java.net.*;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 */
public class TestUDPServer {
	public static void main(String[] args) throws Exception {
		JGN.register(BasicMessage.class);
		
		MessageServer server1 = new UDPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		TestMessageServer listener1 = new TestMessageServer(1);
		server1.addConnectionListener(listener1);
		server1.addMessageListener(listener1);
		JGN.createThread(server1).start();
		
		MessageServer server2 = new UDPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		TestMessageServer listener2 = new TestMessageServer(2);
		server2.addConnectionListener(listener2);
		server2.addMessageListener(listener2);
		JGN.createThread(server2).start();
		
		MessageClient client = server2.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		System.out.println("Client: " + client);
	}
}
