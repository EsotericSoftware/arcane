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
 * Created: Aug 26, 2007
 */
package com.captiveimagination.jgn.synchronization;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.captiveimagination.jgn.Updatable;
import com.captiveimagination.jgn.clientserver.JGNClient;
import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.synchronization.message.SynchronizeCreateMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeRemoveMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeRequestIDMessage;

/**
 * Instantiate for each synchronization session you want to maintain.
 * 
 * @author Matthew D. Hicks
 */
public class SynchronizationManager implements Updatable, MessageListener, JGNConnectionListener {
	private JGNServer server;
	private JGNClient client;
	
	private short id;
	
	private Queue<Short> used;				// Only used by server
	private Queue<SyncWrapper> idQueue;		// Client only - when waiting for a syncObjectId
	
	private GraphicalController controller;
	
	private Queue<SyncWrapper> queue;
	private Queue<SyncWrapper> disabled;
	private Queue<SyncWrapper> passive;
	
	private Queue<SynchronizeCreateMessage> createQueue;
	private Queue<SynchronizeRemoveMessage> removeQueue;
	
	private Queue<SyncObjectManager> objectManagers;
	
	private boolean keepAlive;
	
	public SynchronizationManager(JGNServer server, GraphicalController controller) {
		this(server, controller, (short)-1);
	}
	
	public SynchronizationManager(JGNServer server, GraphicalController controller, short id) {
		this.server = server;
		this.controller = controller;
		this.id = id;
		server.addMessageListener(this);
		server.addClientConnectionListener(this);
		
		used = new ConcurrentLinkedQueue<Short>();
		
		init();
	}
	
	public SynchronizationManager(JGNClient client, GraphicalController controller) {
		this(client, controller, (short)-1);
	}
	
	public SynchronizationManager(JGNClient client, GraphicalController controller, short id) {
		this.client = client;
		this.controller = controller;
		this.id = id;
		client.addMessageListener(this);
		
		idQueue = new ConcurrentLinkedQueue<SyncWrapper>();
		
		init();
	}
	
	private void init() {
		keepAlive = true;
		queue = new ConcurrentLinkedQueue<SyncWrapper>();
		disabled = new ConcurrentLinkedQueue<SyncWrapper>();
		passive = new ConcurrentLinkedQueue<SyncWrapper>();
		objectManagers = new ConcurrentLinkedQueue<SyncObjectManager>();
		
		createQueue = new ConcurrentLinkedQueue<SynchronizeCreateMessage>();
		removeQueue = new ConcurrentLinkedQueue<SynchronizeRemoveMessage>();
	}
	
	public short getId() {
		return id;
	}
	
	/**
	 * Register an object authoritative from this peer.
	 * 
	 * @param object
	 * @param createMessage
	 * @param updateRate
	 * @throws IOException 
	 */
	public void register(Object object, SynchronizeCreateMessage createMessage, long updateRate) throws IOException {
		// Get player id
		short playerId = -1;
		if (client != null) {
			playerId = client.getPlayerId();
		}
		
		// Create SyncWrapper
		SyncWrapper wrapper = new SyncWrapper(object, updateRate, createMessage, playerId);
		if (server != null) {		// Server registering - we have the ids
			short id = serverNextId();
			wrapper.setId(id);
			wrapperFinished(wrapper);
		} else {					// Client registering - ask the server for an id
			// Create a request message for an id
			SynchronizeRequestIDMessage request = new SynchronizeRequestIDMessage();
			request.setSyncManagerId(getId());
			request.setRequestType(SynchronizeRequestIDMessage.REQUEST_ID);
			long id = client.sendToServer(request);
			wrapper.setWaitingId(id);
			System.out.println("Sent request to server for id");
			
			// Add it to the waiting queue
			idQueue.add(wrapper);
		}
	}
	
	private void wrapperFinished(SyncWrapper wrapper) {
		// Set object id
		wrapper.getCreateMessage().setSyncObjectId(wrapper.getId());
		wrapper.getCreateMessage().setSyncManagerId(getId());
		
		// Send create message onward
		if (client != null) {
			client.sendToServer(wrapper.getCreateMessage());
		} else {
			for(JGNConnection conn : server.getConnections()) {
				if(controller.proximity(wrapper.getObject(), conn.getPlayerId()) > 0f) {
					System.out.println("Sending create for "+wrapper.getId());
					wrapper.setLastAction(conn.getPlayerId(), (short)1);
					server.sendToPlayer(wrapper.getCreateMessage(), conn.getPlayerId());
				}
			}
		}
		
		// Add to manager to be updated
		queue.add(wrapper);
	}
	
