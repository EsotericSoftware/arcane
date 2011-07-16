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
 * Created: Jul 20, 2006
 */
package com.captiveimagination.jgn.clientserver;

import com.captiveimagination.jgn.DefaultConnectionController;
import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.message.LocalRegistrationMessage;

/**
 * @author Matthew D. Hicks
 */
public class ClientServerConnectionController extends DefaultConnectionController {
	private JGNClient client;

	public ClientServerConnectionController(JGNClient client) {
		this.client = client;
	}

	/**
	 * overrides the DefaultConnectionController.negotiate() insofar, as
	 * the id of the negotiating LocalRegistrationMessage is not set to the
	 * messageServerId, but to the id of the corresponding JGNClient!!!
	 * This makes it possible for the receiving JGNServer to bundle both TCP and UDP
	 * 'physical' clients as a reference back to this JGNClient. See JGNServer
	 *
	 * @param client
	 */
	@Override
	public void negotiate(MessageClient client) {
		LocalRegistrationMessage message = new LocalRegistrationMessage();
		JGN.populateRegistrationMessage(message);
		message.setId(this.client.getId());
		client.sendMessage(message);
	}
}
