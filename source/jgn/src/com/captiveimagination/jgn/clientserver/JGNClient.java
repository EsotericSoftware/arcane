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
 * Created: Jul 15, 2006
 */
package com.captiveimagination.jgn.clientserver;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.message.PlayerStatusMessage;
import com.captiveimagination.jgn.event.MessageAdapter;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.LocalRegistrationMessage;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JGNClient realizes the client part in the classical Client/Server concept. A JGNClient can only
 * connect to it's JGNServer, while a JGNServer connects to multiple JGNClients. A JGNClient will
 * know of other participants only by means of the JGNServer notifying it of the existence of others.
 * <p/>
 * Sending a message to another player really means sending a PlayerMessage to the JGNServer, that in turn
 * will then 'route' the message to the recipient.
 * <p/>
 * Players are distinguished by their PlayerId. This id will be assigned by the JGNServer, when a JGNClient
 * connects to the JGNServer, and will be communicated during the negotiation process.
 * <p/>
 * Note, that JGN only provides for the Message-transportation layer, eg. there must be an application both
 * 'behind' the server and the JGNClient.
 * See the demo Chat application (in com.captiveimagnation.jgn.test.chat) for an example and usage.
 * <p/>
 * A JGNServer/JGNClient pair can (but need not) use both TCP and UDP protocols in parallel. When both are
 * used, the reliable (TCP) protocol will only be used for CertifiedMessages, all other types will use the
 * fast (UDP) protocol. Also note, that despite the fact that the associated MessageServers are listening on
 * the given addresses, there is no handling of INCOMING connections to this client. BTW, that's why it's called
 * client.
 * <p/>
 * A JGNClients implements Updatable, and as such needs a periodical refreshment by calling update(). Typically,
 * as in other JGN products, this will be accomplished by a seperate thread that could be controlled for instance
 * by methods in JGN(.java), that manage UpdatableRunnable's.
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public class JGNClient implements Updatable {

	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.clientserver.JGNClient");

	private long id;
	private short playerId;
	private MessageServer reliableServer;
	private MessageServer fastServer;

	// listeners to client-'connections'. These get informed if a playerId is dis/connected from
	// the server. This is not a real connection from this JGNClient.
	private ConcurrentLinkedQueue<JGNConnectionListener> listeners;
	// listeners to the one and only server-connection
	private ConcurrentLinkedQueue<JGNConnectionListener> serverListeners;
	
	// used to hold state on the server-connection, start 'not-connected'
	private boolean connectedFlag;
	// the one-and-only connection to the JGNServer
	private JGNDirectConnection serverConnection;
	// the list of (virtual) other client connections for the same server, identified by their playerId.
	// use them to route (via the server) messages to other players.
	private final ConcurrentLinkedQueue<JGNConnection> connections;
	
	/**
	 * Create a new JGNClient instance that doesn't listen for connections on
	 * any local ports (ie: is a client in the traditional sense)
	 * 
	 * The JGNClient is not connected yet, and it's updateThread must still be started().
	 * 
	 * @throws IOException when problems in creating one of the MessageServers
	 */
	public JGNClient() throws IOException {
		this(new TCPMessageServer(null), new UDPMessageServer(null));
	}
	
	/**
	 * Create a new JGNClient instance, that listens on either TCP, or UDP, or both. To NOT use a special
	 * transport, just set its address parameter = null. Although it's possible to set both parameters
	 * to null, this is not overly senseful...
 	 * Create a new JGNClient instance, that can use both TCP and UDP
	 * <p/>
	 * The JGNClient is not connected yet, and it's updateThread must still be started().
	 *
	 * @param reliableAddress SocketAddress where the reliable(TCPMessage)Server should work, or null
	 * @param fastAddress		 where the unreliableButFast(UDPMessage)Server should work, or null
	 * @throws IOException when problems in creating one of the MessageServers
	 */
	public JGNClient(SocketAddress reliableAddress, SocketAddress fastAddress) throws IOException {
		this(reliableAddress != null ? new TCPMessageServer(reliableAddress) : null, fastAddress != null ? new UDPMessageServer(fastAddress) : null);
	}
	
	/**
	 * given a previously created TCP- and/or UDP-Server, create a JGNClient.
	 * 
	 * Internally it will setup the lists used for listening to connections, establish an internal
	 * Messagelistener that looks after LocalRegistrationMessage (for setting the playerID) and for
	 * PlayerStatusMessages (to inform connection listeners on the dis/connect state of others). It
	 * also installs a ClientServerConnectionController for controlling the only connection (to the JGNServer).
	 *
	 * @param reliableServer
	 * @param fastServer
	 * @throws IOException when problems in creating one of the MessageServers
	 */
	public JGNClient(MessageServer reliableServer, MessageServer fastServer) throws IOException {
		id = JGN.generateUniqueId(); // this will be re-set after negotiation to be playerId

		this.reliableServer = reliableServer;
		this.fastServer = fastServer;
		
		connections = new ConcurrentLinkedQueue<JGNConnection>();
		listeners = new ConcurrentLinkedQueue<JGNConnectionListener>();
		serverListeners = new ConcurrentLinkedQueue<JGNConnectionListener>();
		MessageListener ml = new MessageAdapter() {
			public void messageReceived(Message message) {

				// listen for PlayerStatusMessages
				//   first, if a Connection record is Not registered, do just that if it referred to a connect
				//					 else ignore. But if we know about the player and it referrs to a disconnect, unregister
				//					 else ignore.
				//   second, notify each ConnectionListener registered with either Connected, or Disconnected.
				if (message instanceof PlayerStatusMessage) {
					PlayerStatusMessage psm = (PlayerStatusMessage)message;
					boolean pIsConnected = (psm.getPlayerStatus() == PlayerStatusMessage.STATUS_CONNECTED);
					short pid = psm.getPlayerId();

					JGNConnection connection = getConnection(pid);

					// unknown till now
					if (connection == null) {
						if (pIsConnected)
							connection = register(pid);
						// else ignore
					} else { // registered before
						if (! pIsConnected)
							unregister(pid);
						// else ignore
					}
					// motify listeners
					// TODO: should we notify also, if player wasn't registered ??
					for (JGNConnectionListener jcl : listeners) {
						if (pIsConnected)
							jcl.connected(connection);
						else
							jcl.disconnected(connection);
					}

					// listen also for LocalRegistrationMessages.
					//   This was send by JGNServer, and holds (as a speciality for JGN) in it's Message.id the
					//   playerId newly assigned from JGNServer.
					//   Use the playerId as new JGNClient.id
				} else if (message instanceof LocalRegistrationMessage) {
					LocalRegistrationMessage lrm = (LocalRegistrationMessage)message;
					short pid = (short) lrm.getId();
					setPlayerId(pid);
					LOG.info("JGNClient: playerId was assigned to " + pid);
				}
			}
		};
		
		// ClientServerConnectionController controls all Connection issues OUTGOING to the JGNServer
		//  (note ServerClientConnectionController works opposite)
		ClientServerConnectionController controller = new ClientServerConnectionController(this);
		
		if (reliableServer != null) {
			reliableServer.setConnectionController(controller);
			reliableServer.addMessageListener(ml);
		}
		if (fastServer != null) {
			fastServer.setConnectionController(controller);
			fastServer.addMessageListener(ml);
		}

		// logging stuff
		if (LOG.isLoggable(Level.INFO)) {
			String mySrvTCP = (reliableServer == null) ? "none" : "(" + reliableServer.getMessageServerId() + ")";
			String mySrvUDP = (fastServer == null) ? "none" : "(" + fastServer.getMessageServerId() + ")";
			String myId = "JGNClient created (id=" + id + "), TCP=" + mySrvTCP + ", UDP=" + mySrvUDP;
			LOG.info(myId);
	}
	}
	
	public long getId() {
		return id;
	}
	
	public short getPlayerId() {
		return playerId;
	}
	
	protected void setPlayerId(short playerId) {
		this.playerId = playerId;
		LOG.log(Level.FINE, "Player set to {0}", playerId);
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
	 * ******************* UPDATABLE ****************************
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

		// notify my serverconnection-listener, if any
		if (serverListeners.size() > 0) {
			if (!connectedFlag && isServerConnected()) {
			// Connection event for establishing server connection
			connectedFlag = true;
				for (JGNConnectionListener serverListener : serverListeners)
				serverListener.connected(serverConnection);

			} else if (connectedFlag && !isServerConnected()) {
			// Disconnection event for broken server connection
			connectedFlag = false;
				for (JGNConnectionListener serverListener : serverListeners)
				serverListener.disconnected(serverConnection);
			}
		}
	}

	/************************************ User Interface **********************************/

	/**
	 * Tries to connect this JGNClient to a JGNServer using the remote addresses for the transports.
	 * Under the hood, the JGBClient's internal servers are advised to connect themselfes to the remote.
	 * This operation is async, which means the connection may not really be established on return. Use
	 * connectAndWait() for being sure on this.
	 * <p/>
	 * Note, there will be a NPE, when you try to connect to a TCP address, but you didn't create a local
	 * TCP server (when creating the JGNClient). Also with UDP.
	 *
	 * @param reliableRemoteAddress where remote TCP is listening
	 * @param fastRemoteAddress		 where remote UDP is listening
	 * @throws IOException problems when internal servers try to connect, or there is already a connection
	 *                     to the one and only JGNServer.
	 */
	public void connect(SocketAddress reliableRemoteAddress, SocketAddress fastRemoteAddress) throws IOException {
		LOG.log(Level.FINER, "connecting to tcp: {0}; udp: {1}",
				new Object[]{reliableRemoteAddress, fastRemoteAddress});
		if (serverConnection != null) {
			LOG.severe("Server connection already exists, while trying to connect");
			throw new IOException("A connection already exists. Only one connection to a server may exist.");
		}
		serverConnection = new JGNDirectConnection();
		if (reliableRemoteAddress != null) {
			serverConnection.setReliableClient(reliableServer.connect(reliableRemoteAddress));
		}
		if (fastRemoteAddress != null) {
			serverConnection.setFastClient(fastServer.connect(fastRemoteAddress));
		}
	}
	
	/**
	 * Invokes connect() and then waits <code>timeout</code> for the connection to complete successfully.
	 * If it is unable to connect within the allocated time an IOException will be thrown.
	 * 
	 * @param reliableRemoteAddress
	 * @param fastRemoteAddress
	 * @param timeout
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void connectAndWait(SocketAddress reliableRemoteAddress, SocketAddress fastRemoteAddress, long timeout) throws IOException, InterruptedException {
		connect(reliableRemoteAddress, fastRemoteAddress);
		
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + timeout) {
			if (isServerConnected()) {
				LOG.log(Level.INFO, "JGN Connected");
				return;
			}
			Thread.sleep(10);
		}

		// Last attempt before failing
		if (reliableServer != null) {
			if (serverConnection.getReliableClient() == null) {
				LOG.warning("Connection to reliableRemoteAddress failed.");
				throw new IOException("Connection to reliableRemoteAddress failed.");
			} else if (!serverConnection.getReliableClient().isConnected()) {
				LOG.warning("Connection to reliableRemoteAddress failed.");
				throw new IOException("Connection to reliableRemoteAddress failed.");
			}
		} else if (fastServer != null) {
			if (serverConnection.getFastClient() == null) {
				LOG.warning("Connection to fastRemoteAddress failed.");
				throw new IOException("Connection to fastRemoteAddress failed.");
			} else if (!serverConnection.getFastClient().isConnected()) {
				LOG.warning("Connection to fastRemoteAddress failed.");
				throw new IOException("Connection to fastRemoteAddress failed.");
			}
		} else {
			//both reliableServer and fastServer are null
			LOG.warning("Connection failed. (both reliableServer and fastServer are null)");
			throw new IOException("Connection failed.");
		}
	}

	private boolean isServerConnected() {
		if ((serverConnection != null) && (serverConnection.isConnected())) {
			if ((reliableServer == null) == (serverConnection.getReliableClient() == null)) {
				if ((fastServer == null) == (serverConnection.getFastClient() == null)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * cease operations with my Server. Note this is asynch! eg. the process may not
	 * have finished, when returning from this method.
	 *
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		LOG.finer("Disconnecting...");
		if (serverConnection != null) {
			serverConnection.disconnect(); // this will disconnect the associated (local) MessageClients
			serverConnection = null;
			LOG.info("Disconnected!");
		}
	}

	/**
	 * terminates the associated MessageServers after discarding the MessageClients from them.
	 * Note this is asynchronous eg. the process may not have finished, when returning from this method.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		disconnect();
		if (reliableServer != null) reliableServer.close();
		if (fastServer != null) fastServer.close();
		LOG.info("all done!");
	}

	/**
	 * check if associated MessageServer(s) are/is still alive
	 *
	 * @return true if true, and false if false (silly one...)
	 */
	public boolean isAlive() {
		if ((reliableServer != null) && (reliableServer.isAlive())) return true;
		return (fastServer != null) && (fastServer.isAlive());
	}

	/****************************** Sending Messages *********************************/

	/**
	 * send the message to my server, and server will distribute the message to ALL other players.
	 *
	 * @param message
	 */
	public void broadcast(Message message) {
		if (! (message instanceof PlayerMessage)) {
			LOG.log(Level.WARNING, "message is not a playermessage: {0}", message);
			return;
		}
		LOG.log(Level.FINEST, "broadcast {0}", message);
		message.setPlayerId(playerId);
		message.setDestinationPlayerId((short) -2);
		getServerConnection().sendMessage(message);	 // this is a direct connection
	}

	/**
	 * send this message ONLY to the server. It will NOT be broadcasted there.
	 *
	 * @param message
	 */
	public long sendToServer(Message message) {
		if (! (message instanceof PlayerMessage)) {
			LOG.log(Level.WARNING, "message is not a playermessage: {0}", message);
		}
		message.setPlayerId(playerId);
		message.setDestinationPlayerId((short) -1);
		LOG.log(Level.FINEST, "sending {0}", message);
		return getServerConnection().sendMessage(message);	 // this is a direct connection
	}

	/**
	 * this message should go to the player who's id I've given. It will be sent to the
	 * server which only knows, how to distribute it, finally.
	 *
	 * @param message
	 * @param player
	 */
	public <T extends Message & PlayerMessage> void sendToPlayer(T message, short player) {
		if (!(message instanceof PlayerMessage)) {
			LOG.log(Level.WARNING, "message is not a playermessage: {0}", message);
			return;
		}
		message.setPlayerId(playerId);
		LOG.log(Level.FINEST, "sending {0} to player " + player, message);
		if (getConnection(player) != null)
			getConnection(player).sendMessage(message); // this is a relay connection
		/* note, this equivalent to:
			message.setDestinationPlayerId(player);
			getServerConnection().sendMessage(message);
		*/
	}


	/**
	 * ************************* Registering Other Players *********************
	 */

	private JGNConnection register(short playerId) {
		JGNConnection connection = new JGNRelayConnection(this, playerId);
		connections.add(connection);
		LOG.finest("registered a new RelayConnection for player " + playerId);
		return connection;
	}
	
	private void unregister(short playerId) {
		for (JGNConnection connection : connections) {
			if (connection.getPlayerId() == playerId) {
				connections.remove(connection);
				LOG.finest("unregistered the RelayConnection for player " + playerId);
				return;
			}
		}
		LOG.finest("unsuccessful removal of connection for player " + playerId);
	}

	/**
	 * an array representation of my connections-list
	 *
	 * @return all player-connection that I seem to be connected with
	 */
	public JGNConnection[] getConnections() {
		synchronized(connections) {
			return connections.toArray(new JGNConnection[connections.size()]);
		}
	}
	
	/**
	 * retrieve the virtual connection to another player
	 *
	 * @param playerId
	 * @return a JGN[Relay]Connection to the given playerId, or null, if non existent.
	 */
	public JGNConnection getConnection(short playerId) {
		for (JGNConnection connection : connections) {
			if (connection.getPlayerId() == playerId) return connection;
		}
		return null;
	}
	
	/**
	 * the one-and-only (real) connection, eg to my server. Use this to send messages to the
	 * server, if ever...
	 * Send messages to other players by using
	 * getConnection(playerid).sendMessage(PlayerMessage);
	 * This will automatically route the message to the server, using the DestinationPlayerId.
	 *
	 * @return conn to my Server
	 */
	public JGNDirectConnection getServerConnection() {
		return serverConnection;
	}
	
	/********************************* Notification **********************************/
	
	/**
	 * listeners, that will receive events concerned with dis/connect of other players
	 *
	 * @param listener
	 */
	public void addClientConnectionListener(JGNConnectionListener listener) {
		listeners.add(listener);
	}
	
	public boolean removeClientConnectionListener(JGNConnectionListener listener) {
		return listeners.remove(listener);
	}
	
	/**
	 * listeners, that will be informed, when my connection to the JGNServer (not) exist.
	 *
	 * @param listener
	 */
	public void addServerConnectionListener(JGNConnectionListener listener) {
		serverListeners.add(listener);
	}
	
	public boolean removeServerConnectionListener(JGNConnectionListener listener) {
		return serverListeners.remove(listener);
	}
	
	/**
	 * a standard MessageListener, that gets informed of all messages arriving at
	 * one or both of the associated MessageServers. Note, these listeners will receive
	 * notification of ALL messages, not just PlayerMessages.
	 *
	 * @param listener
	 */
	public void addMessageListener(MessageListener listener) {
		if (reliableServer != null) reliableServer.addMessageListener(listener);
		if (fastServer != null) fastServer.addMessageListener(listener);
	}
	
	public void removeMessageListener(MessageListener listener) {
		if (reliableServer != null) reliableServer.removeMessageListener(listener);
		if (fastServer != null) fastServer.removeMessageListener(listener);
	}
}
