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

import com.captiveimagination.jgn.message.Message;

/**
 * A RelayConnection simply holds the playerId of another player.
 * Each time JGNClient wants to send a PlayerMessage to that player,
 * this object will route the message to the server, and only the server
 * knows of the correct connection to the other player.
 * <p/>
 * Note there is no restriction on the type of message imposed by this object,
 * though sending any message that isn't a PlayerMessage will result in loosing
 * the destinationPlayerId as well as the playerID (when de/serializing the message),
 * which makes it impossible for the server to route the message as desired ...
 *
 * @author Matthew D. Hicks
 */
public class JGNRelayConnection implements JGNConnection {
	private JGNClient client; // this is my owner; need this for call back
	private short playerId;
	
	public JGNRelayConnection(JGNClient client, short playerId) {
		this.client = client;
		this.playerId = playerId;
	}
	
	public short getPlayerId() {
		return playerId;
	}
	
	// send the message to the server, after setting the destinationplayer into the message
	public long sendMessage(Message message) {
		message.setDestinationPlayerId(playerId);
		return client.getServerConnection().sendMessage(message);
	}
	
	// this concerns connection to the JGNServer
	public boolean isConnected() {
		return client.getServerConnection().isConnected();
	}
}
