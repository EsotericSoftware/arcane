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
 * Created: Jun 6, 2006
 */
package com.captiveimagination.jgn.queue;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 * @author Skip M. B. Balk
 */
public class MessagePriorityQueue implements MessageQueue {
	private BasicMessageQueue[] lists;
	private final int max;
	private volatile int size = 0;
	private volatile long total = 0;
	
	public MessagePriorityQueue() {
		this(1024);
	}

	public MessagePriorityQueue(int max) {
		this.max = max;
		lists = new BasicMessageQueue[5];
		for (int i = 0; i < lists.length; i++) {
			lists[i] = new BasicMessageQueue();
		}
	}

	public void add(Message message) {
		if (message == null) throw new NullPointerException("Message must not be null");
		
		PriorityMessage m = (PriorityMessage)message;
		// TODO should we check instanceof here, it's a public method
		// ..at the moment add will only be called from MultiMessageQueue, and then message IS a prioMess.
		int p = m.getPriority();

		if (p < PriorityMessage.PRIORITY_TRIVIAL || p > PriorityMessage.PRIORITY_CRITICAL)
			throw new IllegalStateException("Invalid priority: " + m.getPriority());

		if (size == max) throw new QueueFullException("Queue reached max size: " + max);

		synchronized (lists[p]) {
			lists[p].add(m);
		}

		size++;
		total++;
	}
	
	public Message poll() {
		if (isEmpty()) return null;

		for (int i = lists.length - 1; i >= 0; i--) {
			synchronized (lists[i]) {
				if (lists[i].isEmpty()) continue;

				Message m = lists[i].poll();
				size--;
				return m;
			}
		}

		return null;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public long getTotal() {
		return total;
	}

	public int getSize() {
		return size;
	}
}