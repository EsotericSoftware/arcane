package com.captiveimagination.jgn.synchronization;

import com.captiveimagination.jgn.synchronization.message.SynchronizeCreateMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeRemoveMessage;

/**
 * SyncObjectManager implementations can be attached to SynchronizationManagers to manage the creation,
 * deletion, and updates of synchronized objects.
 * 
 * @author Matthew D. Hicks
 */
public interface SyncObjectManager {
	/**
	 * Called when an associated Synchronizer receives a SynchronizeCreateMessage.
	 * If this method returns an Object it is added to the passive registry for the
	 * Synchronizer. If null is returned it is ignored.
	 * 
	 * @param scm
	 * @return
	 * 		created object from message or null if not valid
	 */
	public Object create(SynchronizeCreateMessage scm);
	
	/**
	 * Called when an associated Synchronizer receives a SynchronizeRemoveMessage.
	 * If this method returns true it is removed from the passive registry for the
	 * Synchronizer. If it returns false it is ignored.
	 * 
	 * @param srm
	 * @param obj
	 * @return
	 * 		true if the object was properly removed
	 */
	public boolean remove(SynchronizeRemoveMessage srm, Object obj);
}