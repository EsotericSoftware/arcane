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
 * Created: Oct 3, 2006
 */
package com.captiveimagination.jgn.test.so.swing;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.so.*;

public class TestSharedObjectServer {
	public static void main(String[] args) throws Exception {
		MessageServer server = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		JGN.createThread(server, SharedObjectManager.getInstance()).start();
		
		Person person = SharedObjectManager.getInstance().createSharedBean("Person", Person.class);
		person.setName("ServerName");
		person.setAddress("ServerAddress");
		person.setPhone("ServerPhone");
		SharedObjectManager.getInstance().addShare(person, server);
		SharedObjectManager.getInstance().enable(server);
	}
}
