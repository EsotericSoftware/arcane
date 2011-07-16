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
 * Created: Jun 7, 2006
 */
package com.captiveimagination.jgn;

import static com.captiveimagination.jgn.MessageClient.CloseReason;
import static com.captiveimagination.jgn.MessageClient.Status;
import com.captiveimagination.jgn.message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public final class TCPMessageServer extends NIOMessageServer {
	public TCPMessageServer(SocketAddress address) throws IOException {
		this(address, 1024);
	}

	public TCPMessageServer(SocketAddress address, int maxQueueSize)
			throws IOException {
		super(address, maxQueueSize);
		log.log(Level.INFO, " create TCPMessageServer (id={0}){1}, queuesize= {2}",
				new Object[] { serverId, address != null ? " at "+address : "", maxQueueSize });
		setServerType(ServerType.TCP);
	}

	protected SelectableChannel bindServer(SocketAddress address)
			throws IOException {
		ServerSocketChannel channel = selector.provider()
				.openServerSocketChannel();
		channel.socket().bind(address);
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_ACCEPT);
		return channel;
	}

	protected void accept(SelectableChannel channel) throws IOException {
		SocketChannel connection = ((ServerSocketChannel) channel).accept();
		InetSocketAddress remoteAdr = (InetSocketAddress) connection.socket()
				.getRemoteSocketAddress();
		// blacklist handling
		if (blacklist != null) {
			String newHost = remoteAdr.getAddress().getHostAddress();
			if (blacklist.contains(newHost)) {
				try {
					connection.close();
				} catch (IOException e) {
					// ok
				}
				log.log(Level.WARNING,
						"TCP-Srv (id={0}) rejecting access for host: {1}",
						new Object[] { serverId, remoteAdr });
				return;
			}
		}
		connection.configureBlocking(false);
		connection.socket().setTcpNoDelay(true);
		SelectionKey key = connection.register(selector, SelectionKey.OP_READ
				| SelectionKey.OP_WRITE);
		MessageClient client = new MessageClient(remoteAdr, this);
		log.log(Level.FINEST, " accept client {0} from {1}", new Object[] {
				client, remoteAdr });
		client.setStatus(Status.NEGOTIATING);
		key.attach(client);
		getIncomingConnectionQueue().add(client);
		getMessageClients().add(client);
	}

	protected void read(SelectableChannel channel) {
		MessageClient client = (MessageClient) channel.keyFor(selector)
				.attachment();
		if (client == null)
			return; // have seen happening for short intervals around connect
		try {
			int hasRead = ((SocketChannel) channel)
					.read(client.getReadBuffer());
			if (hasRead == 0)
				return; // spourious wakeup --> can happen

			if (hasRead == -1) {
				client.setCloseReason(CloseReason.ErrChannelClosed);
				collectTrafficProblem(client);
				return;
			}
		} catch (IOException e) {
			client.setCloseReason(CloseReason.ErrChannelRead);
			collectTrafficProblem(client);
			log.log(Level.FINER, " channel.read exception", e);
			return;
		}

		Message message;
		try {
			while ((message = readMessage(client)) != null) {
				client.receiveMessage(message);
			}
		} catch (MessageHandlingException exc) {
			client.setCloseReason(CloseReason.ErrMessageWrong);
			collectTrafficProblem(client);
			log.log(Level.FINER, " channel.read: message corrupt");
			// throw new RuntimeException("..."); // TODO. is this ok ???
		}
	}

	protected boolean write(SelectableChannel channel) {
		SelectionKey key = channel.keyFor(selector);
		MessageClient client = (MessageClient) key.attachment();
		if (client == null)
			return false; // have seen happening for short intervals around
							// connect

		CombinedPacket clientCurWrite = client.getCurrentWrite();
		if (clientCurWrite != null) {
			client.sent(); // Let the system know something has been written
			try {
				((SocketChannel) channel).write(clientCurWrite.getBuffer());
			} catch (IOException e) {
				client.setCloseReason(CloseReason.ErrChannelWrite);
				// remember this client for error processing after updateTraffic
				collectTrafficProblem(client);
				log.log(Level.FINER, " channel.write exception", e);
				return false; // don't try again
			}
			if (!clientCurWrite.getBuffer().hasRemaining()) {
				// Write all messages in combined to sent queue
				clientCurWrite.process();

				client.setCurrentWrite(null);
			} else {
				// Take completed messages and add them to the sent queue
				clientCurWrite.process();
			}
		} else {
			CombinedPacket combined;
			try {
				combined = PacketCombiner.combine(client);
			} catch (MessageHandlingException exc) {
				exc.printStackTrace();
				client.setCloseReason(CloseReason.ErrMessageWrong);
				// remember this client for error processing after updateTraffic
				collectTrafficProblem(client);
				combined = null;
			}

			if (combined != null) {
				try {
					((SocketChannel) channel).write(combined.getBuffer());
				} catch (IOException e) {
					client.setCloseReason(CloseReason.ErrChannelWrite);
					// remember this client for error processing after
					// updateTraffic
					collectTrafficProblem(client);
					return false; // don't try again
				}
				if (combined.getBuffer().hasRemaining()) {
					client.setCurrentWrite(combined);

					// Take completed messages and add them to the sent queue
					combined.process();
				} else {
					client.setCurrentWrite(null);

					// Write all messages in combined to sent queue
					combined.process();

					return true;
				}
				/*
				 * ase: check this: if following was only intended to transit
				 * the state from Disconnecting to Disconnected the following
				 * omission is ok. That transit is now in updateConnections...
				 * otherwise, we'll have to find out what to do
				 */
				// } else if (client.isDisconnectable()) {
				// disconnectInternal(client, true);
			}
		}
		return false;
	}

	protected void connect(SelectableChannel channel) throws IOException {
		if (((SocketChannel) channel).finishConnect()) {
			MessageClient client = (MessageClient) channel.keyFor(selector)
					.attachment();
			if (client != null
					&& client.getStatus() == MessageClient.Status.NEGOTIATING) {
				getIncomingConnectionQueue().add(client);
				getConnectionController().negotiate(client);
			}
		}
	}

	public MessageClient connect(SocketAddress address) {
		MessageClient client = getMessageClient(address);
		if ((client != null) && (client.getStatus() != Status.DISCONNECTING)
				&& (client.getStatus() != Status.DISCONNECTED)) {
			log.log(Level.FINEST, "return existing client: {0} for: {1}",
					new Object[] { client, address });
			return client; // Client already connected, simply return it
		}
		SelectionKey key = null;
		SocketChannel channel = null;

		try {
			client = new MessageClient(address, this);
			client.setStatus(MessageClient.Status.NEGOTIATING);
			getMessageClients().add(client);
			channel = selector.provider().openSocketChannel();
			channel.socket().setTcpNoDelay(true);
			channel.configureBlocking(false);
			key = channel.register(selector, SelectionKey.OP_CONNECT
					| SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			key.attach(client);
			channel.connect(address);
			log.log(Level.FINEST, "finishing connect; client: {0} for: {1}",
					new Object[] { client, address });
			return client;
		} catch (IOException e) {
			log.log(Level.WARNING, "couldn't connect to {0}", address);
			log.log(Level.WARNING, " -- reason:", e);
			try {
				if (channel != null)
					channel.close();
			} catch (IOException e1) { // ah, bad luck::
			}
			if (key != null)
				key.cancel();
			return null;
		}
	}
}
