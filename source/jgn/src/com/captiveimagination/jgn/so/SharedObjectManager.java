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
 * Created: Aug 18, 2006
 */
package com.captiveimagination.jgn.so;

import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.Serializable;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.convert.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.magicbeans.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class SharedObjectManager extends MessageAdapter implements BeanChangeListener, Updatable, ConnectionListener {
	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.so.SharedObjectManager");
	private static SharedObjectManager instance;
	
	private boolean alive;
	private ConcurrentHashMap<Object, SharedObject> registry;
	private HashMap<String, Object> mapping;
	private ConcurrentHashMap<Class, HashMap<String, Converter>> converters;
	private ConcurrentLinkedQueue<Object> queue;
	private HashMap<String, Method> methodMap;
	private ByteBuffer outgoingBuffer;
	private ByteBuffer incomingBuffer;
	private ByteBuffer updateBuffer;
	private ByteBuffer createBuffer;
	private ObjectCreateMessage createMessage;
	private ObjectUpdateMessage updateMessage;
	private ObjectDeleteMessage deleteMessage;
	private ArrayList<SharedObjectListener> listeners;
	
	private SharedObjectManager() {
		alive = true;
		registry = new ConcurrentHashMap<Object, SharedObject>();
		mapping = new HashMap<String, Object>();
		converters = new ConcurrentHashMap<Class, HashMap<String, Converter>>();
		queue = new ConcurrentLinkedQueue<Object>();
		methodMap = new HashMap<String,Method>();
		outgoingBuffer = ByteBuffer.allocateDirect(512 * 1000);
		incomingBuffer = ByteBuffer.allocateDirect(512 * 1000);
		updateBuffer = ByteBuffer.allocateDirect(512 * 1000);
		createBuffer = ByteBuffer.allocateDirect(512 * 1000);
		createMessage = new ObjectCreateMessage();
		updateMessage = new ObjectUpdateMessage();
		deleteMessage = new ObjectDeleteMessage();
		listeners = new ArrayList<SharedObjectListener>();
	}
	
	public <T> T createSharedBean(String name, Class<? extends T> beanInterface) {
		T t = createSharedBeanInternal(name, beanInterface, true);
		applyCreated(name, t, null);
		return t;
	}
	
	// Method named "internal" should be private?
	public <T> T createSharedBeanInternal(String name, Class<? extends T> beanInterface, boolean localObject) {
		// Create Magic Bean
		MagicBeanManager manager = MagicBeanManager.getInstance();
		T bean = manager.createMagicBean(beanInterface);
		
		// Create class conversion if it doesn't already exist
		if (!converters.containsKey(beanInterface)) {
			HashMap<String, Converter> map = new HashMap<String, Converter>();
			Method[] methods = beanInterface.getMethods();
			for (Method m : methods) {
				if ((m.getName().startsWith("get")) && (m.getParameterTypes().length == 0)) {
					try {
						Method setter = beanInterface.getMethod("s" + m.getName().substring(1), m.getReturnType());
						if (setter != null) {
							String fld = m.getName(); // ...substring(3).toLowerCase(); was wrong
							String fldName = (fld.length() > 4) ?
																	(fld.substring(3,4).toLowerCase() + fld.substring(4)) :
																	 fld.substring(3,4).toLowerCase();								
							Class retType = m.getReturnType();
							try {
								map.put(fldName, Converter.getConverter(retType));
							} catch (ConversionException ex) {
								// TODO - How to handle failure here? Maybe method should throw ConversionException?
								LOG.log(Level.SEVERE, "Unable to create shared object.", ex);
								continue;
							}
							m.setAccessible(true);
							setter.setAccessible(true);
							methodMap.put(beanInterface.getName() + ".get." + fldName, m);
							methodMap.put(beanInterface.getName() + ".set." + fldName, setter);
						}
					} catch(NoSuchMethodException exc) {
						// We don't put it in if we can't find a setter with the same signature
					}
				}
			}
			converters.put(beanInterface, map);
		}
		
		// Create Listener and register with server
		manager.addBeanChangeListener(bean, this);
		
		// Create entry for this bean
		registry.put(bean, new SharedObject(name, bean, beanInterface, converters.get(beanInterface), localObject));
		mapping.put(name, bean);
		
		return bean;
	}
	
	public synchronized void update() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Object object;
		while ((object = queue.poll()) != null) {
			outgoingBuffer.clear();
			registry.get(object).update(outgoingBuffer, updateMessage);
		}
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public void shutdown() {
		alive = false;
	}

	public void beanChanged(Object object, String field, Object oldValue, Object newValue) {
		SharedObject so = registry.get(object);
		so.updated(field);
		if (!queue.contains(object)) queue.add(object);
		applyChanged(so.getName(), object, field, null);
	}
	
	protected Method getMethod(String key) {
		return methodMap.get(key);
	}
	
	public Object getObject(String name) {
		return mapping.get(name);
	}
	
	public void removeObject(Object object) {
		// TODO send messages to let everyone know this object should be deleted
		SharedObject so = registry.get(object);
		removeObjectInternal(so);
		applyRemoved(so.getName(), object, null);
	}
	
	private void removeObjectInternal(SharedObject object) {
		// TODO remove object from all servers, clients, and from the manager
	}
	
	public void addShare(Object object, MessageServer server) {
		createBuffer.clear();
		registry.get(object).add(server, createMessage, createBuffer);
	}
	
	public boolean removeShare(Object object, MessageServer server) {
		return registry.get(object).remove(server, deleteMessage);
	}
	
	public void addShare(Object object, MessageClient client) {
		createBuffer.clear();
		registry.get(object).add(client, createMessage, createBuffer);
	}
	
	public boolean removeShare(Object object, MessageClient client) {
		return registry.get(object).remove(client, deleteMessage);
	}
	
	public void enable(MessageServer server) {
		server.addMessageListener(this);
		server.addConnectionListener(this);
	}
	
	public void disable(MessageServer server) {
		server.removeMessageListener(this);
		server.removeConnectionListener(this);
	}
	
	public void enable(MessageClient client) {
		client.addMessageListener(this);
	}
	
	public void disable(MessageClient client) {
		client.removeMessageListener(this);
	}
	
	public void messageReceived(Message message) {
		if (message instanceof ObjectCreateMessage) {
			ObjectCreateMessage m = (ObjectCreateMessage)message;
			try {
				Object object = createSharedBeanInternal(m.getName(), Class.forName(m.getInterfaceClass()), false);
				SharedObject so = registry.get(object);
				so.addInternal(message.getMessageClient());		// TODO verify this is the best route to go
																// It will only send changes back to the source
				applyCreated(m.getName(), object, m.getMessageClient());
			} catch(ClassNotFoundException exc) {
				throw new MessageException("Unable to create shared bean from: " + m.getInterfaceClass(), exc);
			}
		} else if (message instanceof ObjectUpdateMessage) {
			ObjectUpdateMessage m = (ObjectUpdateMessage)message;
			Object object = getObject(m.getName());
			if (object != null) {
				SharedObject so = registry.get(object);
				
				incomingBuffer.clear();
				registry.get(object).apply(m, incomingBuffer);
				
				for (String field : m.getFields()) {
					applyChanged(m.getName(), object, field, m.getMessageClient());
					if (so.isLocal()) {
						beanChanged(object, field, null, null);		// Apply changes so they get sent to all registered
						// TODO make it so changes don't get sent to the originator of this change (m.getMessageClient())
					}
				}
			} else {
				System.err.println("Tried to update a null object(SharedObject): " + m.getName());
			}
		} else if (message instanceof ObjectDeleteMessage) {
			ObjectDeleteMessage m = (ObjectDeleteMessage)message;
			Object object = getObject(m.getName());
			SharedObject so = registry.get(object);
			if (object != null) {
				removeObjectInternal(so);
				applyRemoved(m.getName(), object, m.getMessageClient());
			}
		}
	}
	
	public void connected(MessageClient client) {
	}

	public void disconnected(MessageClient client) {
	}

	public void kicked(MessageClient client, String reason) {
	}

	public void negotiationComplete(MessageClient client) {
		Iterator<SharedObject> iterator = registry.values().iterator();
		SharedObject so;
		while (iterator.hasNext()) {
			so = iterator.next();
			if (so.contains(client.getMessageServer())) {
				updateBuffer.clear();
				so.broadcast(client, updateBuffer);
			}
		}
	}
	
	public void addListener(SharedObjectListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}
	
	public boolean removeListener(SharedObjectListener listener) {
		synchronized(listeners) {
			return listeners.remove(listener);
		}
	}
	
	protected void applyCreated(String name, Object object, MessageClient client) {
		for (SharedObjectListener listener : listeners) {
			listener.created(name, object, client);
		}
	}
	
	protected void applyChanged(String name, Object object, String field, MessageClient client) {
		for (SharedObjectListener listener : listeners) {
			listener.changed(name, object, field, client);
		}
	}
	
	protected void applyRemoved(String name, Object object, MessageClient client) {
		for (SharedObjectListener listener : listeners) {
			listener.removed(name, object, client);
		}
	}
	
	public static final SharedObjectManager getInstance() {
		if (instance == null) {
			instance = new SharedObjectManager();
		}
		return instance;
	}
}
