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
 * Created: Nov 13, 2006
 */
package com.captiveimagination.jgn.util;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.Updatable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives a MessageClient and monitors the status of that client.
 *
 * @author Matthew D. Hicks
 */
public class ClientMonitor implements Updatable {
	private String name;
	private MessageClient client;
	private long frequency;
	private boolean alive;
	private long lastUpdate;
	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.util.ClientMonitor");

	public ClientMonitor(String name, MessageClient client, long frequency) {
		this.name = name;
		this.client = client;
		this.frequency = frequency;
		alive = true;
	}

	public boolean isAlive() {
		return alive;
	}

	public void update() throws Exception {
		if (lastUpdate == 0) { // don't output nothing on first call
			lastUpdate = System.currentTimeMillis();
			return;
		}
		if (System.currentTimeMillis() - lastUpdate > frequency) {
			if (LOG.isLoggable(Level.INFO)) {
				LOG.log(Level.INFO, "{0}: received: {1}, sent: {2}, queues: {3}({4}), {5}({6}), {7}({8})",
						new Object[]{name, client.getReceivedCount(), client.getSentCount(),
								client.getIncomingMessageQueue().getTotal(), client.getIncomingMessageQueue().getSize(),
								client.getOutgoingMessageQueue().getTotal(), client.getOutgoingMessageQueue().getSize(),
								client.getOutgoingQueue().getTotal(), client.getOutgoingQueue().getSize()
						}
				);
			}
//      System.out.println(name + ": Received: " + client.getReceivedCount() +
//               ", Sent: " + client.getSentCount() +
//               ", " + client.getIncomingMessageQueue().getTotal() + "(" + client.getIncomingMessageQueue().getSize() + ")" +
//               ", " + client.getOutgoingMessageQueue().getTotal() + "(" + client.getOutgoingMessageQueue().getSize() + ")" +
//               ", " + client.getOutgoingQueue().getTotal() + "(" + client.getOutgoingQueue().getSize() + ")");
			lastUpdate = System.currentTimeMillis();
		}
	}

	public void close() {
		alive = false;
	}
}
