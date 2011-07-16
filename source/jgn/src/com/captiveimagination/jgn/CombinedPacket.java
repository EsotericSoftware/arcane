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
 * Created: Jun 22, 2006
 */
package com.captiveimagination.jgn;

import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.queue.QueueFullException;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles the combination of packets, the messages associated with them,
 * and the location that distinguishes the end of the message.
 *
 * @author Matthew CTR Hicks
 */
public class CombinedPacket {
	private MessageClient client;
	private ByteBuffer buffer;
	private List<Message> messages;
	private List<Integer> ends;

	public CombinedPacket(MessageClient client) {
		this.client = client;
		messages = new LinkedList<Message>();
		ends = new LinkedList<Integer>();
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public void add(Message message, int end) {
		messages.add(message);
		ends.add(end);
	}

	private Message getMessage() {
		if (messages.size() > 0) {
			return messages.get(0);
		}
		return null;
	}

	private int getEnd() {
		return ends.get(0);
	}

	private void remove() {
		messages.remove(0);
		ends.remove(0);
	}

	private boolean hasMore() {
		return messages.size() > 0;
	}

	public void process() {
		int position = getBuffer().position();
		while (hasMore()) {
			if (getEnd() > position) {
				break;
			}
			try {
			client.getOutgoingMessageQueue().add(getMessage());
			remove();
			} catch (QueueFullException e) {
				// try again next time
				break;
			}
		}
	}

	public int size() {
		return messages.size();
	}
}