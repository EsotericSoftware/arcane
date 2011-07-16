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
 * Created on 20-jun-2006
 */
package com.captiveimagination.jgn;

import com.captiveimagination.jgn.message.Message;

/**
 * used for problems with messages, when they are handled (hah!)
 * eg, when a serialized Message is really too big
 *     when the serialized format is corrupt
 *     when the messagetype is unknown
 *     when the userdefined message could not be instantiated
 *
 * note, there might be a chance that the offending message may be
 * retrieved from this exception.
 *
 * @author Matthew D. Hicks
 *
 */
public class MessageHandlingException extends Exception {
	private static final long serialVersionUID = 1L;
	private final Message failed;

	public MessageHandlingException(String msg) {
		this(msg, null, null);
	}

	public MessageHandlingException(String msg, Message failed) {
		this(msg, failed, null);
	}

	public MessageHandlingException(String msg, Message failed, Throwable cause) {
		super(msg, cause);
		this.failed = failed;
	}

	public final Message getFailedMessage() {
		return failed;
	}
}
