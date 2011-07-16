package com.captiveimagination.jgn.test.stream;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.stream.*;

public class TestStream {
	public static void main(String[] args) throws Exception {
		MessageServer server1 = new TCPMessageServer(new InetSocketAddress((InetAddress)null, 1000));
		server1.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
			}

			public void negotiationComplete(final MessageClient client) {
				Thread t = new Thread() {
					public void run() {
						try {
							long time = System.currentTimeMillis();
							System.out.println("Waiting for incoming data...");
							JGNInputStream input = client.getInputStream();
							FileOutputStream fos = new FileOutputStream(new File("test.bin"));
							byte[] buffer = new byte[512];
							int len;
							while ((len = input.read(buffer)) != -1) {
								fos.write(buffer, 0, len);
							}
							fos.flush();
							fos.close();
							input.close();
							System.out.println("Finished receiving file in " + (System.currentTimeMillis() - time) + "ms");
						} catch(Exception exc) {
							exc.printStackTrace();
						}
					}
				};
				t.start();
			}

			public void disconnected(MessageClient client) {
			}

			
			public void kicked(MessageClient client, String reason) {
			}
		});
		Thread t1 = JGN.createThread(server1);
		t1.start();
		
		MessageServer server2 = new TCPMessageServer(new InetSocketAddress((InetAddress)null, 2000));
		Thread t2 = JGN.createThread(server2);
		t2.start();
		
		MessageClient client = server2.connectAndWait(new InetSocketAddress((InetAddress)null, 1000), 5000);
		if (client != null) {
			System.out.println("Connected...");
			JGNOutputStream output = client.getOutputStream();
			FileInputStream fis = new FileInputStream(new File("file.zip"));
			byte[] buffer = new byte[512];
			int len;
			while ((len = fis.read(buffer)) != -1) {
				output.write(buffer, 0, len);
			}
			output.flush();
			output.close();
			fis.close();
			System.out.println("Finished sending file!");
		}
	}
}
