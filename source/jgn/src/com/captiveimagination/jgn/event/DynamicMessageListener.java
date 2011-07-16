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
 * Created: Jun 12, 2006
 */
package com.captiveimagination.jgn.event;

import com.captiveimagination.jgn.message.Message;

/**
 * A listener implementing DynamicMessageListener can utilize dynamic introspection
 * features built into JGN to determine the closest matching method to the Message
 * that has been received.
 * <p/>
 * For example, if you have two methods in your implementing class:
 * 		messageReceived(Message message)
 * and
 * 		messageReceived(MyMessage message)
 * if you receive a <code>MyMessage</code<> it will be directly routed to the second method instead
 * of the first. However, if you receive a <code>YourMessage</code> (and it does not extend
 * <code>MyMessage</code>) it will be routed to the first method.
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public interface DynamicMessageListener extends MessageListener {
	/**
	 * call the handler
	 *
	 * @param type		 MessageType: Received, Certified, Sent, Failed
	 * @param mess		 the message to dispatch
	 * @param listener at the listener as registered on MessageServer
	 */
	void handle(MessageListener.MESSAGE_EVENT type, Message mess, DynamicMessageListener listener);
}
