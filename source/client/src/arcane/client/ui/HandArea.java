
package arcane.client.ui;

import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;

import arcane.client.ui.util.CardArea;
import arcane.client.ui.util.CardPanel;
import arcane.client.ui.util.CardPanelMouseListener;
import arcane.network.GameCard;
import arcane.network.Network.MoveZoneCards;
import arcane.network.Zone;

public class HandArea extends CardArea {
	public HandArea (JScrollPane scrollPane, final ClientFrame frame) {
		super(scrollPane);

		setDragEnabled(true);
		setVertical(true);

		addCardPanelMouseListener(new CardPanelMouseListener() {
			public void mouseRightClicked (CardPanel panel, MouseEvent evt) {
			}

			public void mouseOver (CardPanel panel, MouseEvent evt) {
			}

			public void mouseOut (CardPanel panel, MouseEvent evt) {
			}

			public void mouseMiddleClicked (CardPanel panel, MouseEvent evt) {
			}

			public void mouseLeftClicked (CardPanel panel, MouseEvent evt) {
				if (mouseOverPanel == null) return;
				MoveZoneCards message = new MoveZoneCards();
				message.fromZoneID = getZoneID();
				short localPlayerID = frame.getGameClient().getLocalPlayer().getId();
				Zone play = new Zone(frame.getPlayerPanel(localPlayerID).getPlayArea().getZoneID(), localPlayerID, "Play", true, true);
				message.toZone = play;
				message.newGameCards = new GameCard[] {mouseOverPanel.gameCard};
				frame.getGameClient().sendToServer(message);
			}

			public void mouseDragged (CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt) {
			}

			public void mouseDragStart (CardPanel dragPanel, MouseEvent evt) {
			}

			public void mouseDragEnd (CardPanel dragPanel, MouseEvent evt) {
			}
		});
	}
}
