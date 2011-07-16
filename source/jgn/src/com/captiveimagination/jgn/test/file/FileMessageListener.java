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

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.file.FileMessage;
import com.captiveimagination.jgn.message.Message;

public class FileMessageListener implements MessageListener {

	public void messageCertified(Message message) {

	}

	public void messageFailed(Message message) {

	}

	public void messageReceived(Message message) {
		if (message instanceof FileMessage) {
			FileMessage m = (FileMessage) message;
			File f = new File(m.getFile(), "test");
			System.out.println(m.getFileName());
			System.out.println(m.getPath());
			System.out.println(m.getFile().length());
		}
		if (message instanceof MyFileMessage) {
			MyFileMessage m = (MyFileMessage) message;
			System.out.println(m.getTest());
		}
	}

	public void messageSent(Message message) {

	}
}