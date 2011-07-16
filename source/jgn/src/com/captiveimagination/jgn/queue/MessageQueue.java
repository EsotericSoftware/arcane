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
 * MessageQueue implementations can be 
 * 
 * @author Matthew D. Hicks
 */
public interface MessageQueue {
	/**
	 * Adds a Message object to this queue.
	 * 
	 * @param message
	 */
	public void add(Message message);

	/**
	 * <code>poll</code> gets the highest priority
	 * Message from the queue and removes it before
	 * returning.
	 * 
	 * @return
	 * 		Highest priority Message FIFO'd to come out
	 * 		or <code>null</code> if the queue is empty
	 */
	public Message poll();

	/**
	 * <code>isEmpty</code> is a very fast check to
	 * see the state of the queue.
	 * 
	 * @return
	 * 		boolean representation of empty state of
	 * 		this MessageQueue.
	 */
	public boolean isEmpty();
	
	/**
	 * The total count of Messages that have been added
	 * to this queue.
	 * 
	 * @return
	 * 		the total number of Messages added
	 */
	public long getTotal();
	
	/**
	 * The current size of the queue
	 * 
	 * @return
	 * 		int representation of the current queue's size
	 */
	public int getSize();
}
