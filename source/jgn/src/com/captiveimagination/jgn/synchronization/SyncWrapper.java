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
 * Created: Aug 26, 2007
 */
package com.captiveimagination.jgn.synchronization;

import java.util.HashMap;

import com.captiveimagination.jgn.clientserver.JGNClient;
import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.captiveimagination.jgn.synchronization.message.SynchronizeCreateMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeRemoveMessage;

class SyncWrapper {
	private short id;
	private long waitingId;
	
	private Object object;
	private long rate;
	private SynchronizeCreateMessage createMessage;
	private short ownerPlayerId;
	
	/**
	 * Long for storing when the last update to the server was, if this is on the client
	 */
	private long lastUpdate;
	/**
	 * Map for storing when the last update to a connected client was, if this is on the server
	 */
	private HashMap<Short, Long> lastServerUpdate = new HashMap<Short, Long>();
	/**
	 * Map for storing the last action of create/remove for a connected client
	 * 0 = nothing; 1 = sent create message; 2 = sent remove message
	 */
	private HashMap<Short, Short> lastAction = new HashMap<Short, Short>();
	
	public SyncWrapper(Object object, long rate, SynchronizeCreateMessage createMessage, short ownerPlayerId) {
		if (object == null) throw new RuntimeException("Object is null: " + id);
		
		this.object = object;
		this.rate = rate * 1000000;		// Convert to nanoseconds for better timing
		this.createMessage = createMessage;
		this.ownerPlayerId = ownerPlayerId;
		lastUpdate = System.nanoTime() - this.rate;		// Make ready for immediate update
	}
	
	public short getId() {
		return id;
	}

	public void setId(short id) {
		this.id = id;
	}
	
	public long getWaitingId() {
		return waitingId;
	}
	
	public void setWaitingId(long waitingId) {
		this.waitingId = waitingId;
	}
	
	public Object getObject() {
		return object;
	}

	public long getRate() {
		return rate;
	}

	public SynchronizeCreateMessage getCreateMessage() {
		return createMessage;
	}

	public short getOwnerPlayerId() {
		return ownerPlayerId;
	}
	
	/**
	 * Removes this key from the lastServerUpdate map
	 * @param playerId the key to remove
	 * @return the object referenced by this key or null if the key didn't exist
	 */
	public Object removeLastServerUpdateKey(short playerId) {
		return lastServerUpdate.remove(playerId);
	}
	
	/**
	 * Removes this key from the lastAction map
	 * @param playerId the key to remove
	 * @return the object referenced by this key or null if the key didn't exist
	 */
	public Object removeLastActionKey(short playerId) {
		return lastAction.remove(playerId);
	}
	
	public boolean hasLastAction(short playerId) {
		return lastAction.containsKey(playerId);
	}
	
	public short getLastAction(short playerId) {
		return lastAction.get(playerId);
	}
	
	public void setLastAction(short playerId, short value) {
		lastAction.put(playerId, value);
	}
	
	protected void update(SynchronizationManager manager, JGNServer server, GraphicalController controller) {
		long updateRate = rate;
		SynchronizeMessage message = controller.createSynchronizationMessage(getObject());
		message.setSyncManagerId(manager.getId());
		message.setSyncObjectId(getId());
		for(JGNConnection conn : server.getConnections()) {
			if(controller.proximity(getObject(), conn.getPlayerId()) > 0f && conn.getPlayerId() != getOwnerPlayerId()) {
				//Check if sent create message
				if(lastAction.containsKey(conn.getPlayerId())) {
					if(lastAction.get(conn.getPlayerId()) != 1) { //No create message sent
						System.out.println("Sending create for "+getId());
						lastAction.put(conn.getPlayerId(), (short)1);
						getCreateMessage().setSyncObjectId(getId());
						getCreateMessage().setSyncManagerId(manager.getId());
						server.sendToPlayer(getCreateMessage(), conn.getPlayerId());
					}
				}
				else {
					System.out.println("Sending create for "+getId());
					lastAction.put(conn.getPlayerId(), (short)1);
					getCreateMessage().setSyncObjectId(getId());
					getCreateMessage().setSyncManagerId(manager.getId());
					server.sendToPlayer(getCreateMessage(), conn.getPlayerId());
				}
				updateRate = (long) (rate/controller.proximity(getObject(), conn.getPlayerId()));
				if(!lastServerUpdate.containsKey(conn.getPlayerId())) {
					lastServerUpdate.put(conn.getPlayerId(), (long)0);
				}
				if (lastServerUpdate.get(conn.getPlayerId()) + updateRate < System.nanoTime()) {
					if (server.getConnections().length > 0) {
						server.sendToPlayer(message, conn.getPlayerId());
					}
					
					lastServerUpdate.put(conn.getPlayerId(), System.nanoTime());
				}
			}
			else if(controller.proximity(getObject(), conn.getPlayerId()) == 0f){
				//Check if sent remove message
				if(lastAction.containsKey(conn.getPlayerId())) {
					if(lastAction.get(conn.getPlayerId()) != 2) { //No remove message sent
						System.out.println("Sending remove for "+getId());
						lastAction.put(conn.getPlayerId(), (short)2);
						SynchronizeRemoveMessage remove = new SynchronizeRemoveMessage();
						remove.setSyncObjectId(getId());
						remove.setSyncManagerId(manager.getId());
						server.sendToPlayer(remove, conn.getPlayerId());
					}
				}
				else {
					lastAction.put(conn.getPlayerId(), (short)2);
				}
			}
		}
	}
	
	protected void update(SynchronizationManager manager, JGNClient client, GraphicalController controller) {
		SynchronizeMessage message = controller.createSynchronizationMessage(getObject());
		message.setSyncManagerId(manager.getId());
		message.setSyncObjectId(getId());
		
		if(lastUpdate + rate < System.nanoTime()) {
			client.sendToServer(message);
			lastUpdate = System.nanoTime();
		}
	}
}
