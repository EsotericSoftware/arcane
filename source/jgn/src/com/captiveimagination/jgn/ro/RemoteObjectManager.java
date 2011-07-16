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
import com.captiveimagination.jgn.MessageServer;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Matthew D. Hicks
 */
public class RemoteObjectManager {
	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.ro.RemoteObjectManager");
	private static final HashMap<MessageClient, HashMap<Class<?>, RemoteObjectHandler>> remoteProxyMap = new HashMap<MessageClient, HashMap<Class<?>, RemoteObjectHandler>>();
	private static final HashMap<MessageServer, HashMap<Class<?>, RemoteInvocationListener>> remoteObjectMap = new HashMap<MessageServer, HashMap<Class<?>, RemoteInvocationListener>>();

	public static final void registerRemoteObject(Class<?> remoteClass, RemoteObject object, MessageServer server) throws IOException {
		HashMap<Class<?>, RemoteInvocationListener> objectMap = remoteObjectMap.get(server);
		if (objectMap == null) {
			objectMap = new HashMap<Class<?>, RemoteInvocationListener>();
			remoteObjectMap.put(server, objectMap);
		}
		if (objectMap.containsKey(remoteClass)) {
			IOException iOE = new IOException(
					"A RemoteObject has already been registered by this name on this MessageServer: " + object.getClass().getName());
			LOG.log(Level.SEVERE, "", iOE);
			throw iOE;
		}
		RemoteInvocationListener ril = new RemoteInvocationListener(remoteClass, object, server);
		objectMap.put(remoteClass, ril);
	}

	public static final void unregisterRemoteObject(Class<? extends RemoteObject> remoteClass, MessageServer server) {
		HashMap<Class<?>, RemoteInvocationListener> objectMap = remoteObjectMap.get(server);
		RemoteInvocationListener ril = objectMap.remove(remoteClass);
		ril.close();
	}

	@SuppressWarnings("unchecked")
	public static final <T extends RemoteObject> T createRemoteObject(Class<? extends T> remoteClass, MessageClient client, long timeout) throws IOException {
		HashMap<Class<?>, RemoteObjectHandler> clientMap = remoteProxyMap.get(client);
		if (clientMap == null) {
			clientMap = new HashMap<Class<?>, RemoteObjectHandler>();
			remoteProxyMap.put(client, clientMap);
		}
		if (clientMap.containsKey(remoteClass)) {
			IOException iOE = new IOException(
					"A remote object by this name already exists for this MessageClient: " + remoteClass.getName());
			LOG.log(Level.SEVERE, "", iOE);
			throw iOE;
		}
		RemoteObjectHandler handler = new RemoteObjectHandler(remoteClass, client, timeout);

		Object o = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{remoteClass}, handler);

		clientMap.put(remoteClass, handler);

		return (T) o;
	}

	public static final void destroyRemoteObject(Class<? extends RemoteObject> remoteClass, MessageClient client) {
		HashMap<Class<?>, RemoteObjectHandler> clientMap = remoteProxyMap.get(client);
		RemoteObjectHandler handler = clientMap.remove(remoteClass);
		handler.close();
	}
}
