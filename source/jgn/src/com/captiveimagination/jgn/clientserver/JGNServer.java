/**
 * Copyright (c) 2005-2007 JavaGameNetworking
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
 * Created: Jul 11, 2006
 */
package com.captiveimagination.jgn.clientserver;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.message.PlayerStatusMessage;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * JGNServer is responsible for the server part in the classical Client/Server concept. A JGNServer is the
 * central rendevouz point in the c/s network and builds the only direct network address that clients will
 * normally know about.
 * <p/>
 * It's main purpose is the routing of messages sent by clients on to (all) other clients. In it's current
 * implementation JGNClients know of each other by their PlayerID, which are assigned by this JGNServer only.
 * Whenever a new Client connects, or a connected client disconnects, JGNServer will notify all other
 * JGNClients by means of PlayerStatusMessages that contain the resp. playerId and the connectionstatus.
 * <p/>
 * <p/>
 * Note, that JGN only provides for the Message-transportation layer, eg. there should normally be an
 * application both 'behind' the JGNServer and the JGNClient.
 * See the demo Chat application (in com.captiveimagnation.jgn.test.chat) for an example and usage.
 * <p/>
 * A JGNServer/JGNClient pair can (but need not) use both TCP and UDP protocols in parallel. When both are
 * used, the reliable (TCP) protocol will only be used for CertifiedMessages, all other types will use the
 * fast (UDP) protocol.
 * <p/>
 * A JGNServer implements Updatable, and as such needs a periodical refreshment by calling update(). Typically,
 * as in other JGN products, this will be accomplished by a seperate thread that could be controlled for instance
 * by methods in JGN(.java), that manage UpdatableRunnable's.
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public class JGNServer implements Updatable {

	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.clientserver.JGNServer");

	private MessageServer reliableServer;
	private MessageServer fastServer;
	private boolean connectionLinking;

	// registry holds records for each registered playerId together with their TCP/UDP MessageClients
	// eg. Communication endpoints.
	private ConcurrentLinkedQueue<JGNDirectConnection> registry;
	// listeners will be notified when a JGNClient dis/connects to this server
	private ConcurrentLinkedQueue<JGNConnectionListener> listeners;

	/**
	 * Create a new JGNServer instance, that uses either TCP, or UDP, or both. To NOT use a special
	 * transport, just set its address parameter = null. Although it's possible to set both parameters
	 * to null, this is not overly senseful...
	 * <p/>
	 * The JGNServer will bind it's MessageServer's to the addresses given, but it's updateThread must
	 * still be started().
	 *
	 * @param reliableAddress SocketAddress where the reliable(TCPMessage)Server will listen, or null
	 * @param fastAddress		 where the unreliableButFast(UDPMessage)Server will listen, or null
	 * @throws IOException when problems in creating one of the MessageServers
	 */
	public JGNServer(SocketAddress reliableAddress, SocketAddress fastAddress) throws IOException {
		this(reliableAddress != null ? new TCPMessageServer(reliableAddress) : null,
				fastAddress != null ? new UDPMessageServer(fastAddress) : null);
	}

	/**
	 * given a previously created TCP- and/or UDP-Server, create a JGNServer.
	 * Internally it will setup the lists used for listening to connectionsand registering JGNClients. It
	 * also installs a ServerClientConnectionController for controlling the behaviour for incoming connections
	 * and messages.
	 *
	 * @param reliableServer
	 * @param fastServer
	 */
	public JGNServer(MessageServer reliableServer, MessageServer fastServer) {
		this.reliableServer = reliableServer;
		this.fastServer = fastServer;
		registry = new ConcurrentLinkedQueue<JGNDirectConnection>();
		listeners = new ConcurrentLinkedQueue<JGNConnectionListener>();
		
		ServerClientConnectionController controller = new ServerClientConnectionController(this);

		if (reliableServer != null) {
			reliableServer.setConnectionController(controller);
			reliableServer.addConnectionListener(controller);
			reliableServer.addMessageListener(controller);
		}
		if (fastServer != null) {
			fastServer.setConnectionController(controller);
			fastServer.addConnectionListener(controller);
			fastServer.addMessageListener(controller);
		}
		// logging stuff
		if (LOG.isLoggable(Level.INFO)) {
			String mySrvTCP = (reliableServer == null) ? "none" : "(" + reliableServer.getMessageServerId() + ")";
			String mySrvUDP = (fastServer == null) ? "none" : "(" + fastServer.getMessageServerId() + ")";
			String myId = "JGNServer created using TCP=" + mySrvTCP + ", UDP=" + mySrvUDP;
			LOG.info(myId);
	}
	
	}
	
	public MessageServer getReliableServer() {
		return reliableServer;
	}
	
	public MessageServer getFastServer() {
		return fastServer;
	}
	
	protected boolean hasBoth() {
		return (reliableServer != null) && (fastServer != null);
	}

	/**
	 * ********************************* Updatable *****************************
	 */

	public void update() throws IOException {
		updateTraffic();
		updateEvents();
		updateConnections();
	}
	
	public void updateConnections() {
		if (reliableServer != null) reliableServer.updateConnections();
		if (fastServer != null) fastServer.updateConnections();
	}
	
	public void updateTraffic() throws IOException {
		if (reliableServer != null) reliableServer.updateTraffic();
		if (fastServer != null) fastServer.updateTraffic();
	}
	
	public void updateEvents() {
		if (reliableServer != null) reliableServer.updateEvents();
		if (fastServer != null) fastServer.updateEvents();
	}

	/**
	 * checks if underlying MessageServers are still alive. If there are 2 MS at least
	 * one of them must be alive to return true.
	 * Note. A MS not being alive means, it has terminated (or is about to terminate).
	 *
	 * @return true if at least one of the underlying MessageServers is alive
	 */
	public boolean isAlive() {
		if ((reliableServer != null) && (reliableServer.isAlive())) return true;
		return (fastServer != null) && (fastServer.isAlive());
	}

	/**
	 * closes the underlying MessageServer(s).
	 * Note: this is asynchronous. Returning from this method does not imply that the
	 * MS are closed already...
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (reliableServer != null) reliableServer.close();
		if (fastServer != null) fastServer.close();
		if (LOG.isLoggable(Level.FINE)) {
			String mySrvTCP = (reliableServer == null) ? "none" : "(" + reliableServer.getMessageServerId() + ")";
			String mySrvUDP = (fastServer == null) ? "none" : "(" + fastServer.getMessageServerId() + ")";
			String myId = "JGNServer closing. TCP=" + mySrvTCP + ", UDP=" + mySrvUDP;
			LOG.fine(myId);
		}
	}

	/******************************** Connection related *********************/

	/**
	 * an array representation of my connections-list. Note, there may be up to 2 MessageClients
	 * (one for TCP, one for UDP) for each connection. Both are associated with same playerId.
	 *
	 * @return all player-connection that I do hold connections to
	 */
	public JGNConnection[] getConnections() {
		return registry.toArray(new JGNConnection[registry.size()]);
	}
	
	/**
	 * return the connection information that contains the given MC.
	 * JGNConnection remembers a playerid and the communication endpoints (fast and/or
	 * reliable MessageClients) for that client. So we can find out:
	 * 1. what the playerId is
	 * 2. if this MC is either 'fast', or 'reliable'
	 * 3. what the other transport's MC is, if any
	 *
	 * @param client for which we look
	 * @return the connection object
	 */
	public JGNConnection getConnection(MessageClient client) {
		String clientAdr;
		if (LOG.isLoggable(Level.FINEST)) {
			clientAdr = getClientHash(client);
			LOG.finest(" for MessageClient " + clientAdr);
		}
		for (JGNDirectConnection connection : registry) {
			if ((connection.getFastClient() != null) && (connection.getFastClient().getId() == client.getId())) {
				MessageClient c = connection.getFastClient();
				LOG.finest(" found fast client " + getClientHash(c));
				return connection;
			} else
			if ((connection.getReliableClient() != null) && (connection.getReliableClient().getId() == client.getId())) {
				MessageClient c = connection.getReliableClient();
				LOG.finest(" found reliable client " + getClientHash(c));
				return connection;
			}
		}
		LOG.finest(" found nothing");
		return null;
	}
	
	private static String getClientHash(MessageClient client) {
		return "@" + Integer.toHexString(client.hashCode());
	}

	/**
	 * get connection info for a given playerId
	 *
	 * @param playerId
	 * @return JGN(Direct)Connection or null, if not registered
	 */
	public JGNConnection getConnection(short playerId) {
		for (JGNDirectConnection connection : registry) {
			if (connection.getPlayerId() == playerId) {
				return connection;
			}
		}
		return null;
	}
	
	/**
	 * disconnect a player from one/both underlying MessageServers
	 *
	 * @param playerId
	 * @return true, if JGNConnection existed, false else
	 * @throws IOException
	 */
	public boolean disconnect(short playerId) throws IOException {
		LOG.log(Level.FINEST, "asked to disconnect player {0}", playerId);
		JGNDirectConnection connection = (JGNDirectConnection)getConnection(playerId);
		if (connection != null) {
			connection.disconnect();
			LOG.log(Level.FINER, "executed disconnect on player {0}", playerId);
			return true;
		}
		LOG.log(Level.WARNING, "asked to disconnect non-existing player {0}", playerId);
		return false;
	}
	
	/**
	 * When true, if a client's reliable or fast MessageClient connection drops, the other connection will be disconnected
	 * manually. When set to false and a client's connection drops, the reliable connection generally disconnects quickly but the
	 * fast connection won't disconnect until it has timed out.
	 */
	public void setConnectionLinking (boolean connectionLinking) {
		this.connectionLinking = connectionLinking;
	}

	/**************************************** Registration ***************************/

	/**
	 * This is a callback from ServerClientConnectionController.negotiate().
	 * Whenever a new JGNClient connected to one of the MessageServers, it created a new MessageClient.
	 * While negotiating this connection the connection controller will ask this registry, if it has
	 * some info about the MC and return that.
	 * If no info available, register() will create a new JGNDirectConnection, and insert the given MC
	 * into the correct slot (reliable/fast) based on the MC's MessageServer.ServerType.
	 *
	 * @param client MessageClient to lookup
	 * @return a ConnectionInfo
	 */
	protected synchronized JGNConnection register(MessageClient client) {
		String clientAdr;
		if (LOG.isLoggable(Level.FINEST)) {
			clientAdr = getClientHash(client);
			LOG.finest("called for MessageClient " + clientAdr + " (" + client.getMessageServer().getServerType() + ")");
		}
		JGNDirectConnection connection = (JGNDirectConnection) getConnection(client);
		if (connection == null) {
			connection = new JGNDirectConnection();
			registry.add(connection);
			LOG.finest("new JGNDirectConnection created");
		}
		if (client.getMessageServer().getServerType() == MessageServer.ServerType.TCP) {
			connection.setReliableClient(client);
		} else {
			connection.setFastClient(client);
		}
		return connection;
	}
	
	/**
	 * This is a callback from ServerClientConnectionController.disconnected().
	 * When a JGNClient disconnected from one of the MessageServers that fact must be reflected in the
	 * Connection record: The resp. JGNConnection is looked up for the given MC and the correct Client will
	 * be nulled out.
	 * If, after that, both (fast + reliable) clients are null (no connection for this JGNClient at all) the
	 * method will remove the record from the registry.
	 * Then it sends to all other players a PlayerStatusMessage, telling that the playerId stored in
	 * JGNConnection was disconnected.
	 * As a last step, all registered JGNConnectionListeners will be informed.
	 *
	 * @param client MessageClient to lookup
	 * @return the old JGN(Direct)Connection
	 */
	protected synchronized JGNConnection unregister(MessageClient client) {
		String clientAdr;
		if (LOG.isLoggable(Level.FINEST)) {
			clientAdr = getClientHash(client);
			LOG.finest("called for MessageClient " + clientAdr + " (" + client.getMessageServer().getServerType() + ")");
		}
		JGNDirectConnection connection = (JGNDirectConnection)getConnection(client);
		if (connection == null) return null;
		if (connection.getFastClient() == client) {
			connection.setFastClient(null);
		} else if (connection.getReliableClient() == client) {
			connection.setReliableClient(null);
		}
		boolean disconnect;
		if (connectionLinking)
			disconnect = (connection.getFastClient() == null) || (connection.getReliableClient() == null);
		else
			disconnect = (connection.getFastClient() == null) && (connection.getReliableClient() == null);
		if (disconnect) {
			registry.remove(connection);
			LOG.log(Level.FINEST, "JGNDirectConnection removed for player {0}", connection.getPlayerId());

			// Make sure both connections are closed.
			if (connectionLinking) {
				if (connection.getFastClient() != null) {
					connection.getFastClient().disconnect();
					connection.setFastClient(null);
				}
				if (connection.getReliableClient() != null) {
					connection.getReliableClient().disconnect();
					connection.setReliableClient(null);
				}
			}

			// Send disconnection message to all other players
			PlayerStatusMessage psm = new PlayerStatusMessage();
			psm.setPlayerId(connection.getPlayerId());
			psm.setPlayerStatus(PlayerStatusMessage.STATUS_DISCONNECTED);
			sendToAllExcept(psm, connection.getPlayerId());
			
			// Throw event to listeners of connection
			for (JGNConnectionListener listener : listeners) {
				listener.disconnected(connection);
			}
		}
		return connection;
	}

	/********************************* PlayerMessage Sending ******************/

	/**
	 * sends a playermessage to all registered players, except the given one
	 * All registered JGNConnections will be visited, and if the playerId does not match
	 * the exceptionPlayerId, the connection will be called to send the message.
	 *
	 * @param message					 to be distributed. Must extend Message and implement PlayerMessage
	 * @param exceptionPlayerId player to be skipped. Note: no harm if that didn't exist
	 */
	public <T extends Message & PlayerMessage> void sendToAllExcept(T message, short exceptionPlayerId) {
		JGNConnection[] connections = getConnections();
		for (JGNConnection connection : connections) {
			if (connection.getPlayerId() != exceptionPlayerId) {
				if (connection.isConnected()) {
					connection.sendMessage(message);
				}
			}
		}
	}
	
	/**
	 * sends a playermessage to the given playerId, if registered before.
	 * All registered JGNConnections will be visited, and if the playerId does match
	 * the given playerId, the connection will be called to send the message.
	 *
	 * @param message	to be distributed. Must extend Message and implement PlayerMessage
	 * @param playerId player to be made happy. Note: no effect if that id didn't exist
	 */
	public <T extends Message & PlayerMessage> void sendToPlayer(T message, short playerId) {
		if (!(message instanceof PlayerMessage)) {
			LOG.log(Level.WARNING, "message is not a playermessage: {0}", message);
			return;
		}
		
		JGNConnection[] connections = getConnections();
		for (JGNConnection connection : connections) {
			if (connection.getPlayerId() == playerId) {
				if (connection.isConnected()) {
					connection.sendMessage(message);
				}
			}
		}
	}
	
	/**
	 * sends a playermessage to all registered players.
	 * Internally, this method uses sendToAllExcept() with nonexisting playerID.
	 *
	 * @param message to be distributed. Must extend Message and implement PlayerMessage
	 */
	public <T extends Message & PlayerMessage> void sendToAll(T message) {
		sendToAllExcept(message, (short)-1);
	}
	

	/**
	 * ******************************** Notifications *********************************
	 */

	// JGNConnectionListeners
	public void addClientConnectionListener(JGNConnectionListener listener) {
		LOG.log(Level.FINEST, "add connectionListener {0}", listener);
		listeners.add(listener);
	}
	
	public boolean removeClientConnectionListener(JGNConnectionListener listener) {
		LOG.log(Level.FINEST, "remove connectionListener {0}", listener);
		return listeners.remove(listener);
	}
	
	protected ConcurrentLinkedQueue<JGNConnectionListener> getListeners() {
		return listeners;
	}
	
	// MessageListeners are forwarded to the underlying MessageServers!
	public void addMessageListener(MessageListener listener) {
		LOG.log(Level.FINEST, "add MessageListener {0}", listener);
		if (reliableServer != null) reliableServer.addMessageListener(listener);
		if (fastServer != null) fastServer.addMessageListener(listener);
	}
	
	public void removeMessageListener(MessageListener listener) {
		LOG.log(Level.FINEST, "remove MessageListener {0}", listener);
		if (reliableServer != null) reliableServer.removeMessageListener(listener);
		if (fastServer != null) fastServer.removeMessageListener(listener);
	}
	
}
