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
 * Created: April 18, 2008
 */
package com.captiveimagination.jgn.ping;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.PingMessage;
import com.captiveimagination.jgn.message.PongMessage;

/**
 * @author Matt Hicks
 */
public class Ping {
	public static final Future<Long> ping(MessageClient client, TimeUnit unit) {
		PingMessage message = new PingMessage();
		message.setSendTime(System.nanoTime());
		PingFuture future = new PingFuture(message.getId(), unit);
		client.addMessageListener(future);
		client.sendMessage(message);
		return future;
	}
	
	public static final long pingAndWait(MessageClient client, TimeUnit unit) throws InterruptedException, ExecutionException {
		Future<Long> future = ping(client, unit);
		return future.get();
	}
}

class PingFuture implements Future<Long>, MessageListener {
	private long pingId;
	private Long pingTime;
	private TimeUnit unit;
	
	public PingFuture(long pingId, TimeUnit unit) {
		this.pingId = pingId;
		this.unit = unit;
	}
	
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	public Long get() throws InterruptedException, ExecutionException {
		while (pingTime == null) {
			Thread.sleep(1);
		}
		return unit.convert(pingTime, TimeUnit.NANOSECONDS);
	}

	public Long get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long time = System.nanoTime();
		timeout = TimeUnit.NANOSECONDS.convert(timeout, unit);	// Convert to nano
		while (pingTime == null) {
			Thread.sleep(1);
			if (System.nanoTime() > time + timeout) {
				return null;
			}
		}
		return unit.convert(pingTime, TimeUnit.NANOSECONDS);
	}

	public boolean isCancelled() {
		return false;
	}

	public boolean isDone() {
		return pingTime != null;
	}

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}

	public void messageReceived(Message message) {
		if (message instanceof PongMessage) {
			PongMessage pong = (PongMessage)message;
			if (pong.getPingId() == pingId) {
				pingTime = System.nanoTime() - pong.getSendTime();
				message.getMessageClient().removeMessageListener(this);
			}
		}
	}

	public void messageSent(Message message) {
	}
}