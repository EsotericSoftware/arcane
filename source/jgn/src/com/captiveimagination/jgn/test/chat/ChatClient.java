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
 * Created: Jul 31, 2006
 */
package com.captiveimagination.jgn.test.chat;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.clientserver.JGNClient;
import com.captiveimagination.jgn.event.DebugListener;
import com.captiveimagination.jgn.event.DynamicMessageAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author Matthew D. Hicks
 */
public class ChatClient extends DynamicMessageAdapter implements ActionListener {
	private JGNClient client;
	private String nickname;
	private JTextPane textPane;
    private JTextField textField;

	public ChatClient() throws Exception {
		JGN.register(NamedChatMessage.class);

		client = new JGNClient();
		client.addMessageListener(this);
		client.addMessageListener(new DebugListener("ChatClient>"));
		JGN.createThread(client).start();

		InetSocketAddress reliableServerAddress = new InetSocketAddress(InetAddress.getLocalHost(), 1000);
		InetSocketAddress fastServerAddress = new InetSocketAddress(InetAddress.getLocalHost(), 2000);

		client.connectAndWait(reliableServerAddress, fastServerAddress, 5000);
		nickname = JOptionPane.showInputDialog("Connection established to server\n\nPlease enter the name you wish to use?");
		initGUI();
	}

	private void initGUI() {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	// TODO fix
		frame.setTitle("Chat Client - " + nickname);
		frame.setSize(300, 300);
		Container c = frame.getContentPane();
        c.setLayout(new BorderLayout());
        textPane = new JTextPane();
        textPane.setText("");
        textPane.setEditable(false);
        c.add(BorderLayout.CENTER, textPane);
        textField = new JTextField();
        textField.addActionListener(this);
        c.add(BorderLayout.SOUTH, textField);
        frame.setVisible(true);
	}

	public void actionPerformed(ActionEvent evt) {
		String txt = textField.getText().trim();
		if (txt.length() > 0) {
			NamedChatMessage message = new NamedChatMessage();
			message.setPlayerName(nickname);
			message.setText(txt);

			if (txt.startsWith("srv")) client.sendToServer(message);
			else if (txt.startsWith("0")) client.sendToPlayer(message, (short) 0);
			else client.broadcast(message);

			textField.setText("");
			writeMessage(client.getPlayerId(), nickname, message.getText());
		}
	}

	public void messageReceived(NamedChatMessage message) {
		writeMessage(message.getPlayerId(), message.getPlayerName(), message.getText());
	}

//	public void messageReceived(Message nm) {
//		if (nm instanceof NamedChatMessage) {
//			NamedChatMessage message = (NamedChatMessage)nm;
//			writeMessage(message.getPlayerId(), message.getPlayerName(), message.getText());
//		}
//	}

	private void writeMessage(short playerId, String playerName, String text) {
		String message = "[" + playerName + ":" + playerId + "]: " + text;
		if (textPane.getText().length() == 0) {
            textPane.setText(message);
        } else {
            textPane.setText(textPane.getText() + "\r\n" + message);
        }
	}

	public static void main(String[] args) throws Exception {
		new ChatClient();
	}
}
