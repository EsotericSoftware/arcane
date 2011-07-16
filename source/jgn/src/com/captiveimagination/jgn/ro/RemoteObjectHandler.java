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
 * Created: Jul 13, 2006
 */
package com.captiveimagination.jgn.ro;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.event.MessageAdapter;
import com.captiveimagination.jgn.message.Message;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Matthew D. Hicks
 */
public class RemoteObjectHandler extends MessageAdapter implements InvocationHandler {
	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.ro.RemoteObjectHandler");

	private Class<? extends RemoteObject> remoteClass;
	private MessageClient client;
	private long timeout;

	private boolean received;
	private Object response;

	protected RemoteObjectHandler(Class<? extends RemoteObject> remoteClass, MessageClient client, long timeout) {
		this.remoteClass = remoteClass;
		this.client = client;
		this.timeout = timeout;

		client.addMessageListener(this);
	}

	public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		received = false;
		RemoteObjectRequestMessage request = new RemoteObjectRequestMessage();
		request.setRemoteObjectName(remoteClass.getName());
		request.setMethodName(method.getName());
		// make _sure_ args are serializable
		if (args == null) {
			args = new Object[0];
		}
		for (Object a : args) {
			Class ac = a.getClass();
			if (! (ac.isPrimitive() || Serializable.class.isAssignableFrom(ac))) {
				LOG.log(Level.SEVERE, "Argument class is not serializable: ", ac);
				throw new IllegalArgumentException("Argument class is not serializable: " + ac);
			}
		}

		request.setParameters(args);
//		System.out.println("SENDING MESSAGE: " + request);
		client.sendMessage(request);

		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + timeout) {
			if (received) break;
			Thread.sleep(1);
		}
		if (!received) {
			LOG.log(Level.WARNING, "Timeout waiting for response from remote machine.");
			throw new IOException("Timeout waiting for response from remote machine.");
		}

		Object obj = response;
		response = null;

		if (obj instanceof Throwable) throw (Throwable) obj;

		return obj;
	}

	public void messageReceived(Message message) {
		if (message instanceof RemoteObjectResponseMessage) {
			RemoteObjectResponseMessage m = (RemoteObjectResponseMessage) message;
			if (m.getRemoteObjectName().equals(remoteClass.getName())) {
				response = m.getResponse();
				received = true;
			}
		}
	}

	public void close() {
		client.removeMessageListener(this);
	}
}
