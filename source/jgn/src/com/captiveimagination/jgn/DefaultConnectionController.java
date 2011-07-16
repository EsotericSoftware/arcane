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
 * Created: Jul 14, 2006
 */
package com.captiveimagination.jgn;

import com.captiveimagination.jgn.message.DisconnectMessage;
import com.captiveimagination.jgn.message.LocalRegistrationMessage;

/**
 * the simplest ConnectionController possible
 *
 * @author Matthew D. Hicks
 */
public class DefaultConnectionController implements ConnectionController {

	/**
	 * docu copied from ConnectionController
	 * <p/>
	 * This method is called when a connection is successfully established and is ready to
	 * send a negotiation message to the remote server.
	 */
	public void negotiate(MessageClient client) {
		LocalRegistrationMessage message = new LocalRegistrationMessage();
		message.setId(client.getMessageServer().getMessageServerId());
		JGN.populateRegistrationMessage(message);
		client.sendMessage(message);
	}

	/**
 * docu copied from ConnectionController
	 * <p/>
 * This method is invoked when a MessageClient is manually told to disconnect. This method
 * is responsible for notifying the remote server of the disconnection.
 */
	public void disconnect(MessageClient client) {
		if (! (client.getStatus() == MessageClient.Status.DISCONNECTED))
			client.sendMessage(new DisconnectMessage());
	}


 /**
	* docu copied from ConnectionController
	 * <p/>
	*	This method is invoked when a MessageClient is manually kicked. This method is responsible
	* for notifying the remote server of the kick and disconnection.
  */
	public void kick(MessageClient client, String reason) {
		client.sendMessage(new DisconnectMessage(reason));
	}
}