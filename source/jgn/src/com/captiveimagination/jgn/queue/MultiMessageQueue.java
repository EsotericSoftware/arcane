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
 * Created: Jun 24, 2006
 */
package com.captiveimagination.jgn.queue;

import com.captiveimagination.jgn.message.*;

/**
 * @author Administrator
 */
public class MultiMessageQueue implements MessageQueue {
	private BasicMessageQueue basicQueue;
	private RealtimeMessageQueue realtimeQueue;
	private MessagePriorityQueue priorityQueue;
	private OrderedMessageQueue orderedQueue;
	private final int max;
	private volatile int size = 0;
	private volatile int total = 0;
	
	public MultiMessageQueue(int max) {
		this.max = max;
		basicQueue = new BasicMessageQueue();
		realtimeQueue = new RealtimeMessageQueue();
		priorityQueue = new MessagePriorityQueue(-1);
		orderedQueue = new OrderedMessageQueue();
	}
	
	public void add(Message message) {
		if (message == null) throw new NullPointerException("Message must not be null");
		
		if (size == max) throw new QueueFullException("Queue reached max size: " + max);
		
		boolean increment = true;
		
		if (message instanceof RealtimeMessage) {
			increment = !realtimeQueue.addRemoves(message);
		} else if (message instanceof PriorityMessage) {
			priorityQueue.add(message);
		} else if (message instanceof OrderedMessage) {
			orderedQueue.add(message);
		} else {
			basicQueue.add(message);
		}
		
		if (increment) size++;
		total++;
	}

	public Message poll() {
		if (isEmpty()) return null;
		
		Message m = realtimeQueue.poll();
		if (m == null) {
			m = priorityQueue.poll();
		}
		if (m == null) {
			m = orderedQueue.poll();
		}
		if (m == null) {
			m = basicQueue.poll();
		}
		if (size > 0) size--;
		
		return m;
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
