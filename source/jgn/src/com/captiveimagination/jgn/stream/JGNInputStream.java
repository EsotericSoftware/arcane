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
 * Created: Jun 19, 2006
 */
package com.captiveimagination.jgn.stream;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.StreamMessage;
import com.captiveimagination.jgn.queue.MessagePriorityQueue;
import com.captiveimagination.jgn.queue.MessageQueue;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Matthew D. Hicks
 */
public class JGNInputStream extends InputStream implements MessageListener {
	private MessageClient client;
	private short streamId;
	private MessageQueue queue;
	private boolean streamClosed;

	private StreamMessage current;
	private int position;

	public JGNInputStream(MessageClient client, short streamId) {
		this.client = client;
		this.streamId = streamId;
		queue = new MessagePriorityQueue(-1);
		client.addMessageListener(this);
	}

	public MessageClient getMessageClient() {
		return client;
	}

	public short getStreamId() {
		return streamId;
	}

	public int read() throws IOException {
		while (!streamClosed) {
			if ((current == null) && (!queue.isEmpty())) {
				current = (StreamMessage) queue.poll();
				position = 0;
			}

			if (current != null) {
				if (current.getDataLength() == -1) {
					streamClosed = true;
					break;
				}

				int read = current.getData()[position++];
				if (position == current.getDataLength()) {
					current = null;
				}
				return read & 0xff;
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException exc) {
				// ignore !! exc.printStackTrace();
			}
		}
		return -1;
	}

	public void messageReceived(Message message) {
		if ((message instanceof StreamMessage) && (((StreamMessage) message).getStreamId() == streamId)) {
			queue.add(message);
		}
	}

	public void messageSent(Message message) {
	}

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}

	public void close() throws IOException {
		streamClosed = true;
		client.removeMessageListener(this);
		client.closeInputStream(streamId);
	}

	public boolean isClosed() {
		return streamClosed;
	}
}
