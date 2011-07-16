/* All rights reserved.
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
 * Created: october 20, 2008
 */
package com.captiveimagination.jgn.test.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.MessageServer;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.file.JGNFileWrapper;

public class TestFileWrapper {
	public static void main(String[] args) throws Exception {
		MessageServer server1 = new TCPMessageServer(new InetSocketAddress(
				InetAddress.getLocalHost(), 10000));

		Thread t1 = JGN.createThread(server1);
		t1.start();

		System.out.println("server 1 loaded!!");

		MessageServer server2 = new TCPMessageServer(new InetSocketAddress(
				InetAddress.getLocalHost(), 20000));
		Thread t2 = JGN.createThread(server2);
		t2.start();

		System.out.println("server 2 loaded!!");

		MessageClient client = server2.connectAndWait(new InetSocketAddress(
				InetAddress.getLocalHost(), 10000), 5000);
		if (client != null) {
			System.out.println("client connected!!");
			JGNFileWrapper fileWrapper1 = new JGNFileWrapper(server1);

			JGNFileWrapper fileWrapper2 = new JGNFileWrapper(server2);
			fileWrapper2.addMessageListener(new FileMessageListener());
			fileWrapper1.broadcastFile(new File("from.mp3"), "from.mp3",
					"/here/");
			
			MyFileMessage test = new MyFileMessage();
			test.setFile(new File("from.mp3"));
			test.setTest("test field !!!!");
			fileWrapper1.broadcastFileMessage(test);

		} else {
			System.out.println("failed connecting ... :-(");
		}
	}

	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}
}