	/**
	 * Unregister an object and remove from remote clients
	 * 
	 * @param object
	 * @return
	 * 		true if removed
	 */
	public boolean unregister(Object object) {
		// Find SyncWrapper
		SyncWrapper wrapper = findWrapper(object);
		if (wrapper == null) return false;

		boolean sendRemove = queue.contains(wrapper);
		
		// Remove remotely
		SynchronizeRemoveMessage remove = new SynchronizeRemoveMessage();
		remove.setSyncManagerId(getId());
		remove.setSyncObjectId(wrapper.getId());
		if (client != null) {
			if (sendRemove) {
				client.broadcast(remove);
				
				// Release id
				SynchronizeRequestIDMessage release = new SynchronizeRequestIDMessage();
				release.setRequestType(SynchronizeRequestIDMessage.RELEASE_ID);
				release.setSyncObjectId(wrapper.getId());
				client.sendToServer(release);
			}
		} else {
			//Check which players are near this object
			for(JGNConnection conn : server.getConnections()) {
				if(controller.proximity(wrapper.getObject(), conn.getPlayerId()) > 0f) {
					if(wrapper.hasLastAction(conn.getPlayerId())) {
						if(wrapper.getLastAction(conn.getPlayerId()) != 2) {
							server.sendToPlayer(remove, conn.getPlayerId());
						}
					}
				}
			}
			
			// Release id
			serverReleaseId(wrapper.getId());
		}
		
		// Remove from self
		if (queue.remove(wrapper)) {
			return true;
		} else if (passive.remove(wrapper)) {
			return true;
		}
		return disabled.remove(wrapper);
	}
	
	/**
	 * Re-enable a disabled object.
	 * 
	 * @param object
	 */
	public void enable(Object object) {
		SyncWrapper wrapper = findWrapper(object);
		disabled.remove(wrapper);
		queue.add(wrapper);
	}
	
	/**
	 * Stop sending updates for a sync object.
	 * 
	 * @param object
	 */
	public void disable(Object object) {
		SyncWrapper wrapper = findWrapper(object);
		queue.remove(wrapper);
		disabled.add(wrapper);
	}
	
	private final synchronized short serverNextId() {
		short id = (short)1;
		while (used.contains(id)) {
			id++;
		}
		used.add(id);
		return id;
	}
	
	private final void serverReleaseId(short id) {
		used.remove(id);
	}
	
	private SyncWrapper findWrapper(Object object) {
		SyncWrapper wrapper = null;
		for (SyncWrapper sync : queue) {
			if (sync.getObject() == object) {
				wrapper = sync;
				break;
			}
		}
		if (wrapper == null) {
			for (SyncWrapper sync : disabled) {
				if (sync.getObject() == object) {
					wrapper = sync;
					break;
				}
			}
		}
		if (wrapper == null) {
			for (SyncWrapper sync : passive) {
				if (sync.getObject() == object) {
					wrapper = sync;
					break;
				}
			}
		}
		return wrapper;
	}
	
	private SyncWrapper findWrapper(short syncObjectId) {
		SyncWrapper wrapper = null;
		for (SyncWrapper sync : queue) {
			if (sync.getId() == syncObjectId) {
				wrapper = sync;
				break;
			}
		}
		if (wrapper == null) {
			for (SyncWrapper sync : disabled) {
				if (sync.getId() == syncObjectId) {
					wrapper = sync;
					break;
				}
			}
		}
		if (wrapper == null) {
			for (SyncWrapper sync : passive) {
				if (sync.getId() == syncObjectId) {
					wrapper = sync;
					break;
				}
			}
		}
		return wrapper;
	}
	
	public short findSyncObjectId(Object obj) {
		SyncWrapper sync = findWrapper(obj);
		if(sync != null) {
			return sync.getId();
		}
		return -1;
	}
	
	public void addSyncObjectManager(SyncObjectManager som) {
		objectManagers.add(som);
	}
	
