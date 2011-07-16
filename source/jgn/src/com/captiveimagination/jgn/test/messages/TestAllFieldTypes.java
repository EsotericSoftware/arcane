/**
 * Copyright (c) 2005-2007 JavaGameNetworking
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
 * Created: Feb 2, 2006
 */
package com.captiveimagination.jgn.test.messages;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.event.DebugLogListener;
import com.captiveimagination.jgn.event.DynamicMessageAdapter;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.io.IOException;

/**
 * a simple testszenario that transmits a AllFieldTypesMessage from Server1 to Server2
 * and protocols the results...
 *
 * @author Alfons Seul
 */
public class TestAllFieldTypes {

	public static void main(String[] args) throws Exception {
		JGN.register(AllFieldTypesMessage.class);

		MessageServer server1 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		DebugLogListener tms1 = new DebugLogListener("Srv1> ");
		DynamicMessageAdapter dma1 = new DynamicMessageAdapter() {
			public void messageReceived(AllFieldTypesMessage m) {
				System.out.println("  ** message.byte = "+m.getFByte());
				System.out.println("  ** message.bool = "+m.getFBool());
				System.out.println("  ** message.int[2] = "+m.getFIntArr(2));
				System.out.println("  ** message.long = "+m.getFLong());
				System.out.println("  ** message.ser = "+m.getFSer());
				System.out.println("  ** message.enum = "+m.getFEnum());
				System.out.println("  ** message.Str = "+m.getFString());
			}
		};
		server1.addMessageListener(tms1);
		server1.addConnectionListener(tms1);
		server1.addMessageListener(dma1);
		JGN.createThread(server1).start();

		MessageServer server2 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		DebugLogListener tms2 = new DebugLogListener("Srv2> ");
		server2.addMessageListener(tms2);
		server2.addConnectionListener(tms2);
		JGN.createThread(server2).start();

		MessageClient client = server2.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		if (client == null) throw new IOException("Connection not established!");
		System.out.println("Connection established!");

		AllFieldTypesMessage mess = new AllFieldTypesMessage();
		client.sendMessage(mess);
		Thread.sleep(2000);

		client.disconnectAndWait(5000);
		server2.closeAndWait(5000);
		server1.closeAndWait(5000);
		Thread.sleep(200);
	}

}
