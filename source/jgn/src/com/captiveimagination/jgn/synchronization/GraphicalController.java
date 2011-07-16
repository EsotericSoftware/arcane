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
 * Created: Jul 27, 2006
 */
package com.captiveimagination.jgn.synchronization;

import com.captiveimagination.jgn.synchronization.message.*;

/**
 * GraphicalController provides the link between JGN's synchronization and
 * the graphical engine being utilized.
 * 
 * @author Matthew D. Hicks
 */
public interface GraphicalController<T> {
	/**
	 * The implementation of this method should return a value between
	 * 0.0f and 1.0f signifying the proximity. If proximity is 1.0f the
	 * speed at which updates occur will be standard. As the proximity
	 * declines towards 0.0f the updates are less frequent (further away
	 * objects need to be updated less often). If the value returned is
	 * 0.0f no updates will be sent as it is determined to be outside of
	 * the proximity range of this player.
	 * 
	 * @param object
	 * @param playerId
	 * @return
	 * 		float
	 */
	public float proximity(T object, short playerId);
	
	/**
	 * This method is responsible for generating a synchronization message
	 * based on the information contained in <code>object</code>.
	 * 
	 * @param object
	 * @return
	 * 		RealtimeMessage
	 */
	public SynchronizeMessage createSynchronizationMessage(T object);
	
	/**
	 * After a synchronization message has been properly received this method
	 * is invoked to apply the synchronization information to the scene.
	 * 
	 * @param message
	 * @param object
	 */
	public void applySynchronizationMessage(SynchronizeMessage message, T object);

	/**
	 * This method is called in order to validate messages that are received
	 * before they are applied to the scene.
	 * 
	 * @param message
	 * @param object
	 * @return
	 * 		boolean
	 */
	public boolean validateMessage(SynchronizeMessage message, T object);

	/**
	 * This method is called in order to validate a creation message received
	 * before applied to the scene.
	 * 
	 * @param message
	 * @return
	 * 		boolean
	 */
	public boolean validateCreate(SynchronizeCreateMessage message);
	
	/**
	 * This method is called in order to validate a removal message received
	 * before applied to the scene.
	 * 
	 * @param message
	 * @return
	 * 		boolean
	 */
	public boolean validateRemove(SynchronizeRemoveMessage message);
}