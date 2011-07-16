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
 * Created: Jun 23, 2006
 */
package com.captiveimagination.jgn.message;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.captiveimagination.jgn.message.type.*;
import com.captiveimagination.jgn.queue.OrderedMessageQueue;

/**
 * Messages implementing OrderedMessage will have a guaranteed order
 * assigned for receipt on the remote machine. OrderedMessage necessarily
 * extends GroupMessage as a group assignment must exist in order for the
 * ordering to be consistent. An OrderedMessage requires that messages be
 * in exact sequence with none lost and thus also extends CertifiedMessage.
 * 
 * @author Matthew D. Hicks
 */
public abstract class OrderedMessage extends Message implements GroupMessage, CertifiedMessage, Comparable<OrderedMessage> {
	private static final HashMap<OrderedMessageQueue, HashMap<Object, AtomicInteger>> orders = new HashMap<OrderedMessageQueue, HashMap<Object, AtomicInteger>>();
	
	private int orderId;
	
	public OrderedMessage() {
		orderId = -1;
	}
	
	public int getOrderId() {
		return orderId;
	}
	
	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}
	
	public static final synchronized void assignOrderId(OrderedMessage message, OrderedMessageQueue queue) {
		HashMap<Object,AtomicInteger> connectionOrders;
		if (orders.containsKey(queue)) {
			connectionOrders = orders.get(queue);
			//ase: was wrong:: connectionOrders.get(message.getMessageClient());
		} else {
			connectionOrders = new HashMap<Object,AtomicInteger>();
			orders.put(queue, connectionOrders);
		}
		
		if (message.getGroupId() == -1) {
			// No groupId, so we base it off the class
			if (connectionOrders.containsKey(message.getClass())) {
				int next = connectionOrders.get(message.getClass()).incrementAndGet();
				message.setOrderId(next);
			} else {
				AtomicInteger integer = new AtomicInteger(0);
				connectionOrders.put(message.getClass(), integer);
				message.setOrderId(integer.intValue());
			}
		} else {
			// An groupId has been assigned, so lets use it
			if (connectionOrders.containsKey(message.getGroupId())) {
				int next = connectionOrders.get(message.getGroupId()).incrementAndGet();
				message.setOrderId(next);
			} else {
				AtomicInteger integer = new AtomicInteger(0);
				connectionOrders.put(message.getGroupId(), integer);
				message.setOrderId(integer.intValue());
			}
		}
	}
	
	public int compareTo(OrderedMessage message) {
		return ((Integer)getOrderId()).compareTo(message.getOrderId());
	}
}
