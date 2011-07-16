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
 * Created: Aug 18, 2006
 */
package com.captiveimagination.jgn.so;

import com.captiveimagination.jgn.*;

/**
 * Implementations of this interface are used to monitor creation, changes, and removals to
 * the shared object system.
 * 
 * @author Matthew D. Hicks
 */
public interface SharedObjectListener {
	/**
	 * An object has been created either remotely or locally. If it was created
	 * remotely <code>client</code> will be a reference to the remote machine that
	 * created it. If it was created locally <code>client</code> will be null.
	 * 
	 * @param name
	 * @param object
	 * @param client
	 */
	public void created(String name, Object object, MessageClient client);
	
	/**
	 * An object has been changed either remotely or locally. If the change was
	 * made remotely <code>client</code> will be a reference to the remote machine
	 * that made the change. If it was created locally <code>client</code> will be
	 * null.
	 * 
	 * @param name
	 * @param object
	 * @param field
	 * @param client
	 */
	public void changed(String name, Object object, String field, MessageClient client);
	
	/**
	 * An object has been removed either remotely or locally. If the removal was
	 * made remotely <code>client</code> will be a reference to the remote machine
	 * that removed it. If it was removed locally <code>client</code> will be null.
	 * 
	 * @param name
	 * @param object
	 * @param client
	 */
	public void removed(String name, Object object, MessageClient client);
}
