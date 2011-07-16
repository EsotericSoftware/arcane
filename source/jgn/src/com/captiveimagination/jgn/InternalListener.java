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

import com.captiveimagination.jgn.event.ConnectionListener;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.type.CertifiedMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This listener gets added by default to every new MessageServer as a MessageListener
 * and ConnectionListener and handles internal event handling. Removing the InternalListener
 * from a MessageServer is not a good idea unless you have replicated all functionality
 * contained within this listener.
 * <p/>
 * Currently only used for handling 'System-level' messages on messageReceived and messageSent
 * <p/>
 * This is a singleton object.
 *
 * @author Matthew D. Hicks
 */
class InternalListener implements MessageListener, ConnectionListener {

	private static InternalListener instance;
	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.InternalListener");

	private InternalListener() { // I'm a singleton
	}
	
	public static final InternalListener getInstance() {
		if (instance == null) {
			instance = new InternalListener();
		}
		return instance;
	}

	/**
	 * handles all 'System' messages received:
	 * -- LocalRegistrationMessage:
	 *         First checks if a filter condition exists in MessageServer for the given
	 *         MessageClient. If true the connection will be kicked.
	 * <p/>
	 *         Else extracts the (user)message id's and names as used on the REMOTE
	 *         side and stores them into local Registry of MessageClient (they will differ
	 *         from those used on THIS side of the connection).
	 * <p/>
	 *         if this MessageClient hasn't sent a LRM, (which means, the negotiation startet
	 *         from the other side) the ConnectionController of the MessageServer of the
	 *         current MessageClient will be asked to handle negotiation.
	 * <p/>
	 * -- DisconnectMessage:
	 *         Sets the associated MessageClient to DISCONNECTING. That state will eventually
	 *         be picked up by the MessageServer and transitet into DISCONNECTED. Kickreason and
	 *         CloseReason are set into MessageClient, before
	 * <p/>
	 * -- Message instanceof CertifiedMessage:
	 *         Send back a Receipt with the Receipt-Id set to the Id of the received Message.
	 * <p/>
	 * -- Receipt:
	 *         calls MessageClient.certify() to move the certified message to the CertifiedQueue
	 *         for event handling
	 *
	 * @param message
	 */
	@SuppressWarnings({"unchecked"})
	public void messageReceived(Message message) {
		MessageClient myClient = message.getMessageClient();

		if (message instanceof LocalRegistrationMessage) {
			// Verify if connection is valid
			String filterMessage = myClient.getMessageServer().shouldFilterConnection(myClient);
			if (filterMessage != null) {
				// They have been filtered, so we kick them
				myClient.kick(filterMessage);
				return;
			}

			// Handle incoming negotiation information
			// this message holds the Message NAMES and their MessageIds
			// as they are used on the other side of the 'wire'. We have to store
			// them within the MC, because they will differ from mine in JGN.java!
			LocalRegistrationMessage m = (LocalRegistrationMessage) message;
			myClient.setId(m.getId());
			LOG.finest("LRM recvd; setting ClientId @" + Integer.toHexString(myClient.hashCode()) + " to: " + m.getId());
			String[] messages = m.getRegisteredClasses();
			short[] ids = m.getIds();

			int i = 0;
			try {
				for (; i < messages.length; i++) {
					myClient.register(ids[i], Class.forName(messages[i]));
				}
				// transfer ok, put client into CONNECTED state, and prepare to notify ConnectionListeners
				myClient.setStatus(MessageClient.Status.CONNECTED);
				myClient.getMessageServer().getNegotiatedConnectionQueue().add(myClient);
			} catch (ClassNotFoundException exc) {
				// TODO: check if this shouldn't be disconnect-ING, was DISCONNECTED !!
				//myClient.setStatus(MessageClient.Status.DISCONNECTED);
				myClient.setStatus(MessageClient.Status.DISCONNECTING);
				myClient.setCloseReason(MessageClient.CloseReason.ErrMessageWrong);
				// no, could be an attack, don't stop complete system
				// throw new RuntimeException("Message "+messages[i]+" unknown!",exc);
				LOG.log(Level.WARNING, " unknown messagetype in LRM received for " + myClient, exc);
			}

			if (!myClient.hasSentRegistration()) {
				myClient.getMessageServer().getConnectionController().negotiate(myClient);
			}
		} else if (message instanceof TimeSynchronizationMessage) {
			TimeSynchronizationMessage m = (TimeSynchronizationMessage) message;
			if (m.getRemoteTime() != -1) {
				long actualTime = System.currentTimeMillis();
				long lag = (actualTime - m.getLocalTime()) / 2;
				if (actualTime >= m.getRemoteTime()
						&& m.getRemoteTime() >= m.getLocalTime())
					m.getMessageClient().setTimeConversion(0);
				else
					m.getMessageClient().setTimeConversion(
							(m.getRemoteTime() + lag - actualTime));
			}
			if (m.isResponseDesired()) {
				m.setRemoteTime(System.currentTimeMillis());
				m.setLocalTime(m.getLocalTime());
				m.setResponseDesired(false);
				m.getMessageClient().sendMessage(m);
			}
		} else if (message instanceof PingMessage) {
			PingMessage ping = (PingMessage)message;
			PongMessage pong = new PongMessage();
			pong.setSendTime(ping.getSendTime());
			pong.setPingId(ping.getId());
			message.getMessageClient().sendMessage(pong);
		} else if (message instanceof DisconnectMessage) {
			// Disconnect me from the remote client
			if ((myClient.getStatus() == MessageClient.Status.CONNECTED) || (myClient.getStatus() == MessageClient.Status.NEGOTIATING)) {
				myClient.setStatus(MessageClient.Status.DISCONNECTING);
				myClient.setKickReason(((DisconnectMessage) message).getReason());
				// this could be a reply to my own disconnect()
				if (myClient.getCloseReason() == MessageClient.CloseReason.NA)
					myClient.setCloseReason(MessageClient.CloseReason.ClosedByRemote);
			}
		}

		if (message instanceof CertifiedMessage) {
			// Send back a message to the sender to let them know the message was received
			Receipt receipt = new Receipt();
			receipt.setCertifiedId(message.getId());
			myClient.sendMessage(receipt);

		} else if (message instanceof Receipt) {
			// Received confirmation of a CertifiedMessage
			myClient.certifyMessage(((Receipt) message).getCertifiedId());
		}
	}

	/**
	 * checks if the message that was sent just now is a CertifiedMessage. If so, that
	 * message will be moved to the Certifiable Queue, where it will await confirmation.
	 *
	 * @param message
	 */
	public void messageSent(Message message) {
		if (message instanceof CertifiedMessage) {
			message.getMessageClient().getCertifiableMessageQueue().add(message);
		}
	}

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}

	public void connected(MessageClient client) {
	}

	public void negotiationComplete(MessageClient client) {
	}

	public void disconnected(MessageClient client) {
	}

	public void kicked(MessageClient client, String reason) {
	}
}
