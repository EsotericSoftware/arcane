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
 * Created: Oct 3, 2006
 */
package com.captiveimagination.jgn.test.so.swing;

import java.awt.*;
import java.awt.event.*;
import java.net.*;

import javax.swing.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.so.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class TestSharedObjectClient implements SharedObjectListener, ActionListener {
	private JFrame frame;
	private JTextField field1;
	private JTextField field2;
	private JTextField field3;
	
	public TestSharedObjectClient() throws Exception {
		MessageServer server = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 0));
		JGN.createThread(server, SharedObjectManager.getInstance()).start();
		
		SharedObjectManager.getInstance().addListener(this);
		SharedObjectManager.getInstance().enable(server);
		
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(200, 150);
		
		Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(3, 2));
		c.add(BorderLayout.CENTER, panel);
		
		JLabel label = new JLabel("Name: ");
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(label);
		field1 = new JTextField();
		panel.add(field1);
		label = new JLabel("Address: ");
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(label);
		field2 = new JTextField();
		panel.add(field2);
		label = new JLabel("Phone: ");
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(label);
		field3 = new JTextField();
		panel.add(field3);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		JButton button = new JButton("Apply");
		button.addActionListener(this);
		buttons.add(button);
		c.add(BorderLayout.SOUTH, buttons);
		
		frame.setVisible(true);
		
		MessageClient client = server.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		if (client != null) {
			System.out.println("Connected...");
			frame.setTitle("Connection ID: " + client.getId());
		} else {
			System.out.println("Problem connecting!");
		}
	}

	public void changed(String name, Object object, String field, MessageClient client) {
		if (object instanceof Person) {
			Person p = (Person)object;
			if (field.equals("name")) {
				field1.setText(p.getName());
			} else if (field.equals("address")) {
				field2.setText(p.getAddress());
			} else if (field.equals("phone")) {
				field3.setText(p.getPhone());
			}
		}
	}

	public void created(String name, Object object, MessageClient client) {
		System.out.println("Created: " + object);
	}

	public void removed(String name, Object object, MessageClient client) {
	}
	
	public void actionPerformed(ActionEvent e) {
		Person p = (Person)SharedObjectManager.getInstance().getObject("Person");
		if (p != null) {
			if (!p.getName().equals(field1.getText())) {
				p.setName(field1.getText());
			} else if (!p.getAddress().equals(field2.getText())) {
				p.setAddress(field2.getText());
			} else if (!p.getPhone().equals(field3.getText())) {
				p.setPhone(field3.getText());
			}
		} else {
			System.out.println("Trying to apply but person doesn't exist!");
		}
	}
	
	public static void main(String[] args) throws Exception {
		new TestSharedObjectClient();
	}
}
