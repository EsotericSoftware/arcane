package com.captiveimagination.jgn.test.unit;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.type.*;

public class TestUniqueMessages extends AbstractMessageServerTestCase {
	@SuppressWarnings("all")
	public void testUniqueMessage1() throws Exception {
		MyUniqueMessage message = new MyUniqueMessage();
		server1.addMessageListener(new DynamicMessageAdapter() {
			public void messageReceived(Message message) {
			}

			public void messageSent(Message message) {
			}
			
			public void messageSent(UniqueMessage message) {
				System.out.println("S1> Sent unique message: " + message.getId());
			}
		});
		server2.addMessageListener(new DynamicMessageAdapter() {
			public void messageReceived(Message message) {
			}
			
			public void messageReceived(UniqueMessage message) {
				System.out.println("S2> Received unique message: " + message.getId());
			}

			public void messageSent(Message message) {
			}
		});
		client1to2.sendMessage(message);
		client1to2.sendMessage(message);
		Thread.sleep(5000);
	}
}
