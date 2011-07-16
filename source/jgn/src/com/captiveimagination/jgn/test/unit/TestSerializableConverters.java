package com.captiveimagination.jgn.test.unit;

import java.util.*;

import com.captiveimagination.jgn.event.*;

/**
 * @author Matthew D. Hicks
 */
public class TestSerializableConverters extends AbstractMessageServerTestCase {
	@SuppressWarnings("all")
	public void testSerializable() throws Exception {
		server2.addMessageListener(new DynamicMessageAdapter() {
			public void messageReceived(MySerializableMessage message) {
				System.out.println("Received Message: " + message.getCalendar());
			}
		});
		
		MySerializableMessage message = new MySerializableMessage();
		GregorianCalendar calendar = new GregorianCalendar();
		message.setCalendar(calendar);
		client1to2.sendMessage(message);
		
		Thread.sleep(5000);
	}
}
