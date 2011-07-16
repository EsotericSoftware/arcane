
package arcane.client.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;

import arcane.ArcaneException;
import arcane.client.GameClient;
import arcane.network.MessagePanelType;
import arcane.network.Network.PassPriority;
import arcane.ui.util.UI;

public class MessagePanel extends JPanel {
	static private final int GUTTER = 5;

	private GameClient gameClient;
	private JTextArea textArea;
	private Component component;

	public MessagePanel (GameClient gameClient) {
		this.gameClient = gameClient;
		initializeComponents();
	}

	private void initializeComponents () {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		textArea = new JTextArea();
		textArea.setBackground(null);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		textArea.setFont(new JLabel().getFont());
		add(textArea);
	}

	public void setText (String text, MessagePanelType inputType) {
		if (getComponentCount() > 1) remove(1);
		textArea.setText(text);
		component = null;
		switch (inputType) {
		case none:
			break;
		case pass:
			JButton button = UI.getButton();
			button.setText("Pass");
			add(button);
			button.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					gameClient.sendToServer(new PassPriority());
				}
			});
			component = button;
			break;
		default:
			throw new ArcaneException("Unknown message input type: " + inputType);
		}
		layout();
	}

	public void layout () {
		int width = getWidth() - GUTTER * 2;
		int height = getHeight() - GUTTER * 2;
		Dimension componentSize = component != null ? component.getPreferredSize() : new Dimension();

		textArea.setLocation(GUTTER, GUTTER);
		textArea.setSize(width, height - componentSize.height);

		if (component != null) {
			component.setLocation(width - componentSize.width + GUTTER, height - componentSize.height + GUTTER);
			component.setSize(componentSize);
		}
	}
}
