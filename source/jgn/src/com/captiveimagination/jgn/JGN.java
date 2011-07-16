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
 * Created: Jun 10, 2006
 */
package com.captiveimagination.jgn;

import com.captiveimagination.jgn.clientserver.message.PlayerStatusMessage;
import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.convert.Converter;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.ro.RemoteObjectRequestMessage;
import com.captiveimagination.jgn.ro.RemoteObjectResponseMessage;
import com.captiveimagination.jgn.so.ObjectCreateMessage;
import com.captiveimagination.jgn.so.ObjectDeleteMessage;
import com.captiveimagination.jgn.so.ObjectUpdateMessage;
import com.captiveimagination.jgn.synchronization.message.*;
import com.captiveimagination.jgn.translation.TranslatedMessage;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Foundational static class for various functionality that is abstract from any other
 * specific class but is necessary for the JGN project.
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public class JGN {
	static public final short ID_CLASS_STRING = 0;
	static public final short ID_NULL_OBJECT = -1;

	private static final Map<Short, Class<?>> idToClass = new ConcurrentHashMap<Short, Class<?>>();
	private static final Map<Class<?>, Short> classToId = new HashMap<Class<?>, Short>();
	// hierarchy maps a messageclass --> List of superclasses, interfaces upto Message.class
	private static final Map<Class<? extends Message>, ArrayList<Class<?>>> hierarchy = new HashMap<Class<? extends Message>, ArrayList<Class<?>>>();
	private static final int systemIdCnt; // used in populateLocalRegistryMessage()

	static {
		// Certain messages must be known before negotiation so this is explicitly done here
		short n = -2;
		// foundation
		register(LocalRegistrationMessage.class, n--);
		register(TimeSynchronizationMessage.class, n--);
		register(StreamMessage.class, n--);
		register(NoopMessage.class, n--);
		register(Receipt.class, n--);
		register(DisconnectMessage.class, n--);
		register(PingMessage.class, n--);
		register(PongMessage.class, n--);
		// remote objects
		register(RemoteObjectRequestMessage.class, n--);
		register(RemoteObjectResponseMessage.class, n--);
		// JGN base
		register(PlayerStatusMessage.class, n--);
		register(ChatMessage.class, n--);
		// sync
		register(SynchronizeCreateMessage.class, n--);
		register(SynchronizeRemoveMessage.class, n--);
		register(Synchronize2DMessage.class, n--);
		register(Synchronize3DMessage.class, n--);
		register(SynchronizePhysicsMessage.class, n--);
		register(SynchronizeDead2DMessage.class, n--);
		register(SynchronizeDead3DMessage.class, n--);
		register(SynchronizeRequestIDMessage.class, n--);
		// SharedObject Messages
		register(ObjectCreateMessage.class, n--);
		register(ObjectUpdateMessage.class, n--);
		register(ObjectDeleteMessage.class, n--);
		// Translation
		register(TranslatedMessage.class, n--);
		// Primitives.
		register(boolean.class, n--);
		register(byte.class, n--);
		register(char.class, n--);
		register(short.class, n--);
		register(int.class, n--);
		register(long.class, n--);
		register(float.class, n--);
		register(double.class, n--);
		// Primitive wrappers.
		register(Boolean.class, n--);
		register(Byte.class, n--);
		register(Character.class, n--);
		register(Short.class, n--);
		register(Integer.class, n--);
		register(Long.class, n--);
		register(Float.class, n--);
		register(Double.class, n--);
		// Other.
		register(String.class, n--);

		systemIdCnt = idToClass.size();
	}

	/**
	 * Clases that will be transferred over the network can be registered before the initial connectivity negotiation process for
	 * maximum performance. Registered objects transfer the object's class name as a short ID. Unregistered objects can be
	 * transferred but incur the hit of transferring the object's class name as a String. This is much less efficient and should be
	 * avoided.
	 * <p>
	 * This method invokes {@link Converter#register(Class)} on the converter that will handle the registered class.
	 * <p>
	 * By default all primitives (and primitive wrappers) and JGN system messages are registered as well as
	 * {@link java.lang.String}. To efficiently transfer ANY other classes over the network (whether your own or JDK classes like
	 * ArrayList, HashMap, etc), those classes must be registered.
	 * 
	 * @param c The class to be registered.
	 * @throws RuntimeException if the class has no parameterless constructor or cannot be serialized.
	 * 
	 * @see Converter
	 */
	public static final synchronized void register(Class<?> c) {
		if (idToClass.containsValue(c)) return;

		// Check for the existence of a parameterless constructor.
		// Since system classes don't need this check, it's put into the public access point:
		if (!Enum.class.isAssignableFrom(c)) {
			Constructor[] ctors = c.getConstructors();
			boolean hasZeroArgCtor = false;
			for (Constructor ctor : ctors) {
				if (ctor.getParameterTypes().length == 0) {
					hasZeroArgCtor = true;
					break;
				}
			}
			if (!hasZeroArgCtor) {
				// This class can't be serialized, this is an application error. Stop further processing.
				throw new RuntimeException("Fatal: Class cannot be serialized (missing no arg constructor): " + c.getName());
			}
		}
		short id = generateId();
		while (idToClass.containsKey(id) || id == 0) {
			id = generateId();
		}
		register(c, id);
	}

	private static final void register(Class<?> c, short id) {
		// Check if the object has a registered converter.
		try {
			Converter converter = Converter.getConverter(c);
			// Inform the converter that a class has been registered so it can perform any caching necessary.
			converter.register(c);
		} catch (ConversionException ex) {
			// This message can't be serialized, this is an application error. Stop further processing.
			throw new RuntimeException("Fatal: Class cannot be serialized: " + c.getName(), ex);
		}
		idToClass.put(id, c);
		classToId.put(c, id);
		// Store the message's class hierarchy.
		if (Message.class.isAssignableFrom(c))
			hierarchy.put((Class<? extends Message>)c, collectMessageClassHierarchy(c));
	}

	private static final short generateId() {
		return (short) Math.round(Math.random() * Short.MAX_VALUE);
	}

	/**
	 * Returns the id registered locally for the specified class.
	 *
	 * @param c - the message type, asked for
	 * @return short The id, or null if the class is not registered.
	 */
	public static final Short getRegisteredClassId(Class<?> c) {
		return classToId.get(c);
	}

	/**
	 * Returns a class for given id.
	 *
	 * @param typeId
	 * @return The object class or <code>null</code> if no class was registered with that id.
	 */
	public static final Class<?> getRegisteredClass(short typeId) {
		return idToClass.get(typeId);
	}

	/**
	 * returns a sorted list of superclasses upto Message.class. On each level all the implemented
	 * Interfaces are recursivly included. This hierarchy is used in DynamicMessageListener mimic,
	 * to find the next fitting method, if there is no method in Listener that deals directly with
	 * the argument class.
	 *
	 * @param c - a 'real' message class to get the hierarchy list for
	 * @return a list of classes and Interfaces representing the superclass/interface structure of c
	 *         or null, if c was not scanned before
	 */
	public static ArrayList<Class<?>> getMessageClassHierarchy(Class<? extends Message> c) {
		return hierarchy.get(c);
	}

	private static ArrayList<Class<?>> collectMessageClassHierarchy(Class<?> c) {
		ArrayList<Class<?>> list = new ArrayList<Class<?>>();
		do {
			if (list.contains(c)) break;
			list.add(c);
			collectInterfaces(list, c); // recursively find all interfaces and their super
		} while ((c = c.getSuperclass()) != Message.class); // next superclass
		return list;
	}

	private static void collectInterfaces(ArrayList<Class<?>> lst, Class c) {
		Class[] interfaces = c.getInterfaces();
		for (Class ifc : interfaces) {
			if (lst.contains(ifc)) break;
			lst.add(ifc);
			collectInterfaces(lst, ifc);
		}
	}

	/**
	 * fills the currently registered non-system object class names into the LRM,
	 * together with their local ID's.
	 * Will not contain sysmtem level messages (with ids < 0).
	 *
	 * @param message the LRM to be filled in
	 */
	public static final void populateRegistrationMessage(LocalRegistrationMessage message) {
		// destination arrays
		int nonSystem = idToClass.size() - systemIdCnt;
		short[] ids = new short[nonSystem];
		String[] names = new String[nonSystem];
		// registered keys
		Short[] registeredIds = idToClass.keySet().toArray(new Short[idToClass.keySet().size()]);

		// extract id + name
		int count = 0;
		for (Short id : registeredIds) {
			if (id < 0) continue;
			ids[count] = id;
			names[count] = idToClass.get(id).getName();
			count++;
		}

		message.setIds(ids);
		message.setRegisteredClasses(names);
		message.setPriority(PriorityMessage.PRIORITY_HIGH);

	}

	/**
	 * tries to generate a pretty random long
	 * note: random will return the same value
	 *        each time the application (eg. VM) will start anew.
	 *        This may be good for developers. Might consider using a private
	 *        java.util.Random for production.
	 *
	 * @return long
	 */
	public static final long generateUniqueId() {
		long id = Math.round(Math.random() * Long.MAX_VALUE);
		id += Math.round(Math.random() * Long.MIN_VALUE);
		return id;
	}

	/**
	 * convenience routine for creating a Runnable that wraps a list of Updatables
	 * each pass through the list will be sperated by 1 ms pause.
	 *
	 * @param updatables - one or more "tasks" to be done until they are not any more alive.
	 * @return Runnable - ready for wrapping into a thread and get started.
	 * @see Updatable
	 * @see UpdatableRunnable
	 */
	public static final Runnable createRunnable(Updatable... updatables) {
		return createRunnable(2, updatables);
	}

	/**
	 * same as createRunnable(Updatable... updatables) but lets you specify the sleep time
	 * between passes.
	 *
	 * @param sleep - time to snare in ms, if <=0 will instead Thread.yield()
	 * @param updatables -one or more "tasks" to be done until they are not any more alive.
	 * @return Runnable - ready for wrapping into a thread and get started.
	 */
	public static final Runnable createRunnable(long sleep, Updatable... updatables) {
		return new UpdatableRunnable(new DefaultUncaughtExceptionHandler(), sleep, updatables);
	}
	
	/**
	 * same as createRunnable(long sleep, Updatable... updatables) but lets you specify
	 * the UncaughtExceptionHandler to use.
	 *
	 * @param handler - the uncaught exception handler to use when an exception is not
	 * 					handled internally.
	 * @param sleep - time to snare in ms, if <=0 will instead Thread.yield()
	 * @param updatables -one or more "tasks" to be done until they are not any more alive.
	 * @return Runnable - ready for wrapping into a thread and get started.
	 */
	public static final Runnable createRunnable(UncaughtExceptionHandler handler, long sleep, Updatable... updatables) {
		return new UpdatableRunnable(handler, sleep, updatables);
	}

	/**
	 * convenience routine for creating a ready-to-run thread around a Runnable that wraps a
	 * list of Updatables. Each pass through the list will be separated by 1 ms pause.
	 *
	 * @param updatables - one or more "tasks" to be done until they are not any more alive.
	 * @return Thread
	 */
	public static final Thread createThread(Updatable... updatables) {
		return createThread(2, updatables);
	}

	/**
	 * same as createThread(Updatable... updatables) but lets you specify the sleep time
	 * between passes.
	 * The returned thread will have a name of "JGN_Upd"
	 *
	 * @param sleep - time to snare in ms, if <=0 will instead Thread.yield()
	 * @param updatables -one or more "tasks" to be done until they are not any more alive.
	 * @return Thread - ready for getting started.
	 */
	public static final Thread createThread(long sleep, Updatable... updatables) {
		Thread res = new Thread(createRunnable(sleep, updatables));
		res.setName("JGN_Upd" + res.getId());
		return res;
	}

	/**
	 * same as createThread(long sleep, Updatable... updatables) but lets you specify
	 * the UncaughtExceptionHandler to use.
	 * 
	 * The returned thread will have a name of "JGN_Upd"
	 *
	 * @param handler - the uncaught exception handler to use when an exception is not
	 * 					handled internally.
	 * @param sleep - time to snare in ms, if <=0 will instead Thread.yield()
	 * @param updatables -one or more "tasks" to be done until they are not any more alive.
	 * @return Thread - ready for getting started.
	 */
	public static final Thread createThread(UncaughtExceptionHandler handler, long sleep, Updatable... updatables) {
		Thread res = new Thread(createRunnable(handler, sleep, updatables));
		res.setName("JGN_Upd" + res.getId());
		return res;
	}
}

