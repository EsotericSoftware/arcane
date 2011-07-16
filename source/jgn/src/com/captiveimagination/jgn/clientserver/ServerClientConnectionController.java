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
 * Created: Jul 22, 2006
 */
package com.captiveimagination.jgn.clientserver;

import com.captiveimagination.jgn.DefaultConnectionController;
import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.clientserver.message.PlayerStatusMessage;
import com.captiveimagination.jgn.event.ConnectionListener;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.LocalRegistrationMessage;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls behaviour of JGNServer.
 * Responsible for:	negotiating with a new connecting MessageClient,
 * 		assigning new playerIds
 * 		notifying other on a new player
 * 		delegate to JGNServer for disconnects
 * 		route received PlayerMessages to destination
 *
 * @author Matthew D. Hicks
 */
public class ServerClientConnectionController extends DefaultConnectionController implements ConnectionListener,
				MessageListener {

	private static Logger LOG = Logger
					.getLogger("com.captiveimagination.jgn.clientserver.ServerClientConnectionController");

	private JGNServer server;
	private boolean[] playerIds;

	public ServerClientConnectionController(JGNServer server) {
		this.server = server;
		playerIds = new boolean[Short.MAX_VALUE]; // wow, 32k players!! ( we count starting at 0 )
	}

	/**
	 * A new MessageClient (eg a new Connection either on TCP or UDP) was created and will be
	 * prepared for operation now. That happens as follows:
	 * 1.	the MC will be registered in JGNServer. That will use or create a JGNDirectConnection
	 * that holds the MC as given.
	 * 2.	if the connection was fresh, playerId was -1. In that case a new playerId will be determined
	 * and put into the connection record.
	 * 3.	A LocalRegistrationMessage will be created as usual, BUT the message.ID will be set to the
	 * newly created playerId. The receiving JGNClient will afterwards use this id to set it's new
	 * playerId.
	 * 4.	Only, if the JGNClient is 'fully' connected (eg. if the current JGNServer supports both protocols
	 * and there are now MessageClients for both, otherwise the client is 'fully' connected with one MC
	 * already):
	 * 5.		Inform all ConnectionListeners of JGNServer of the connect
	 * 6.		Send a new playerId - connecting PlayerStatusMessage to all OTHER JGNClients
	 * 7.		For each OTHER JGNClient send a PlayerStatusMessage about them to the NEW JGNClient.
	 *
	 * @param client
	 */
	@Override
	public void negotiate(MessageClient client) {
		LOG.log(Level.FINEST, "negotiating client {0}", ("@" + Integer.toHexString(client.hashCode())));
		JGNDirectConnection connection = (JGNDirectConnection)server.register(client);
		short playerId = connection.getPlayerId();
		if (playerId == -1) {
			playerId = nextPlayerId();
			connection.setPlayerId(playerId);
			LOG.log(Level.FINEST, "playerId set to " + playerId);
		}

		LocalRegistrationMessage message = new LocalRegistrationMessage();
		// Send negotiation message back
		message.setId(playerId);
		JGN.populateRegistrationMessage(message);
		client.sendMessage(message);

		if (((server.hasBoth()) && (connection.getReliableClient() != null) && (connection.getFastClient() != null))
						|| (!server.hasBoth())) { // here, if only single protocol - or both MCs exist; fall through, if conn not 'complete'

			// Throw event to listeners of connection if complete
			ConcurrentLinkedQueue<JGNConnectionListener> listeners = server.getListeners();
			for (JGNConnectionListener listener : listeners) {
				listener.connected(connection);
			}

			// Send connection message to all (old) connected clients
			PlayerStatusMessage psm = new PlayerStatusMessage();
			psm.setPlayerId(playerId);
			psm.setPlayerStatus(PlayerStatusMessage.STATUS_CONNECTED);
			server.sendToAllExcept(psm, playerId); // spare the newby

			// Send messages to the newby client for all established (old) connections
			JGNConnection[] connections = server.getConnections();
			for (JGNConnection conn : connections) {
				if (conn.getPlayerId() != playerId) { // all conn's but the newby's one
					psm.setPlayerId(conn.getPlayerId());
					// redundant: psm.setPlayerStatus(PlayerStatusMessage.STATUS_CONNECTED);
					connection.sendMessage(psm);
				}
			}
		}
		if (LOG.isLoggable(Level.FINEST)) {
			dumpConnections(server);
		}
	}

	// used only for logging
	private static void dumpConnections(JGNServer server) {
		JGNConnection[] all = server.getConnections();
		LOG.finest("Current Connections:");
		for (JGNConnection a : all) {
			JGNDirectConnection da = (JGNDirectConnection)a;
			String fast = (da.getFastClient() == null) ? "none" : da.getFastClient().getAddress().toString();
			String reli = (da.getReliableClient() == null) ? "none" : da.getReliableClient().getAddress().toString();
			LOG.finest("  " + a.getPlayerId() + ": tcp to " + reli + "; udp to " + fast);
		}
	}

	// remember all playerIds since I was born. Return next unused.
	// TODO: linear search not efficient for 'late comers'
	private synchronized short nextPlayerId() {
		for (int i = 0; i < playerIds.length; i++) {
			if (!playerIds[i]) {
				playerIds[i] = true;
				return (short)i;
			}
		}
		throw new RuntimeException("Ran out of player ids.");
	}

	private synchronized void restorePlayerId(short id) {
		if (id >= 0 && id < playerIds.length) playerIds[id] = false;
	}

	/**
	 * ********************* implements ConnectionListener ******************
	 */

	public void connected(MessageClient client) {
	}

	public void negotiationComplete(MessageClient client) {
	}

	// delegate disconnect logic to my server.unregister()
	public void disconnected(MessageClient client) {
		JGNDirectConnection c = (JGNDirectConnection)server.unregister(client);
		if (c != null) {
			if ((c.getFastClient() == null) && (c.getReliableClient() == null)) {
				restorePlayerId(c.getPlayerId());
			}
		}
	}

	/**
	 * ********************* implements MessageListener ********************
	 */

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}

	public void messageSent(Message message) {
	}

	public void kicked(MessageClient client, String reason) {// TODO this should be managed internally
	}

	// if the message is a PlayerMessage, find the connection for the destinationPlayerId
	// and send it thereto.
	public void messageReceived(Message message) {
		if (server == null) {
			return;
		} else if (message == null) {
			return;
		} else if (message.getMessageClient() == null) {
			return;
		} else if (server.getConnection(message.getMessageClient()) == null) {
			return;
		}
		
		if (message instanceof PlayerMessage) {
			short dp = message.getDestinationPlayerId();
			if ((message.getPlayerId() != -1) && (message.getPlayerId() != server.getConnection(message.getMessageClient()).getPlayerId())) {
				LOG.log(Level.WARNING, "MessageClient tried to send a message with the wrong playerId, ignoring (Received: " + message.getPlayerId() + ", Expected: " + server.getConnection(message.getMessageClient()).getPlayerId() + ")!");
				// TODO send something to cheat handler
				return;
			}
			if (dp == -2) { // broadcast to all
				// it's a pitty we can't use following:
				// server.sendToAllExcept((PlayerMessage)message, message.getPlayerId());
				//
				// since either (with cast to PlayerMessage) it will complain about not being a Message
				// or (without) it will complain about not being a PlayerMessage ...
				// Facit: use generics with care...
				JGNConnection[] connections = server.getConnections();
				for (JGNConnection connection : connections) {
					if (connection.getPlayerId() != message.getPlayerId()) { // not sender
						if (connection.isConnected()) {
							connection.sendMessage(message);
						}
					}
				}
			} else if (dp == -1) {
			} // that's a message for me (THE server), let another MessageListener handle that
			else { // send message to explicit dp
				JGNConnection connection = server.getConnection(dp);
				// TODO add validation features
				if (connection != null && connection.isConnected()) { // route message to destination
					connection.sendMessage(message);
				}
			}
		} // end of PlayerMessage handling. All other types fall through
	}
}
