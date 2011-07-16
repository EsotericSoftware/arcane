
package arcane.client.ui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import arcane.client.GameClient;
import arcane.network.Network;
import arcane.ui.util.UI;

public class ForcePanel extends JPanel {
	private GameClient gameClient;

	public ForcePanel (GameClient gameClient) {
		this.gameClient = gameClient;

		initializeComponents();
	}

	private void initializeComponents () {
		setLayout(new GridLayout(5, 2));
		{
			JButton button = UI.getButton();
			button.setText("Draw");
			add(button);
			button.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					gameClient.sendToServer(new Network.DrawCard());
				}
			});
		}
		{
			JButton button = UI.getButton();
			button.setText("Arrow");
			add(button);
		}
		{
			JButton button = UI.getButton();
			button.setText("Damage");
			add(button);
		}
		{
			JButton button = UI.getButton();
			button.setText("Zone");
			add(button);
		}
		{
			JButton button = UI.getButton();
			button.setText("Power");
			add(button);
		}
		{
			JButton button = UI.getButton();
			button.setText("Toughness");
			add(button);
		}
		{
			JButton button = UI.getButton();
			button.setText("P/T Counter");
			add(button);
		}
		{
			JButton button = UI.getButton();
			button.setText("Counter");
			add(button);
		}
		{
			JButton button = UI.getButton();
			button.setText("Facedown");
			add(button);
		}
	}
}
