package com.captiveimagination.jgn.test.unit;

import java.io.*;

import com.captiveimagination.jgn.ro.*;
import com.captiveimagination.jgn.ro.ping.*;

public class TestPing extends AbstractMessageServerTestCase {
	public void testPing() throws IOException {
		Ping ping = RemoteObjectManager.createRemoteObject(Ping.class, client1to2, 5000);
		for (int i = 0; i < 10; i++) {
			long time = System.nanoTime();
			System.out.println("Ping: " + (System.nanoTime() - ping.ping(time)));
		}
	}
}
