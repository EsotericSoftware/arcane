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
 * Created: october 20, 2008
 */
package com.captiveimagination.jgn.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

import com.captiveimagination.jgn.message.StreamMessage;

/**
 * @author Kine(Bolidz)
 */

public class JGNFileReceiver {

	private JGNFileWrapper wrapper;
	private short streamId;
	private boolean finished = false;
	private boolean closed = false;

	private int numberOfMessageReceived = 0;

	private File tempFile;
	private FileOutputStream ostream;

	public JGNFileReceiver(JGNFileWrapper wrapper, short streamId) {
		this.wrapper = wrapper;
		this.streamId = streamId;

	}

	public short getStreamId() {
		return streamId;
	}

	public void compute(StreamMessage message) {
		if (numberOfMessageReceived == 0) {

			try {
				tempFile = new File("FileWrapperTemp"
						+ Integer.toString(streamId));
				wrapper.getFileReceiverTempFiles().put(streamId, tempFile);

				ostream = new FileOutputStream(tempFile);
			} catch (FileNotFoundException e) {
				FileWrapperException fWE = new FileWrapperException(
						"file Wrapper(receiver) : problem creating the temporary file");
				wrapper.getLOG().log(Level.WARNING, "", fWE);
			}
		}

		if (message.getDataLength() != -1) {
			try {
				ostream.write(message.getData());
				numberOfMessageReceived++;
			} catch (IOException e) {
				FileWrapperException fWE = new FileWrapperException(
						"file Wrapper(receiver) : problem receiving the stream...");
				wrapper.getLOG().log(Level.WARNING, "", fWE);
			}
		} else {
			finished = true;
		}

	}

	public File getTempFile() {
		return tempFile;
	}

	public void setTempFile(File tempFile) {
		this.tempFile = tempFile;
	}

	public void close() throws IOException {
		finished = true;

		ostream.close();

	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

}
