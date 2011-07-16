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
 * Created: Jul 14, 2006
 */
package com.captiveimagination.jgn.clientserver;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.CertifiedMessage;

import java.io.IOException;

/**
 * A JGNDirectConnection binds together the 2 possible MessageClients used for
 * TCP/UDP and the PlayerID.
 * It knows how to disconnect from those MessageClients as well as how to send
 * messages to the server: if there is only one protocol installed, it uses that (aha!)
 * otherwise it prefers to send messages via UDP, but sends CertifiedMessages via
 * reliable TCP. BTW, messages are not forced to be PlayerMessages...
 * <p/>
 * In contrast to JGNRelayConnections, messages are sent directly via corresponding MC,
 * therefore you'll find this type of connection at JGNServer, and as the connection
 * from JGNClient to JGNServer.
 * <p/>
 * see JGNRelayConnection for routing 'client-to-client' via JGNServer.
 *
 * @author Matthew D. Hicks
 */
public class JGNDirectConnection implements JGNConnection {
	private short playerId;
	private MessageClient reliableClient;
	private MessageClient fastClient;

	public JGNDirectConnection() {
		playerId = -1;
	}

	public void setPlayerId(short playerId) {
		this.playerId = playerId;
	}

	public short getPlayerId() {
		return playerId;
	}

	public MessageClient getFastClient() {
		return fastClient;
	}

	public void setFastClient(MessageClient fastClient) {
		this.fastClient = fastClient;
	}

	public MessageClient getReliableClient() {
		return reliableClient;
	}

	public void setReliableClient(MessageClient reliableClient) {
		this.reliableClient = reliableClient;
	}

	public boolean isConnected() {
		if ((reliableClient != null) && (!reliableClient.isConnected())) {
			return false;
		} else if ((fastClient != null) && (!fastClient.isConnected())) {
			return false;
		} else if ((reliableClient == null) && (fastClient == null)) {
			return false;
		}
		return true;
	}

	public void disconnect() throws IOException {
		if (reliableClient != null) {
			reliableClient.disconnect();
		}
		if (fastClient != null) {
			fastClient.disconnect();
		}
	}

	public long sendMessage(Message message) {
		if (message.getPlayerId() == -1) {
			message.setPlayerId(this.playerId);
		}
		if ((message instanceof CertifiedMessage) && (reliableClient != null)) {
			return reliableClient.sendMessage(message);
		} else if (fastClient != null) {
			return fastClient.sendMessage(message);
		} else if (reliableClient != null) {
			return reliableClient.sendMessage(message);
		}
		return -1;
	}
}
