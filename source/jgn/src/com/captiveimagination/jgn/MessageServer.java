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
 * Created: Jun 5, 2006
 */
package com.captiveimagination.jgn;

import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.convert.Converter;
import com.captiveimagination.jgn.event.ConnectionListener;
import com.captiveimagination.jgn.event.DynamicMessageListener;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.LocalRegistrationMessage;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.NoopMessage;
import com.captiveimagination.jgn.queue.ConnectionQueue;
import com.captiveimagination.jgn.queue.MessageQueue;
import com.captiveimagination.jgn.queue.QueueFullException;
import com.captiveimagination.jgn.ro.RemoteObjectManager;
import com.captiveimagination.jgn.ro.ping.Ping;
import com.captiveimagination.jgn.ro.ping.ServerPing;
import com.captiveimagination.jgn.translation.DataTranslator;
import com.captiveimagination.jgn.translation.TranslatedMessage;
import com.captiveimagination.jgn.translation.TranslationManager;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MessageServer is the abstract foundation from which all sending and receiving
 * of Messages occur.
 * <p/>
 * Note, that the name Message'Server' doesn't refer to the common Server as in Client/Server sermon.
 * It's name stems from the fact, that it locally serves all connections to the outside world. These
 * connections are realized in (see) MessageClients. While the MessageServer is mainly concerned with
 * Connections, the MessageClients are mainly responsible for data handling, that will go around with
 * 'a little help' from the MessageServer.
 * <p/>
 * MessageServer implements Updatable which defines a method update(). Normally this method will be
 * called periodically to enable serving all MessageClients and related tasks.
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public abstract class MessageServer implements Updatable {
	public enum ServerType {
		Unknown, TCP, UDP }		 // set by subclasses

	public static long DEFAULT_TIMEOUT = 30 * 1000;
	public static ConnectionController DEFAULT_CONNECTION_CONTROLLER = new DefaultConnectionController();

	protected long serverId;
	private SocketAddress address;
	private ServerType srvType;
	private int maxQueueSize;
	private long connectionTimeout;
	private ConnectionQueue incomingConnections;			// Waiting for ConnectionListener handling
	private ConnectionQueue negotiatedConnections;		// Waiting for ConnectionListener handling
	private ConnectionQueue disconnectedConnections;	// Waiting for ConnectionListener handling
	private final ConcurrentLinkedQueue<ConnectionListener> connectionListeners;
	private final ConcurrentLinkedQueue<MessageListener> messageListeners;
	private final ArrayList<ConnectionFilter> filters;
	private ArrayList<DataTranslator> translators;
	protected ArrayList<String> blacklist;            // list of blocked IP-adresses; null = no blocks at all
	protected AbstractQueue<MessageClient> clients;
	protected boolean keepAlive;
	protected boolean alive;

	// log per instance, used for subclasses as well
	protected Logger log = Logger.getLogger("com.captiveimagination.jgn.MessageServer");

	private ConnectionController controller;

	public MessageServer(SocketAddress address, int maxQueueSize) throws IOException {
		serverId = JGN.generateUniqueId();
		srvType = ServerType.Unknown;

		this.address = address;
		this.maxQueueSize = maxQueueSize;

		keepAlive = true;
		alive = true;

		connectionTimeout = DEFAULT_TIMEOUT;
		incomingConnections = new ConnectionQueue();
		negotiatedConnections = new ConnectionQueue();
		disconnectedConnections = new ConnectionQueue();
		connectionListeners = new ConcurrentLinkedQueue<ConnectionListener>();
		messageListeners = new ConcurrentLinkedQueue<MessageListener>();
		filters = new ArrayList<ConnectionFilter>();
		translators = new ArrayList<DataTranslator>();
		clients = new ConcurrentLinkedQueue<MessageClient>();

		controller = DEFAULT_CONNECTION_CONTROLLER;

		addConnectionListener(InternalListener.getInstance());
		addMessageListener(InternalListener.getInstance());

		// Default registered RemoteObjects
		RemoteObjectManager.registerRemoteObject(Ping.class, new ServerPing(), this);
	}

	public void setMessageServerId(long serverId) {
		this.serverId = serverId;
		log.finer("server id set to " + serverId);
	}

	public long getMessageServerId() {
		return serverId;
	}

	public ServerType getServerType() {
		return srvType;
	}

	protected void setServerType(ServerType st) {
		if (srvType != ServerType.Unknown) {
			log.severe("Servertype cannot change after being assigned");
			throw new IllegalArgumentException("Servertype cannot change after being assigned");
		}
		srvType = st;
	}

	public SocketAddress getSocketAddress() {
		return address;
	}

	public boolean isAlive() {
		return alive;
	}

	public int getMaxQueueSize() {
		return maxQueueSize;
	}

	public void setConnectionTimeout(long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public ConnectionController getConnectionController() {
		return controller;
	}

	public void setConnectionController(ConnectionController controller) {
		this.controller = controller;
	}

	protected ConnectionQueue getIncomingConnectionQueue() {
		return incomingConnections;
	}

	protected ConnectionQueue getNegotiatedConnectionQueue() {
		return negotiatedConnections;
	}

	protected ConnectionQueue getDisconnectedConnectionQueue() {
		return disconnectedConnections;
	}

	public AbstractQueue<MessageClient> getMessageClients() {
		return clients;
	}

	public void setBlacklist(ArrayList<String> blcklst) {
		log.finer("blacklist will be set to " + blcklst);
		blacklist = blcklst;
	}

	public ArrayList<String> getBlacklist() {
		return blacklist;
	}

  /**
	 * Sends <code>message</code> to all connected clients.
	 *
	 * @param   message
	 * @return	total number of clients messages sent to
	 */
	public int broadcast(Message message) {
		int sent = 0;
		log.finest("starting broadcast message: " + message);
		for (MessageClient client : getMessageClients()) {
			try {
				client.sendMessage(message);
				sent++;
			} catch (ConnectionException exc) {
				// Ignore when broadcasting
			} catch (QueueFullException qfe) {
				// Ignore when broadcasting
			}
		}
		log.finest("finished broadcasting. Sent: " + sent);
		return sent;
	}

	public MessageClient getMessageClient(SocketAddress address) {
		for (MessageClient client : getMessageClients()) {
			if (client.getAddress().equals(address)) return client;
		}
		return null;
	}

	/**
	 * Establishes a connection to the remote host distinguished by
	 * <code>address</code>. This method is non-blocking.
	 *
	 * @param address
	 * @return A MessageClient will always be returned that references the connection
	 * 		either in-progress or that has already been established. Only one connection
	 * 		can exist from one MessageServer to another, so if a second request is made
	 * 		the original MessageClient will be returned and a new one will not be created.
	 * @throws IOException
	 */
	public abstract MessageClient connect(SocketAddress address) throws IOException;

	/**
	 * Exactly the same as connect, but blocks for <code>timeout</code> in milliseconds
	 * for the connection to be established and returned. If the connection is already
	 * established it will be immediately returned. If the connection is not established
	 * in the time allotted via timeout the client will be disconnected and will no longer
	 * attempt the connect.
	 * <p/>
	 * <b>WARNING</b>: Do not execute this method in the same thread as is doing the
	 * update calls or you will end up with a timeout every time.
	 *
	 * @param address
	 * @param timeout
	 * @return MessageClient for the connection that is established.
	 * @throws IOException
	 */
	public MessageClient connectAndWait(SocketAddress address, int timeout) throws IOException {
    MessageClient client = connect(address);
    if (client == null) return null; // can happen when IOE on channel.connect() see TCPMessageServer.connect

		log.finest("waiting on client to connect: " + client);
    long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + timeout) {
			if (client.isConnected()) {
				log.finest("client connected: " + client);
				return client;
			}
			try {
				Thread.sleep(10);
      } catch (InterruptedException e) { //
      }
    }
		// Last attempt before failing
		if (client.isConnected()) {
			log.finest("client connected: " + client);
			return client;
		}
    client.setCloseReason(MessageClient.CloseReason.TimeoutConnect);
		log.finest("timeout: failed connecting client " + client);
    client.disconnect();
		return null;
	}

	protected abstract void disconnectInternal(MessageClient client, MessageClient.CloseReason reason);

	/**
	 * Closes all open connections to remote clients by disconnecting local Clients.
	 * prepares to be shutdown
	 */
	public void close() throws IOException {
		log.finest("close() entered...");
		synchronized (getMessageClients()) {
			for (MessageClient client : getMessageClients()) {
				if (client.isConnected()) {
					client.disconnect();
					client.setCloseReason(MessageClient.CloseReason.ClosedByServer);
				}
			}
		}
		keepAlive = false;
		log.finest("... close() finished");
	}

	/**
	 * Exactly the same as close, but blocks for <code>timeout</code> in milliseconds for the
	 * server to not be 'alive' anymore.
	 * <p/>
	 * <b>WARNING</b>: Do not execute this method in the same thread as is doing the
	 * update calls or you will end up with a timeout every time.
	 *
	 * @param timeout in ms
	 * @throws IOException if no shutdown within allotted time
	 */
	public void closeAndWait(long timeout) throws IOException {
		log.finest("waiting on server to close...");
		close();
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() <= time + timeout) {
			if (!isAlive()) {
				log.finest("... done waiting on server to close...");
				return;
			}
			// _TODO: change this is ugly
			// I THINK, this is neither necessary nor senseful, since clients got 'disconnect() in
			// the call to close() above. No good, if we do it twice!!
			// BUT, we should wait until all clients closed(), that will set alive to
			// false in MessageServer.updateConnections()...

//      synchronized (getMessageClients()) {
//				if ((getMessageClients().size() > 0) && (getMessageClients().peek().getStatus() == MessageClient.Status.CONNECTED)) {
//					getMessageClients().peek().disconnect();
//				}
//			}
			try {
			Thread.sleep(10);
			} catch (InterruptedException e) {/**/}
		}
		log.severe("Server did not shutdown timely, still " +
				getMessageClients().size() + " MessageClients connected");
		throw new IOException("MessageServer did not shutdown within the allotted time (" + getMessageClients().size() + ").");
	}

	/**
	 * Processing incoming/outgoing traffic for this MessageServer
	 * implementation. Should handle incoming connections, incoming
	 * messages, and outgoing messages.
	 *
	 * @throws IOException
	 */
	public abstract void updateTraffic() throws IOException;

	/**
	 * Handles processing events for incoming/outgoing messages sending to
	 * the registered listeners for both the MessageServer and MessageClients
	 * associated as well as the incoming established connection events.
	 */
	public synchronized void updateEvents() {
		// Process incoming connections first
		while (!incomingConnections.isEmpty()) {
			MessageClient client = incomingConnections.poll();
			synchronized (connectionListeners) {
				for (ConnectionListener listener : connectionListeners) {
					listener.connected(client);
				}
			}
		}

		// Process incoming Messages to the listeners
		for (MessageClient client : clients) {
			notifyIncoming(client);
		}

		// Process outgoing Messages to the listeners
		for (MessageClient client : clients) {
			notifyOutgoing(client);
		}

		// Process certified Messages to the listeners
		for (MessageClient client : clients) {
			notifyCertified(client);
		}

		// Process the list of certified messages that have failed
		for (MessageClient client : clients) {
			notifyFailed(client);
		}

		// Process the list of certified messages that still are waiting for certification
		for (MessageClient client : clients) {
			List<Message> messages = client.getCertifiableMessageQueue().clonedList();
			for (Message m : messages) {
				if ((m.getTimestamp() != -1) && (m.getTimestamp() + m.getTimeout() < System.currentTimeMillis())) {
					if (m.getTries() == m.getMaxTries() || !client.isConnected()) {
						// Message failed
						log.log(Level.FINER, "couldn't certify message {0} on client {1}",
								new Object[]{m.toString(), client.toString()});
						client.getFailedMessageQueue().add(m);
						client.getCertifiableMessageQueue().remove(m);
					} else {
						log.log(Level.FINEST, "retry certifying message {0} on client {1}",
								new Object[]{m.toString(), client.toString()});
						m.setTries(m.getTries() + 1);
						m.setTimestamp(-1); // avoid being checked in this loop again! Note, will be overridden
					  		// in PacketCombiner, when it will finally be written out to the wire.
						try {
							client.getOutgoingQueue().add(m);	// We don't want to clone or reset the unique id
						} catch (QueueFullException qfe) {
							client.getCertifiableMessageQueue().add(m); // try again
						}
					}
				}
			}
		}

		// Process incoming negotiated connections
		while (!negotiatedConnections.isEmpty()) {
			MessageClient client = negotiatedConnections.poll();
			synchronized (connectionListeners) {
				for (ConnectionListener listener : connectionListeners) {
					listener.negotiationComplete(client);
				}
			}
		}

		// Process disconnected connections
		while (!disconnectedConnections.isEmpty()) {
			MessageClient client = disconnectedConnections.poll();
			synchronized (connectionListeners) {
				for (ConnectionListener listener : connectionListeners) {
					if (client.getKickReason() != null) {
						listener.kicked(client, client.getKickReason());
					}
					listener.disconnected(client);
				}
			}
		}
	}

	private static void sendToListener(Message message, MessageListener listener,
																		 MessageListener.MESSAGE_EVENT type) {
		if (listener instanceof DynamicMessageListener) {
			DynamicMessageListener dml = (DynamicMessageListener) listener;
			dml.handle(type, message, dml);
		} else switch (type) {
			case CERTIFIED:
				listener.messageCertified(message);
				break;
			case FAILED:
				listener.messageFailed(message);
				break;
			case RECEIVED:
				listener.messageReceived(message);
				break;
			case SENT:
				listener.messageSent(message);
				break;
//      will not happen, unless MessageTypes will be changed. That would yield a major rework....
//			default:
//				throw new RuntimeException("Unknown listener type specified: " + type);
		}
	}

	/**
	 * Process incoming Messages to the listeners
	 *
	 * @param client
	 */
	protected void notifyIncoming(MessageClient client) {
		MessageQueue incomingMessages = client.getIncomingMessageQueue();
		while (!incomingMessages.isEmpty()) {
			Message message = incomingMessages.poll();
			if (message == null) {
				System.err.println("********* SHOULDN'T HAPPEN - Message is null in MessageServer.notifyIncoming");
				continue;
			}
			synchronized (client.getMessageListeners()) {
				for (MessageListener listener : client.getMessageListeners()) {
					sendToListener(message, listener, MessageListener.MESSAGE_EVENT.RECEIVED);
				}
			}
			synchronized (messageListeners) {
				for (MessageListener listener : messageListeners) {
					sendToListener(message, listener, MessageListener.MESSAGE_EVENT.RECEIVED);
				}
			}
		}
	}

	/**
	 * Process outgoing Messages to the listeners
	 *
	 * @param client
	 */
	protected void notifyOutgoing(MessageClient client) {
		MessageQueue outgoingMessages = client.getOutgoingMessageQueue();
		while (!outgoingMessages.isEmpty()) {
			Message message = outgoingMessages.poll();
			synchronized (client.getMessageListeners()) {
				for (MessageListener listener : client.getMessageListeners()) {
					sendToListener(message, listener, MessageListener.MESSAGE_EVENT.SENT);
				}
			}
			synchronized (messageListeners) {
				for (MessageListener listener : messageListeners) {
					sendToListener(message, listener, MessageListener.MESSAGE_EVENT.SENT);
				}
			}
		}
	}

	/**
	 * Process certified Messages to the listeners
	 *
	 * @param client
	 */
	protected void notifyCertified(MessageClient client) {
		MessageQueue certifiedMessages = client.getCertifiedMessageQueue();
		while (!certifiedMessages.isEmpty()) {
			Message message = certifiedMessages.poll();
			synchronized (client.getMessageListeners()) {
				for (MessageListener listener : client.getMessageListeners()) {
					sendToListener(message, listener, MessageListener.MESSAGE_EVENT.CERTIFIED);
				}
			}
			synchronized (messageListeners) {
				for (MessageListener listener : messageListeners) {
					sendToListener(message, listener, MessageListener.MESSAGE_EVENT.CERTIFIED);
				}
			}
		}
	}

	/**
	 * Process the list of certified messages that have failed
	 *
	 * @param client
	 */
	protected void notifyFailed(MessageClient client) {
		MessageQueue failedMessages = client.getFailedMessageQueue();
		while (!failedMessages.isEmpty()) {
			Message message = failedMessages.poll();
			synchronized (client.getMessageListeners()) {
				for (MessageListener listener : client.getMessageListeners()) {
					sendToListener(message, listener, MessageListener.MESSAGE_EVENT.FAILED);
				}
			}
			synchronized (messageListeners) {
				for (MessageListener listener : messageListeners) {
					sendToListener(message, listener, MessageListener.MESSAGE_EVENT.FAILED);
				}
			}
		}
	}

	/**
	 * convenience routine that calls messagelisteners on all messageEvents in
	 * MessageClient.
	 * Used from DisconnectInternal()
	 *
	 * @param c
	 */
	protected void notifyClient(MessageClient c) {
		notifyIncoming(c);
		notifyOutgoing(c);
		notifyCertified(c);
		notifyFailed(c);
	}

	/**
	 * Processes all MessageClients associated with this MessageServer and
	 * checks for connections that have been closed or have timed out and
	 * removes them.
	 */
	public synchronized void updateConnections() {
    NoopMessage message = null;

    // Cycle through connections and see if anything to do
    for (MessageClient client : clients) {
      switch (client.getStatus()) {

        case NEGOTIATING:
          // disconnect if negotiating state too long
          if (client.lastReceived() + (connectionTimeout / 3) < System.currentTimeMillis()) {
						log.log(Level.FINER, "time out negotiating on {0} ", client);
            disconnectInternal(client, MessageClient.CloseReason.TimeoutConnect);
          }
          break;

        case CONNECTED:
          // disconnect, if timed out
          if (client.lastReceived() + connectionTimeout < System.currentTimeMillis()) {
            client.setCloseReason(MessageClient.CloseReason.TimeoutRead);
            client.disconnect();
            client.received();
						log.log(Level.FINER, "timed out while connected: {0}", client);
          }

          // send Noop, if needs
          if (client.lastSent() + (connectionTimeout / 3) < System.currentTimeMillis()) {
            if (message == null) message = new NoopMessage();
            try {
              client.sendMessage(message);
              client.sent();
						} catch (QueueFullException exc) {
              // well, ignore
            }
          }
          break;

        case DISCONNECTING:
          // disconnect finally, if possible
          if (client.isDisconnectable()) {
            // we don't know any more what led to this ...
						disconnectInternal(client, client.getCloseReason());
          }
          // staying too long in state
          else if (client.lastReceived() + (connectionTimeout / 3) < System.currentTimeMillis()) {
						log.log(Level.FINER, "timed out disconnecting: {0}", client);
            disconnectInternal(client, MessageClient.CloseReason.TimeoutDisconnect);
          }
          break;

        case DISCONNECTED:
          break;
        case NOT_CONNECTED:
          break;
      }
    }

    // If attempting to shutdown make sure all MessageClients are closed before alive = false
    if ((!keepAlive) && (alive)) {
      synchronized (getMessageClients()) {
        for (MessageClient client : getMessageClients()) {
          if (client.getStatus() != MessageClient.Status.DISCONNECTED) {
            return;
          }
        }
        alive = false;
				log.log(Level.FINE, "Server id=" + serverId + " shutdown: alive = false");
      }
    }
	}

	/**
	 * Convenience method to call updateTraffic(), updateEvents() and updateConnections().
	 * This is only necessary if you aren't explicitly calling these
	 * methods already.
	 *
	 * @throws IOException
	 */
	public void update() throws IOException {
		updateTraffic();
		updateEvents();
		updateConnections();
	}

	/**
	 * Adds a ConnectionListener to this MessageServer
	 *
	 * @param listener
	 */
	public void addConnectionListener(ConnectionListener listener) {
		synchronized (connectionListeners) {
			connectionListeners.add(listener);
		}
		log.finest("added ConnectionListener: " + listener);
	}

	/**
	 * Removes a ConnectionListener from this MessageServer
	 *
	 * @param listener
	 * @return true if the listener was contained in the list
	 */
	public boolean removeConnectionListener(ConnectionListener listener) {
		boolean result;
		synchronized (connectionListeners) {
			result = connectionListeners.remove(listener);
		}

		if (result)
			log.finest("removed ConnectionListener: " + listener);
		else
			log.finest("NOT removed ConnectionListener: " + listener);
		return result;
	}

	/**
	 * Adds a MessageListener to this MessageServer
	 *
	 * @param listener
	 */
	public void addMessageListener(MessageListener listener) {
		synchronized (messageListeners) {
			messageListeners.add(listener);
		}
		log.finest("added MessageListener: " + listener);
	}

	/**
	 * Removes a MessageListener from this MessageServer
	 *
	 * @param listener
	 * @return true if the listener was contained in the list
	 */
	public boolean removeMessageListener(MessageListener listener) {
		boolean result;
		synchronized (messageListeners) {
			result = messageListeners.remove(listener);
		}
		if (result)
			log.finest("removed MessageListener: " + listener);
		else
			log.finest("NOT removed MessageListener: " + listener);
		return result;
	}

	/**
	 * Adds a ConnectionFilter to this MessageServer
	 *
	 * @param filter
	 */
	public void addConnectionFilter(ConnectionFilter filter) {
		synchronized (filters) {
			filters.add(filter);
		}
		log.finest("added ConnectionFilter: " + filter);
	}

	/**
	 * Removes a ConnectionFilter from this MessageServer
	 *
	 * @param filter
	 * @return true if the filter was contained in the list
	 */
	public boolean removeConnectionFilter(ConnectionFilter filter) {
		boolean result;
		synchronized (filters) {
			result = filters.remove(filter);
		}
		if (result)
			log.finest("removed ConnectionFilter: " + filter);
		else
			log.finest("NOT removed ConnectionFilter: " + filter);
		return result;
	}

	/**
	 * find out if client should be kicked
	 *
	 * @param client
	 * @return String, the reason why we don't like the client, or null if no filtering
	 */
	protected String shouldFilterConnection(MessageClient client) {
		for (ConnectionFilter filter : filters) {
			String reason = filter.filter(client);
			if (reason != null) {
				log.log(Level.FINER, "filtered client {0} with '{1}'", new Object[]{client, reason});
				return reason;
			}
		}
		return null;
	}

	/**
	 * Adds a DataTranslator to this MessageServer
	 *
	 * @param translator
	 */
	public void addDataTranslator(DataTranslator translator) {
		translators.add(translator);
		log.finest("removed Translator: " + translator);
	}

	/**
	 * Removes a DataTranslator from this MessageServer
	 *
	 * @param translator
	 * @return true if the translator was contained in the list
	 */
	public boolean removeDataTranslator(DataTranslator translator) {
		boolean result;
		result = translators.remove(translator);
		if (result)
			log.finest("removed Translator: " + translator);
		else
			log.finest("NOT removed Translator: " + translator);
		return result;
	}

	// note, don't throw MHE anymore, all Exceptions caught within method (and rethrown)
	private void translateMessage(Message message) {
		if (translators.size() == 0) {
			return;
		}
		if (message instanceof LocalRegistrationMessage) {
			return;
		}
		try {
			TranslatedMessage tm = TranslationManager.createTranslatedMessage(message);
			byte[] b = tm.getTranslated();
			for (DataTranslator translator : translators) {
				b = translator.outbound(b);
			}
			tm.setTranslated(b);
			tm.setMessageClient(message.getMessageClient());
			message.setTranslatedMessage(tm);
		} catch (Exception exc) {
			// TODO: tis very hard, if we terminate here. Check!!
			log.log(Level.SEVERE, "outgoing Translation failed: ", exc);
			throw new MessageException("Exception during translation", exc);
		}
	}

	protected Message revertTranslated(TranslatedMessage tm) {//throws MessageHandlingException {
		try {
			byte[] b = tm.getTranslated();
			for (DataTranslator translator : translators) {
				b = translator.inbound(b);
			}
			return TranslationManager.createMessage(b);
		} catch (Exception exc) {
			// TODO: tis very hard, if we terminate here. Check!!
			log.log(Level.SEVERE, "incoming Translation failed: ", exc);
			throw new MessageException("Exception during translation", exc);
		}
	}

	// used only by PacketCombiner.combine()
	protected void convertMessage(Message message, ByteBuffer buffer) throws MessageHandlingException {
		translateMessage(message);

		Message m = message;
		if (message.getTranslatedMessage() != null) {
			m = message.getTranslatedMessage();
		}
		try {
			Converter.writeClassAndObject(message.getMessageClient(), m, buffer);
		} catch (ConversionException ex) {
			throw new MessageHandlingException("Unable to serialize message: " + message.getClass().getName(), null, ex);
		}
	}

}
