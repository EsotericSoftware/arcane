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
 * Created: Oct 1, 2006
 */
package com.captiveimagination.jgn.test.so.basic;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.so.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class BasicSharedObjectClient {
	public static void main(String[] args) throws Exception {
		// Create the server
		MessageServer server = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		//server.addMessageListener(new DebugListener("Client"));
		// Create a single thread managing updates for the server and the SharedObjectManager
		JGN.createThread(server, SharedObjectManager.getInstance()).start();
		
		// Add a listener to see changes
		SharedObjectManager.getInstance().addListener(new SharedObjectListener() {
			public void changed(String name, Object object, String field, MessageClient client) {
				System.out.println("Changed: " + name + ", " + object + ", " + field + ", " + client);
			}

			public void created(String name, Object object, MessageClient client) {
				System.out.println("Created: " + name + ", " + object + ", " + client);
			}

			public void removed(String name, Object object, MessageClient client) {
				System.out.println("Removed: " + name + ", " + object + ", " + client);
			}
		});
		
		// Enable sharing on the server so it can comprehend events
		SharedObjectManager.getInstance().enable(server);
		
		// Connect to the server
		MessageClient client = server.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		if (client != null) {
			System.out.println("Connected!");
			Thread.sleep(5000);
			MySharedBean bean = (MySharedBean)SharedObjectManager.getInstance().getObject("MyBean");
			System.out.println("Remote Bean Value: " + bean.getOne());
			bean.setTwo(53);
		} else {
			System.out.println("Unable to connect!");
		}
	}
}