	public boolean removeSyncObjectManager(SyncObjectManager som) {
		return objectManagers.remove(som);
	}
	
	/**
	 * Called internally when a SynchronizeCreateMessage is received
	 * 
	 * @param message
	 */
	public Object create(SynchronizeCreateMessage message) {
		for (SyncObjectManager manager : objectManagers) {
			Object obj = manager.create(message);
			if (obj != null) {
				// Create SyncWrapper
				SyncWrapper wrapper = new SyncWrapper(obj, 0, message, message.getPlayerId());
				wrapper.setId(message.getSyncObjectId());
				passive.add(wrapper);
				if(server != null) {
					// Forward create message to clients
					for(JGNConnection conn : server.getConnections()) {
						if(controller.proximity(wrapper.getObject(), conn.getPlayerId()) > 0f && conn.getPlayerId() != wrapper.getOwnerPlayerId()) {
							System.out.println("Sending create for "+wrapper.getId());
							wrapper.setLastAction(conn.getPlayerId(), (short)1);
							server.sendToPlayer(wrapper.getCreateMessage(), conn.getPlayerId());
						}
					}
				}
				return obj;
			}
		}
		return null;
	}
	
	/**
	 * Called internally when a SynchronizeRemoveMessage is received
	 * 
	 * @param message
	 */
	public Object remove(SynchronizeRemoveMessage message) {
		SyncWrapper wrapper = findWrapper(message.getSyncObjectId());
		for (SyncObjectManager manager : objectManagers) {
			if (manager.remove(message, wrapper.getObject())) {
				unregister(wrapper.getObject());
				return true;
			}
		}
		return false;
	}

	public boolean isAlive() {
		return keepAlive;
	}

	public void update() throws Exception {
		// Create objects
		SynchronizeCreateMessage createMessage;
		while ((createMessage = createQueue.poll()) != null) {
			create(createMessage);
		}

		// Remove objects
		SynchronizeRemoveMessage removeMessage;
		while ((removeMessage = removeQueue.poll()) != null) {
			remove(removeMessage);
		}
		
		for (SyncWrapper sync : queue) {
			if (server != null) {
				sync.update(this, server, controller);
			} else {
				sync.update(this, client, controller);
			}
		}
	}
	
