
package arcane.client.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import arcane.client.GameClient;
import arcane.network.Network;
import arcane.network.Network.Chat;
import arcane.ui.util.UI;

public class ChatPanel extends JPanel {
	private GameClient gameClient;
	private JTextPane textPane;
	private JTextField textField;
	private JButton sendButton;
	private JScrollPane textScroll;

	public ChatPanel (GameClient gameClient) {
		this.gameClient = gameClient;
		initializeComponents();
	}

	private void initializeComponents () {
		textScroll = new JScrollPane();
		add(textScroll);

		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setFont(new JLabel().getFont());
		textScroll.setViewportView(textPane);

		ActionListener sendTextListener = new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				String text = textField.getText().trim();
				if (text.length() == 0) return;
				textField.setText("");
				send(text);
			}
		};

		textField = new JTextField();
		add(textField);
		textField.addActionListener(sendTextListener);

		sendButton = UI.getButton();
		sendButton.setText("Send");
		sendButton.addActionListener(sendTextListener);
		add(sendButton);
	}

	public void layout () {
		int width = getWidth();
		int height = getHeight();
		Dimension buttonSize = sendButton.getPreferredSize();

		textScroll.setSize(width, height - buttonSize.height);

		textField.setLocation(0, height - buttonSize.height);
		textField.setSize(width - buttonSize.width, buttonSize.height);

		sendButton.setLocation(width - buttonSize.width, height - buttonSize.height);
		sendButton.setSize(buttonSize);
	}

	private void send (String text) {
		Chat chat = new Network.Chat();
		chat.text = text;
		gameClient.sendToServer(chat);
	}

	public void addMessage (String text) {
		Document document = textPane.getDocument();
		try {
			document.insertString(document.getEndPosition().getOffset(), text + "\n", null);
		} catch (BadLocationException ignored) {
		}
		textPane.setCaretPosition(document.getLength());
	}
}
