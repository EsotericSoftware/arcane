package com.captiveimagination.jgn.test.ro;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.ro.RemoteObject;
import com.captiveimagination.jgn.ro.RemoteObjectManager;

public class TestRemoteObject {
	public static void main(String[] args) throws Exception {
		// Create our first server
		MessageServer server1 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		JGN.createThread(server1).start();
		
		// Create our second server
		MessageServer server2 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		JGN.createThread(server2).start();
		
		// Establish a MessageClient from server2 to server1
		MessageClient client = server2.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		
		// Create the RemoteActionImplementation for server2 and register it
		RemoteAction impl = new RemoteActionImplementation();
		impl.setMessage("Initial Message");
		RemoteObjectManager.registerRemoteObject(RemoteAction.class, impl, server1);
		
		// Get an instance of RemoteAction on server1 pointing back to server2
		RemoteAction action = RemoteObjectManager.createRemoteObject(RemoteAction.class, client, 5000);
		
		// Invoke methods on RemoteAction that get invoked on server2
		System.out.println("Message: " + action.getMessage());
		action.setMessage("Testing");
		System.out.println("Message: " + action.getMessage());
	}
}

interface RemoteAction extends RemoteObject {
	public String getMessage();
	
	public void setMessage(String message);
}

class RemoteActionImplementation implements RemoteAction {
	private String message;
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}