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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.captiveimagination.jgn.message.*;

/**
 * BasicMessageQueue handles the basic function of enqueuing and
 * retrieving messages.
 *
 * ase: note, size and total are not really states of the datastructure, so
 *      they might be *temporarily* out of sync with the correct size of the
 *      queue.
 * /ase
 *
 * @author Matthew D. Hicks
 */
public class BasicMessageQueue implements MessageQueue {
	private Queue<Message> list;
	private volatile long total;
	private List<Message> clone;
	
	public BasicMessageQueue() {
		list = new ConcurrentLinkedQueue<Message>();
		clone = new ArrayList<Message>();
		total = 0;
	}
	
	public void add(Message message) {
		if (message == null) throw new NullPointerException("Message must not be null");
		
		list.add(message);

		total++;
	}

	public Message poll() {
		return list.poll();
	}

	public boolean isEmpty() {
		return list.size() == 0;
	}

	public long getTotal() {
		return total;
	}

	public int getSize() {
		return list.size();
	}
	
	public void remove(Message message) {
		list.remove(message);
	}
	
	public List<Message> clonedList() {
		clone.clear();
		for (Message m : list) {
			clone.add(m);
		}
		return clone;
	}
}
