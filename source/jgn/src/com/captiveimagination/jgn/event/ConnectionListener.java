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
package com.captiveimagination.jgn.event;

import com.captiveimagination.jgn.*;

/**
 * ConnectionListener implementations can be added to any MessageServer to be notified when a
 * connection has been established or lost.
 * 
 * @author Matthew D. Hicks
 */
public interface ConnectionListener {
	/**
	 * This method is invoked when a connection has
	 * been successfully established with a MessageClient
	 * 
	 * @param client
	 */
	public void connected(MessageClient client);
	
	/**
	 * This method is invoked in circumstances when your
	 * connection has either been rejected at initial
	 * connection or was forcibly removed after communication
	 * was successfully established.
	 * 
	 * @param client
	 * @param reason
	 */
	public void kicked(MessageClient client, String reason);
	
	/**
	 * This method is invoked when a connection has
	 * been successfully established and the negotiation
	 * process has completed successfully.
	 * 
	 * @param client
	 */
	public void negotiationComplete(MessageClient client);
	
	/**
	 * This method is invoked when a connection has
	 * been disconnected either gracefully or via
	 * timeout.
	 * 
	 * @param client
	 */
	public void disconnected(MessageClient client);
}