	public void shutdown() {
		keepAlive = false;
	}

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}

	public void messageReceived(Message message) {
		if (message instanceof SynchronizeCreateMessage) {
			if (((SynchronizeCreateMessage)message).getSyncManagerId() == getId()) {
				createQueue.add((SynchronizeCreateMessage)message);
			}
		} else if (message instanceof SynchronizeRemoveMessage) {
			if (((SynchronizeRemoveMessage)message).getSyncManagerId() == getId()) {
				removeQueue.add((SynchronizeRemoveMessage)message);
			}
		} else if (message instanceof SynchronizeMessage) {
			if (((SynchronizeMessage)message).getSyncManagerId() == getId()) {
				SynchronizeMessage m = (SynchronizeMessage)message;
				SyncWrapper wrapper = findWrapper(m.getSyncObjectId());
				if (wrapper == null) {
					System.err.println("Unable to find object: " + m.getSyncObjectId() + " on " + (server != null ? "Server" : "Client"));
					return;
				}
				Object obj = wrapper.getObject();
				if (controller.validateMessage(m, obj)) {
					// Successfully validated synchronization message
					controller.applySynchronizationMessage(m, obj);
					// Check to see if we should send the message on
					if(server != null) {
						wrapper.update(this, server, controller);
					}
				} else {
					// Failed validation, so we ignore the message and send back our own
					m = controller.createSynchronizationMessage(obj);
					m.setSyncManagerId(getId());
					m.setSyncObjectId(((SynchronizeMessage)message).getSyncObjectId());
					message.getMessageClient().sendMessage(m);
				}
			}
		} else if (message instanceof SynchronizeRequestIDMessage) {
			SynchronizeRequestIDMessage request = (SynchronizeRequestIDMessage)message;
			if (request.getSyncManagerId() == getId()) {
				if (request.getRequestType() == SynchronizeRequestIDMessage.REQUEST_ID) {
					short id = serverNextId();
					request.setRequestType(SynchronizeRequestIDMessage.RESPONSE_ID);
					request.setSyncManagerId(getId());
					request.setSyncObjectId(id);
					request.getMessageClient().sendMessage(request);
					System.out.println("Server provided id: " + id);
				} else if (request.getRequestType() == SynchronizeRequestIDMessage.RESPONSE_ID) {
					for (SyncWrapper wrapper : idQueue) {
						if (wrapper.getWaitingId() == request.getId()) {
							wrapper.setId(request.getSyncObjectId());
							System.out.println("Received id from server: " + request.getSyncObjectId());
							wrapperFinished(wrapper);
							idQueue.remove(wrapper);
							break;
						}
					}
				} else if (request.getRequestType() == SynchronizeRequestIDMessage.RELEASE_ID) {
					serverReleaseId(request.getSyncObjectId());
				}
			}
		}
	}

	public void messageSent(Message message) {
	}

	public void connected(JGNConnection connection) {
		// Client connected to server - send creation messages to new connection
		for (SyncWrapper wrapper : queue) {
			if(controller.proximity(wrapper.getObject(), connection.getPlayerId()) > 0f) {
				if(wrapper.hasLastAction(connection.getPlayerId())) {
					if(wrapper.getLastAction(connection.getPlayerId()) != 1) {
						connection.sendMessage(wrapper.getCreateMessage());
						wrapper.setLastAction(connection.getPlayerId(), (short)1);
						System.out.println("Sending create for "+wrapper.getId());
					}
				}
				else {
					connection.sendMessage(wrapper.getCreateMessage());
					wrapper.setLastAction(connection.getPlayerId(), (short)1);
					System.out.println("Sending create for "+wrapper.getId());
				}
			}
		}
		for (SyncWrapper wrapper : disabled) {
			if(controller.proximity(wrapper.getObject(), connection.getPlayerId()) > 0f) {
				if(wrapper.hasLastAction(connection.getPlayerId())) {
					if(wrapper.getLastAction(connection.getPlayerId()) != 1) {
						connection.sendMessage(wrapper.getCreateMessage());
						wrapper.setLastAction(connection.getPlayerId(), (short)1);
						System.out.println("Sending create for "+wrapper.getId());
					}
				}
				else {
					connection.sendMessage(wrapper.getCreateMessage());
					wrapper.setLastAction(connection.getPlayerId(), (short)1);
					System.out.println("Sending create for "+wrapper.getId());
				}
			}
		}
		for (SyncWrapper wrapper : passive) {
			if(controller.proximity(wrapper.getObject(), connection.getPlayerId()) > 0f) {
				if(wrapper.hasLastAction(connection.getPlayerId())) {
					if(wrapper.getLastAction(connection.getPlayerId()) != 1) {
						connection.sendMessage(wrapper.getCreateMessage());
						wrapper.setLastAction(connection.getPlayerId(), (short)1);
						System.out.println("Sending create for "+wrapper.getId()+" to "+connection.getPlayerId());
					}
				}
				else {
					connection.sendMessage(wrapper.getCreateMessage());
					wrapper.setLastAction(connection.getPlayerId(), (short)1);
					System.out.println("Sending create for "+wrapper.getId()+" to "+connection.getPlayerId());
				}
			}
		}
	}

	public void disconnected(JGNConnection connection) {
		// Remove all connections associated with this player
		for (SyncWrapper wrapper : passive) {
			if (wrapper.getOwnerPlayerId() == connection.getPlayerId()) {
				SynchronizeRemoveMessage removeMessage = new SynchronizeRemoveMessage();
				removeMessage.setSyncObjectId(wrapper.getId());
				removeQueue.add(removeMessage);
			}
		}
		//Remove values for lastServerUpdate and lastAction for this connection
		for (SyncWrapper wrapper : passive) {
			wrapper.removeLastServerUpdateKey(connection.getPlayerId());
			wrapper.removeLastActionKey(connection.getPlayerId());
		}
		for (SyncWrapper wrapper : queue) {
			wrapper.removeLastServerUpdateKey(connection.getPlayerId());
			wrapper.removeLastActionKey(connection.getPlayerId());
		}
		for (SyncWrapper wrapper : disabled) {
			wrapper.removeLastServerUpdateKey(connection.getPlayerId());
			wrapper.removeLastActionKey(connection.getPlayerId());
		}
	}
}