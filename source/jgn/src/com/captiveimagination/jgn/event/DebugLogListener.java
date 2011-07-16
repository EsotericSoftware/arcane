package com.captiveimagination.jgn.event;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.NoopMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple debug listener for MessageListener and ConnectionListener that
 * prints out method invocations using logging system on level FINE.
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public class DebugLogListener implements MessageListener, ConnectionListener {
	private static Logger LOG = Logger.getLogger("com.captiveimagination.jgn.event.DebugLogListener");

	private String id;
	private boolean ignoreNoop = true;

	public DebugLogListener(String id) {
		this.id = id;
	}

	private Object[] extractMessage(Message message) {
		return new Object[]{id,
				message.getMessageClient().getAddress(),
				message};
	}

	private Object[] extractClient(MessageClient client, String rsn) {
		return new Object[]{id,
				client.getAddress(),
				String.valueOf(client.getId()),
				rsn == null ? client.getCloseReason() : rsn};
	}

	public void messageCertified(Message message) {
		LOG.log(Level.FINE, "{0}: messageCertified() from {1} : {2}", extractMessage(message));
	}

	public void messageFailed(Message message) {
		LOG.log(Level.FINE, "{0}: messageFailed() @ {1} : {2}", extractMessage(message));
	}

	public void messageReceived(Message message) {
		if (!(ignoreNoop && message instanceof NoopMessage))
			LOG.log(Level.FINE, "{0}: messageReceived() from {1} : {2}", extractMessage(message));
	}

	public void messageSent(Message message) {
		if (!(ignoreNoop && message instanceof NoopMessage))
			LOG.log(Level.FINE, "{0}: messageSent() to {1} : {2}", extractMessage(message));
	}

	public void connected(MessageClient client) {
		LOG.log(Level.FINE, "{0}: connected() with {1}, id= {2}", extractClient(client, null));
	}

	public void disconnected(MessageClient client) {
		LOG.log(Level.FINE, "{0}: disconnected() from {1}, id= {2}. (reason={3})", extractClient(client, null));
	}

	public void negotiationComplete(MessageClient client) {
		LOG.log(Level.FINE, "{0}: negotiationComplete() with {1}, id= {2}", extractClient(client, null));
	}

	public void kicked(MessageClient client, String reason) {
		LOG.log(Level.FINE, "{0}: kicked() @ {1}, id= {2}. (reason={3})", extractClient(client, reason));
	}
}
