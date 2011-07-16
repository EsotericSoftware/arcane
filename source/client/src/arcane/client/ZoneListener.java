
package arcane.client;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import arcane.ArcaneException;
import arcane.Card;
import arcane.client.ui.ClientFrame;
import arcane.client.ui.PlayerInfoPanel;
import arcane.client.ui.util.Animation;
import arcane.client.ui.util.CardPanel;
import arcane.client.ui.util.CardPanelContainer;
import arcane.network.GameCard;
import arcane.network.Network.MoveZoneCards;
import arcane.network.Zone;
import arcane.ui.util.UI;
import arcane.util.Util;

import com.captiveimagination.jgn.event.DynamicMessageAdapter;

public class ZoneListener extends DynamicMessageAdapter {
	private ClientFrame frame;
	private Map<Integer, Zone> idToZone = new HashMap<Integer, Zone>();
	private Map<Integer, List<GameCard>> zoneIdToGameCards = new HashMap<Integer, List<GameCard>>();
	private Map<Integer, Integer> zoneIdToCount = new HashMap<Integer, Integer>();

	public ZoneListener (ClientFrame frame) {
		this.frame = frame;
	}

	public void messageReceived (MoveZoneCards message) {
		// Get "to" zone.
		Zone toZone = idToZone.get(message.toZone.getId());
		if (toZone == null) {
			toZone = message.toZone;
			idToZone.put(toZone.getId(), toZone);
		}
		// Store zone ID in card containers.
		final CardPanelContainer toArea = getCardPanelContainer(toZone);
		if (toArea != null) toArea.setZoneID(toZone.getId());
		// Update "to" zone count.
		int count = getCount(toZone.getId());
		count += message.count;
		zoneIdToCount.put(toZone.getId(), count);
		PlayerInfoPanel infoPanel = frame.getPlayerPanel(toZone.getPlayerID()).getInfoPanel();
		infoPanel.updateZone(toZone, toZone.getName() + ": " + count);
		// Update "to" zone cards.
		List<GameCard> toZoneCards = getGameCards(toZone.getId());
		toZoneCards.addAll(Arrays.asList(message.newGameCards));

		// If new cards were created, don't remove from old zone or start an animation.
		if (message.fromZoneID == -1) return;

		// Get "from" zone.
		Zone fromZone = idToZone.get(message.fromZoneID);
		if (fromZone == null) throw new ArcaneException("Unknown \"from\" zone ID: " + message.fromZoneID);
		// Update "from" count.
		count = getCount(fromZone.getId());
		count -= message.count;
		zoneIdToCount.put(fromZone.getId(), count);
		infoPanel = frame.getPlayerPanel(fromZone.getPlayerID()).getInfoPanel();
		infoPanel.updateZone(fromZone, fromZone.getName() + ": " + count);

		// Get zone locations for animation.
		JLayeredPane layeredPane = frame.getLayeredPane();
		CardPanelContainer fromArea = getCardPanelContainer(fromZone);
		int fromZoneX = 0, fromZoneY = 0;
		if (fromArea == null) {
			infoPanel = frame.getPlayerPanel(fromZone.getPlayerID()).getInfoPanel();
			Point zoneLocation = infoPanel.getZoneLocation(fromZone.getId());
			fromZoneX = zoneLocation.x;
			fromZoneY = zoneLocation.y;
		}
		int toZoneX = 0, toZoneY = 0;
		if (toArea == null) {
			infoPanel = frame.getPlayerPanel(toZone.getPlayerID()).getInfoPanel();
			Point zoneLocation = infoPanel.getZoneLocation(toZone.getId());
			toZoneX = zoneLocation.x;
			toZoneY = zoneLocation.y;
		}

		for (int i = 0; i < message.count; i++) {
			GameCard showGameCard = null;

			// Remove card from "from" zone.
			if (message.oldGameCardIDs.length > 0) {
				int oldGameCardID = message.oldGameCardIDs[i];
				List<GameCard> fromZoneCards = getGameCards(fromZone.getId());
				for (Iterator<GameCard> iter = fromZoneCards.iterator(); iter.hasNext();) {
					GameCard fromZoneCard = iter.next();
					if (fromZoneCard.getId() == oldGameCardID) {
						showGameCard = fromZoneCard;
						iter.remove();
						break;
					}
				}
			}
			// Get start animation location and remove card from "from" card container.
			int startWidth, startX, startY;
			if (message.oldGameCardIDs.length > 0 && fromArea != null) {
				//fromArea
				CardPanel fromPanel = fromArea.getCardPanel(message.oldGameCardIDs[i]);
				//if(fromPanel == null) return;
				startWidth = fromPanel.getCardWidth();
				Point fromPos = SwingUtilities.convertPoint(fromArea, fromPanel.getCardLocation(), layeredPane);
				startX = fromPos.x;
				startY = fromPos.y;
				fromArea.removeCardPanel(fromPanel);
			} else {
				startWidth = 10;
				startX = fromZoneX - Math.round(startWidth / 2.0f);
				startY = fromZoneY - Math.round(Math.round(startWidth * CardPanel.ASPECT_RATIO) / 2.0f);
			}

			if (message.newGameCards.length > 0) showGameCard = message.newGameCards[i];
			// Get end animation location and add card to "to" card container.
			CardPanel toPanel = null;
			int endWidth, endX, endY;
			if (message.newGameCards.length > 0 && toArea != null) {
				// Add card to container in Swing thread.
				class AddCardRunnable implements Runnable {
					public GameCard addCard;
					public CardPanel toPanel;

					public void run () {
						toPanel = toArea.addCard(addCard);
					}
				}
				AddCardRunnable addCardRunnable = new AddCardRunnable();
				addCardRunnable.addCard = showGameCard;
				UI.invokeAndWait(addCardRunnable);
				toPanel = addCardRunnable.toPanel;

				endWidth = toPanel.getCardWidth();
				Point toPos = SwingUtilities.convertPoint(toArea, toPanel.getCardLocation(), layeredPane);
				endX = toPos.x;
				endY = toPos.y;
			} else {
				endWidth = 10;
				endX = toZoneX - Math.round(endWidth / 2.0f);
				endY = toZoneY - Math.round(Math.round(endWidth * CardPanel.ASPECT_RATIO) / 2.0f);
			}

			// Use card back when from and to zone contents cannot be seen.
			if (showGameCard == null) showGameCard = new GameCard(Card.back);

			// Run animation.
			CardPanel animationPanel = new CardPanel(showGameCard, true);
			if (toZone.getName().equals("Play")) {
				// BOZO - Make moveCardToPlay more like moveCard.
				Animation.moveCardToPlay(startX, startY, startWidth, endX, endY, endWidth, animationPanel, toPanel, layeredPane, 400);
			} else
				Animation.moveCard(startX, startY, startWidth, endX, endY, endWidth, animationPanel, toPanel, layeredPane, 400);

			Util.sleep(100);
		}
	}

	public List<GameCard> getGameCards (int zoneID) {
		List<GameCard> cards = zoneIdToGameCards.get(zoneID);
		if (cards == null) {
			cards = new ArrayList<GameCard>();
			zoneIdToGameCards.put(zoneID, cards);
		}
		return cards;
	}

	private int getCount (int zoneID) {
		Integer count = zoneIdToCount.get(zoneID);
		if (count != null) return count;
		return 0;
	}

	private CardPanelContainer getCardPanelContainer (Zone zone) {
		if (zone.getName().equals("Play")) return frame.getPlayerPanel(zone.getPlayerID()).getPlayArea();
		if (zone.getName().equals("Graveyard")) return frame.getPlayerPanel(zone.getPlayerID()).getGraveyardArea();
		if (zone.getPlayerID() == frame.getGameClient().getLocalPlayer().getId() && zone.getName().equals("Hand"))
			return frame.getHandArea();
		return null;
	}
}
