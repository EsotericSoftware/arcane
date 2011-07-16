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
 * Created: Jul 6, 2006
 */
package com.captiveimagination.jgn;

import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.convert.Converter;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.translation.TranslatedMessage;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * the workhorse for the non blocking servertype.
 * <p/>
 * provides important functions:
 * disconnectInternal : close a channel, cleanup a MessageClient
 * updateTraffic: cycle through all available selected keys (NIO) and dispatch to
 *                methods defined in subclasses (TCP/UDPMessageServer)
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public abstract class NIOMessageServer extends MessageServer {
	protected Selector selector;
	private ArrayList<MessageClient> problems;

	public NIOMessageServer(SocketAddress address, int maxQueueSize) throws IOException {
		super(address, maxQueueSize);
		selector = Selector.open();
		if (address != null)
			bindServer(address);
		problems = new ArrayList<MessageClient>();
	}

	protected abstract SelectableChannel bindServer(SocketAddress address) throws IOException;

	protected abstract void accept(SelectableChannel channel) throws IOException;

	protected abstract void connect(SelectableChannel channel) throws IOException;

	protected abstract void read(SelectableChannel channel) throws IOException;

	protected abstract boolean write(SelectableChannel channel) throws IOException;

	protected void disconnectInternal(MessageClient client, MessageClient.CloseReason reason) {
		String rsn = reason.toString();
		log.log(Level.FINEST, " disconnecting internally client {0} because {1}", new Object[]{client, rsn});
		// close NIO's channel, remove key
		for (SelectionKey key : selector.keys()) {
			if (key.attachment() == client) {
				try {
					key.channel().close();
				} catch (IOException e) { // ah, bad luck
				}
				key.cancel();
				break;
			}
		}

		// Parse through all the certified messages unsent
		// push them to failed
		Message message;
		while ((message = client.getCertifiableMessageQueue().poll()) != null) {
			client.getFailedMessageQueue().add(message);
		}

		// Execute events to invoke any events left for this client's messages
		notifyClient(client);
		
		client.setStatus(MessageClient.Status.DISCONNECTED);
		if (reason != MessageClient.CloseReason.Ignore)
			client.setCloseReason(reason);
		clients.remove(client);
		getDisconnectedConnectionQueue().add(client);
	}

	public synchronized void updateTraffic() throws IOException {
		// Ignore if no longer alive
		if (!isAlive()) return;

		// If should be shutting down, check
		if ((getMessageClients().size() == 0) && (!keepAlive)) {
			for (SelectionKey key : selector.keys()) {
				key.channel().close();
				key.cancel();
			}
			selector.close();
			log.log(Level.INFO, "shutting down server id=" + serverId);
			alive = false;
			return;
		}

		// Handle Accept, Read, and Write
		if (selector.selectNow() > 0) {
			problems.clear(); // we'll have no problems at start

			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey activeKey = keys.next();
				keys.remove();
				if ((activeKey.isValid()) && (activeKey.isAcceptable())) {
					accept(activeKey.channel());
				}
				if ((activeKey.isValid()) && (activeKey.isReadable())) {
					read(activeKey.channel());
				}
				if ((activeKey.isValid()) && (activeKey.isWritable())) {
					while (write(activeKey.channel())) {
					}
				}
				if ((activeKey.isValid()) && (activeKey.isConnectable())) {
          try {
            connect(activeKey.channel());
          } catch (IOException e) {
//            System.err.println("  ** connect(channel) error:");
//            e.printStackTrace();
						log.log(Level.FINER, " error in connect(channel):", e);
						Object attchmnt = activeKey.attachment();
						if ((attchmnt != null) && (attchmnt instanceof MessageClient)) {
							MessageClient mc = (MessageClient) attchmnt;
							mc.setCloseReason(MessageClient.CloseReason.ErrChannelConnect);
              collectTrafficProblem(mc);
            }
          }
        }
			}
			// handle all cases where one of the above handler methods reported an error
			for (MessageClient client : problems) {
				log.log(Level.FINEST, " -- problems with {0}", client);
        disconnectInternal(client, client.getCloseReason());
			}
		}
	}

	// collect clients that encounter problems during updateTraffic
	protected void collectTrafficProblem(MessageClient client) {
		problems.add(client);
	}

	// transform client's received byte array into a message
	protected Message readMessage(MessageClient client) throws MessageHandlingException {
		client.received();
		ByteBuffer clientBuffer = client.getReadBuffer();
		int position = clientBuffer.position();
		clientBuffer.position(client.getReadPosition());
		int messageLength = clientBuffer.getInt();
//		System.out.println("***** There is a message to read: " + messageLength + " received data: " + (position - 4 - client.getReadPosition()));
		if (messageLength <= position - 4 - client.getReadPosition()) {
			// Read message
			Message message;
			try {
				message = (Message)Converter.readClassAndObject(clientBuffer);
			} catch (ConversionException ex) {
				ex.printStackTrace();
				throw new MessageHandlingException("", null, ex);
			}
			if (message instanceof TranslatedMessage) {
				message = revertTranslated((TranslatedMessage) message);
			}
			if (messageLength < position - 4 - client.getReadPosition()) {
				// Still has content
				client.setReadPosition(messageLength + 4 + client.getReadPosition());
				clientBuffer.position(position);
			} else {
				// Clear the buffer
				clientBuffer.clear();
				client.setReadPosition(0);
			}
			message.setMessageClient(client);
			return message;
		} else if (messageLength > clientBuffer.capacity()) {
			throw new MessageHandlingException("Message length (" + messageLength + ") is larger than client buffer (" + clientBuffer.capacity() + ") capacity");
		} else {
			// If the capacity of the buffer has been reached
			// we must compact it
			// FIXME this involves a data-copy, don't
			clientBuffer.position(client.getReadPosition());
			clientBuffer.compact();
			position = position - client.getReadPosition();
			client.setReadPosition(0);
			clientBuffer.position(position);
		}
		return null;
	}
}