/**
 * A Runnable that within it's run method, cycles a list of updatables as long as they
 * stay alive. Additionally a sleep time between cycles can be specified
 */
class UpdatableRunnable implements Runnable {
	public static final ThreadLocal<UpdatableRunnable> local = new ThreadLocal<UpdatableRunnable>();
	
	private long sleep;
	private Updatable[] updatables;
	private Logger log;
	private boolean keepAlive;
	private UncaughtExceptionHandler handler;

	/**
	 * Creates the Runnable
	 *
	 * @param sleep      if > 0, will sleep this amount in ms between cycles;
	 *                   else no delay (but a Thread.yield instead)
	 *                   NOTE that bigger values may reduce the response time of the system
	 *                   but too small a value will possibly eat up most CPU time, and make other
	 *                   threads unresponsive.
	 * @param updatables the tasks, to be run by executing their update() method.
	 */
	public UpdatableRunnable(UncaughtExceptionHandler handler, long sleep, Updatable... updatables) {
		this.handler = handler;
		this.sleep = sleep;
		this.updatables = updatables;
		// note this logger is per instance (and not based on classname) !
		log = Logger.getLogger("com.captiveimagination.jgn.UpdRun");
	}

	/**
	 * periodically calls the update() method of all Updatables
	 * The owning thread will terminate, when all Updatables deliver isAlive() == false.
	 * <p/>
	 * Each Throwable during an update() will be wrapped into a RunTimeException and
	 * currently terminate ALL tasks and the owning thread.
	 */
	public void run () {
		local.set(this);
		keepAlive = true;
		boolean alive;
		long threadId = Thread.currentThread().getId(); // this is as of Jdk15!

		// note: use following line, to adjust the real id of the real thread
		//       with the fake id as of JDK14 logger!!
		log.info("JGN update thread " + threadId + " started");

		if (log.isLoggable(Level.FINER)) {
			for (Updatable u : updatables) {
				log.log(Level.FINER, " -works on: {0}", u);
			}
		}
		do {
			alive = false;
			for (Updatable u : updatables) {
				if (u.isAlive()) {
					alive = true; // if at least one u is alive(), keep running
					try {
						u.update();
					} catch (Throwable t) {
						// TODO ase: think again about termination
//						log.severe("Update thread " + threadId + " will die, because..");
//						log.log(Level.SEVERE, "-->", t);
//						throw new RuntimeException(t);
						handler.uncaughtException(Thread.currentThread(), t);
					}
				}
			}
			if (sleep > 0) {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException exc) {
					// no real need for --> exc.printStackTrace();
					// therefore this is space for rent...
				}
			} else {
				Thread.yield();
			}
		} while ((alive) && (keepAlive));
		log.info("JGN update thread " + threadId + " terminated");
	}
	
	public void shutdown() {
		keepAlive = false;
	}
}

class DefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {
    private static final Logger logger = Logger.getLogger(DefaultUncaughtExceptionHandler.class.getName());
    
	public void uncaughtException(Thread t, Throwable e) {
		if (e instanceof ConnectionException) {
			logger.log(Level.WARNING, "Uncaught connection exception", e);
		} else {
			logger.log(Level.SEVERE, "Uncaught exception: Terminating Thread.", e);
			UpdatableRunnable.local.get().shutdown();
		}
	}
}
