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
 * Created: /**
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.StreamMessage;

/**
 * @author Kine(Bolidz)
 */

public class JGNFileWrapper implements MessageListener {

	private static Logger LOG = Logger
			.getLogger("com.captiveimagination.jgn.file.FileWrapper");

	MessageServer messageServer;

	private HashMap<Short, JGNFileSender> fileSenders = new HashMap<Short, JGNFileSender>();
	private HashMap<Short, JGNFileReceiver> fileReceivers = new HashMap<Short, JGNFileReceiver>();

	private HashMap<Short, File> fileReceiverTempFiles = new HashMap<Short, File>();
	private Random randomizer;

	private ArrayList<MessageListener> messageListener = new ArrayList<MessageListener>();

	public JGNFileWrapper(MessageServer messageServer) {
		this.messageServer = messageServer;
		messageServer.addMessageListener(this);
		randomizer = new Random();
	}

	public void broadcastFileMessage(FileMessage fileMessage) {
		JGNFileSender jGNFileSender;

		try {
			short StreamId = (short) randomizer.nextInt(32000);
			jGNFileSender = getFileSender(StreamId);

			FileInputStream fis = new FileInputStream(fileMessage.getFile());

			byte[] buffer = new byte[512];
			int len;

			while ((len = fis.read(buffer)) != -1) {
				jGNFileSender.write(buffer, 0, len);
			}
			jGNFileSender.flush();
			jGNFileSender.close();
			fis.close();

			fileMessage.setStreamId(StreamId);
			fileMessage.setFile(null);

			messageServer.broadcast(fileMessage);
			for (MessageListener mL : messageListener) {
				mL.messageSent(fileMessage);
			}

		} catch (IOException e) {
			FileWrapperException fWE = new FileWrapperException(
					"file Wrapper : problem sending the datas");
			LOG.log(Level.WARNING, "", fWE);
		}

	}

	public void broadcastFile(File file, String fileName, String path) {
		JGNFileSender jGNFileSender;
		try {
			short StreamId = (short) randomizer.nextInt(32000);

			jGNFileSender = getFileSender(StreamId);

			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[512];
			int len;
			while ((len = fis.read(buffer)) != -1) {
				jGNFileSender.write(buffer, 0, len);
			}
			jGNFileSender.flush();
			jGNFileSender.close();
			fis.close();

			FileMessage fileMessage = new FileMessage();
			fileMessage.setFileName(fileName);
			fileMessage.setPath(path);
			fileMessage.setStreamId(StreamId);

			messageServer.broadcast(fileMessage);
			for (MessageListener mL : messageListener) {
				mL.messageSent(fileMessage);
			}

		} catch (IOException e) {
			FileWrapperException fWE = new FileWrapperException(
					"file Wrapper : problem sending the datas, probably file don't exists");
			LOG.log(Level.WARNING, "", fWE);
		}

	}

	public void addMessageListener(MessageListener messageListener) {
		this.messageListener.add(messageListener);
	}

	public void removeMessageListener(MessageListener messageListener) {
		if (this.messageListener.contains(messageListener))
			this.messageListener.remove(messageListener);
	}

	public HashMap<Short, File> getFileReceiverTempFiles() {
		return fileReceiverTempFiles;
	}

	public void setFileReceiverTempFiles(
			HashMap<Short, File> fileReceiverTempFiles) {
		this.fileReceiverTempFiles = fileReceiverTempFiles;
	}

	public void messageReceived(Message message) {
		if (message instanceof StreamMessage) {

			try {
				getFileReceiver(((StreamMessage) message).getStreamId())
						.compute((StreamMessage) message);
			} catch (IOException e) {
				FileWrapperException fWE = new FileWrapperException(
						"file Wrapper : problem rceiving a data message");
				LOG.log(Level.WARNING, "", fWE);
			}

		}

		if (message instanceof FileMessage) {

			try {

				FileMessage m = (FileMessage) message;
				long time = System.currentTimeMillis();
				long delay = 0;
				while (!getFileReceiver(m.getStreamId()).isFinished()
						&& delay < 5000) {
					try {
						Thread.sleep(5);
						delay = System.currentTimeMillis() - time;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (getFileReceiver(m.getStreamId()).isFinished())

				{
					m.setFile(getFileReceiver(m.getStreamId()).getTempFile());

					closeFileReceiver(m.getStreamId());

					for (MessageListener mL : messageListener) {
						mL.messageReceived(m);
					}

				} else {

					FileWrapperException fWE = new FileWrapperException(
							"file Wrapper : file net received in time.problem sending the datas");
					LOG.log(Level.WARNING, "", fWE);

					closeFileReceiver(m.getStreamId());
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void flush() {

		for (short streamId : fileReceivers.keySet()) {
			try {

				if (getFileReceiver(streamId).isFinished())
					fileReceiverTempFiles.get(streamId).delete();
			} catch (IOException e) {
				FileWrapperException fWE = new FileWrapperException(
						"file Wrapper : problem flushing the files");
				LOG.log(Level.WARNING, "", fWE);

			}
		}

	}

	public void close() {
		messageServer.removeMessageListener(this);

		for (short streamId : fileReceivers.keySet()) {
			try {

				closeFileReceiver(streamId);
				if (fileReceiverTempFiles.containsKey(streamId))
					fileReceiverTempFiles.get(streamId).delete();

			} catch (IOException e) {
				FileWrapperException fWE = new FileWrapperException(
						"file Wrapper : problem flushing the files");
				LOG.log(Level.WARNING, "", fWE);

			}

		}

	}

	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	public void messageSent(Message message) {
	}

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}

	public JGNFileSender getFileSender() throws IOException {
		return getFileSender((short) 0);
	}

	public JGNFileSender getFileSender(short streamId) throws IOException {
		if (fileSenders.containsKey(streamId)) {
			FileWrapperException fWE = new FileWrapperException(
					"file Wrapper : the canal for sending is already in use !!!");
			LOG.log(Level.WARNING, "", fWE);
			throw fWE;
		}
		JGNFileSender fileSender = new JGNFileSender(this, streamId);
		fileSenders.put(streamId, fileSender);
		return fileSender;
	}

	public void closeFileSender(short streamId) throws IOException {
		if (fileSenders.containsKey(streamId)) {
			if (!fileSenders.get(streamId).isClosed())
				fileSenders.get(streamId).close();
			fileSenders.remove(streamId);
		}
	}

	public JGNFileReceiver getFileReceiver() throws IOException {
		return getFileReceiver((short) 0);
	}

	public JGNFileReceiver getFileReceiver(short streamId) throws IOException {
		if (fileReceivers.containsKey(streamId)) {
			return fileReceivers.get(streamId);
		} else {

			JGNFileReceiver fileReceiver = new JGNFileReceiver(this, streamId);
			fileReceivers.put(streamId, fileReceiver);
			return fileReceiver;
		}
	}

	public void closeFileReceiver(short streamId) throws IOException {
		if (fileReceivers.containsKey(streamId)) {
			if (!fileReceivers.get(streamId).isClosed())
				fileReceivers.get(streamId).close();
			fileReceivers.remove(streamId);
		}
	}

	public MessageServer getMessageServer() {
		return messageServer;
	}

	public static Logger getLOG() {
		return LOG;
	}

	public static void setLOG(Logger log) {
		LOG = log;
	}

}