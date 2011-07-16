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

import com.captiveimagination.jgn.message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.logging.Level;

/**
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public final class UDPMessageServer extends NIOMessageServer {
	private DatagramChannel channel;
	private ByteBuffer readLookup;

	public UDPMessageServer(SocketAddress address) throws IOException {
		this(address, 1024);
	}

	public UDPMessageServer(SocketAddress address, int maxQueueSize) throws IOException {
		super(address, maxQueueSize);
		log.log(Level.INFO, " create UDPMessageServer (id={0}){1}, queuesize= {2}",
				new Object[] { serverId, address != null ? " at "+address : "", maxQueueSize });
		setServerType(ServerType.UDP);
		readLookup = ByteBuffer.allocateDirect(1024 * 5);
	}

	protected SelectableChannel bindServer(SocketAddress address) throws IOException {
		channel = selector.provider().openDatagramChannel();
		channel.socket().bind(address);
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    return channel;
	}

	protected void accept(SelectableChannel channel) {
		// UDP Message Server will never receive an accept event
	}

	protected void connect(SelectableChannel channel) {
		// UDP Message Server will never receive a connect event
	}

	protected void read(SelectableChannel c) { //throws IOException {
		MessageClient client = null;
    try {
			InetSocketAddress address = (InetSocketAddress) channel.receive(readLookup);
			if (address == null) {
				// a message was sent but never reached the host, this seems to be ok
				return;
			}
      // blacklist handling
      if (blacklist != null) {
        String newHost = address.getAddress().getHostAddress();
        if (blacklist.contains(newHost)) {
					log.log(Level.WARNING, "UDP-Srv (id={0}) rejecting access for host: {1}", new Object[]{serverId, address});
					System.out.println("UDP-Srv (" + getMessageServerId() + ") Access denied for host: " + newHost);
          return;
        }
      }

			readLookup.limit(readLookup.position());
			readLookup.position(0);
			client = getMessageClient(address);

			if (client == null) {
				client = new MessageClient(address, this);
				client.setStatus(MessageClient.Status.NEGOTIATING);
				getIncomingConnectionQueue().add(client);
				getMessageClients().add(client);
				log.log(Level.FINEST, "'accept' new client {0} from {1}", new Object[]{client, address});
			}
			client.getReadBuffer().put(readLookup);
			readLookup.clear();

			Message message;
			while ((message = readMessage(client)) != null) {
				client.receiveMessage(message);
			}
		} catch (MessageHandlingException exc) {
			if(client != null)
				client.setCloseReason(MessageClient.CloseReason.ErrMessageWrong);
			collectTrafficProblem(client);
		} catch (IOException ioe) {
			// an error occured while reading, it's bad, we don't know the client
			// so do nothing, except logging
			log.log(Level.FINER, " channel.read exception", ioe);
		}
	}

	protected boolean write(SelectableChannel c) {
		for (MessageClient client : getMessageClients()) {
			if (client.getCurrentWrite() != null) {
				client.sent();		// Let the system know something has been written
				try {
					channel.send(client.getCurrentWrite().getBuffer(), client.getAddress());
				} catch (IOException e) { // closed by remote
					client.setCloseReason(MessageClient.CloseReason.ErrChannelWrite);
					collectTrafficProblem(client);
					log.log(Level.FINER, " channel.write exception", e);
					continue;
				}
				if (!client.getCurrentWrite().getBuffer().hasRemaining()) {
					// Write all messages in combined to sent queue
					client.getCurrentWrite().process();
					client.setCurrentWrite(null);
				} else {
					// Take completed messages and add them to the sent queue
					client.getCurrentWrite().process();
				}
			} else {
				CombinedPacket combined;
				try {
					combined = PacketCombiner.combine(client);
				} catch (MessageHandlingException exc) {
					client.setCloseReason(MessageClient.CloseReason.ErrMessageWrong);
					// remember this client for error processing after updateTraffic
					collectTrafficProblem(client);
					combined = null;
				}

				if (combined != null) {
					try {
						channel.send(combined.getBuffer(), client.getAddress());
					} catch (IOException e) {
						client.setCloseReason(MessageClient.CloseReason.ErrChannelWrite);
						// remember this client for error processing after updateTraffic
						collectTrafficProblem(client);
						log.log(Level.FINER, " channel.write exception", e);
						continue;
					}
					if (combined.getBuffer().hasRemaining()) {
						client.setCurrentWrite(combined);

						// Take completed messages and add them to the sent queue
						combined.process();

						return false;	// No more room for sending
					} else {
						client.setCurrentWrite(null);

						// Write all messages in combined to sent queue
						combined.process();
					}
/* ase: check this:
        if following was only intended to transit the state from Disconnecting to Disconnected
        the following omission is ok. That transition is now in updateConnections...
        otherwise, we'll have to find out what to do
*/
//				} else if (client.getStatus() == MessageClient.Status.DISCONNECTING) {
//					disconnectInternal(client, true);
				}
			}
		}
		return false;
	}

  /**
   * since UDP is connectionless, we just create a MessageClient and start negotiation
	 *
   * @param address
   * @return a MessageClient pointing to address
   */
  public MessageClient connect(SocketAddress address) {
		MessageClient client = getMessageClient(address);
		if ((client != null) && (client.getStatus() != MessageClient.Status.DISCONNECTING) &&
				(client.getStatus() != MessageClient.Status.DISCONNECTED)) {
			log.log(Level.FINEST, "return existing client: {0} for: {1}", new Object[]{client, address});
			return client;		// Client already connected, simply return it
		}
		client = new MessageClient(address, this);
		client.setStatus(MessageClient.Status.NEGOTIATING);
		getMessageClients().add(client);
		getIncomingConnectionQueue().add(client);
		getConnectionController().negotiate(client);
		log.log(Level.FINEST, "created client: {0} for: {1}", new Object[]{client, address});
		return client;
	}
}
