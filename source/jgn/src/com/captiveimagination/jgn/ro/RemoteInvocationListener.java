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

import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.event.MessageAdapter;
import com.captiveimagination.jgn.message.Message;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Matthew D. Hicks
 */
public class RemoteInvocationListener extends MessageAdapter {

	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.ro.RemoteInvocationListener");

	private Class<?> remoteClass;
	private RemoteObject object;
	private MessageServer server;

	protected RemoteInvocationListener(Class<?> remoteClass, RemoteObject object, MessageServer server) {
		this.remoteClass = remoteClass;
		this.object = object;
		this.server = server;
		server.addMessageListener(this);
	}

	public void messageReceived(Message message) {
		if (message instanceof RemoteObjectRequestMessage) {
			RemoteObjectRequestMessage m = (RemoteObjectRequestMessage) message;
			if (m.getRemoteObjectName().equals(remoteClass.getName())) {
				RemoteObjectResponseMessage response = new RemoteObjectResponseMessage();
				response.setMethodName(m.getMethodName());
				response.setRemoteObjectName(m.getRemoteObjectName());
				try {
					Class[] classes;
					Object[] parameters = m.getParameters();
					if (parameters != null) {
						classes = new Class[parameters.length];
						for (int i = 0; i < parameters.length; i++) {
							if (parameters[i] != null) {
								classes[i] = parameters[i].getClass();
							}
						}
					} else {
						classes = new Class[0];
					}
					Method method = null;
					try {
						method = remoteClass.getMethod(m.getMethodName(), classes);
					} catch (NoSuchMethodException exc) {
						for (Method meth : remoteClass.getMethods()) {
							if ((meth.getName().equals(m.getMethodName())) && (meth.getParameterTypes().length == classes.length)) {
								method = meth;
								break;
							}
						}
					}
					if (method == null)
					  response.setResponse(new NoSuchMethodException());
					else {
						method.setAccessible(true); // may produce NPE!
						Object obj = method.invoke(object, parameters);

						// make _sure_ result is serializable
						if (obj != null) {
							Class objc = obj.getClass();
							if (! (objc.isPrimitive() || Serializable.class.isAssignableFrom(objc))) {
								IllegalArgumentException iAE = new IllegalArgumentException("result class is not serializable: " + objc);
								LOG.log(Level.SEVERE, "", iAE);
								response.setResponse(iAE);
							} else response.setResponse(obj);
						}
					}
				} catch (Exception exc) {
					LOG.log(Level.SEVERE, "", exc);
					response.setResponse(exc);
				}
				m.getMessageClient().sendMessage(response);
			}
		}
	}

	public void close() {
		server.removeMessageListener(this);
	}
}
