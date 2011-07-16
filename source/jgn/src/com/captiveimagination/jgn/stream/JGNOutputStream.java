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
 * Created: Jun 19, 2006
 */
package com.captiveimagination.jgn.stream;

import java.io.IOException;
import java.io.OutputStream;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.message.StreamMessage;
import com.captiveimagination.jgn.queue.*;

/**
 * @author Matthew D. Hicks
 */
public class JGNOutputStream extends OutputStream {
	private MessageClient client;
	private short streamId;
	private byte[] buffer;
	private int position;
	private StreamMessage message;
	private boolean streamClosed;

	private int bufferSize = 512;

	public JGNOutputStream(MessageClient client, short streamId)
			throws IOException {
		this.client = client;
		this.streamId = streamId;
		message = new StreamMessage();
		message.setStreamId(streamId);
		setBufferSize(bufferSize);
	}

	public MessageClient getMessageClient() {
		return client;
	}

	public short getStreamId() {
		return streamId;
	}

	public void write(int b) throws IOException {
		if (streamClosed)
			throw new IOException("This stream has been closed already ("
					+ streamId + ").");
		if (position >= buffer.length) {
			flush();
			buffer = new byte[bufferSize];
			message.setData(buffer);
		}
		buffer[position++] = (byte) b;
	}

	public void setBufferSize(int length) throws IOException {
		flush();
		buffer = new byte[length];
		message.setData(buffer);
	}

	public void flush() throws IOException {
		if (position > 0) {
			
			if (position < 512) {
				byte[] tempBuffer = new byte[position];
				for (int i = 0; i < position; i++) {
					tempBuffer[i] = buffer[i];
				}

				message.setData(tempBuffer);
			}
			message.setDataLength(position);
			sendMessage();
			position = 0;
		}
	}

	public void close() throws IOException {
		flush();
		message.setData(null);
		message.setDataLength(-1);
		sendMessage();
		streamClosed = true;
		client.closeOutputStream(streamId);
	}

	public boolean isClosed() {
		return streamClosed;
	}

	private void sendMessage() {
		boolean keepTrying = true;
		while (keepTrying) {
			try {
				client.sendMessage(message);
				keepTrying = false;
			} catch (QueueFullException exc) {
				// do nothing *now*
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException exc) {
				// ok
			}
		}
	}
}
