
package arcane.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import arcane.network.GameCard;
import arcane.network.Player;
import arcane.network.Zone;
import arcane.network.Network.MoveZoneCards;

public class ZoneManager {
	static private int nextZoneID = 1;

	private GameServer gameServer;
	private Map<Short, List<Zone>> playerIdToZones = new HashMap<Short, List<Zone>>();
	private Map<Integer, List<GameCard>> zoneIdToGameCards = new HashMap<Integer, List<GameCard>>();
	private Map<Integer, Integer> zoneIdToCount = new HashMap<Integer, Integer>();
	private Map<Integer, Zone> gameCardIdToZone = new HashMap<Integer, Zone>();
	private Map<Integer, GameCard> gameCardIdToGameCard = new HashMap<Integer, GameCard>();

	public ZoneManager (GameServer gameServer) {
		super();
		this.gameServer = gameServer;
	}

	private List<Zone> getZones (short playerID) {
		List<Zone> zones = playerIdToZones.get(playerID);
		if (zones == null) {
			zones = new ArrayList<Zone>();
			playerIdToZones.put(playerID, zones);
		}
		return zones;
	}

	public Zone getZone (int zoneID) {
		for (List<Zone> zones : playerIdToZones.values()) {
			for (Zone zone : zones) {
				if (zone.getId() == zoneID) return zone;
			}
		}
		return null;
	}

	public Zone getZone (Player player, String zoneName) {
		return getZone(player.getId(), zoneName, true, true);
	}

	public Zone getZone (short playerID, String zoneName) {
		return getZone(playerID, zoneName, true, true);
	}

	public Zone getZone (Player player, String zoneName, boolean isPublic, boolean isViewable) {
		return getZone(player.getId(), zoneName, isPublic, isViewable);
	}

	public Zone getZone (short playerID, String zoneName, boolean isPublic, boolean isViewable) {
		for (Zone zone : getZones(playerID)) {
			if (zone.getName().equals(zoneName)) return zone;
		}
		Zone zone = new Zone(nextZoneID++, playerID, zoneName, isPublic, isViewable);
		playerIdToZones.get(playerID).add(zone);
		return zone;
	}

	public GameCard getGameCard(int gameCardID){
		GameCard gameCard = gameCardIdToGameCard.get(gameCardID);
		return gameCard;
	}

	public int getCount (Zone zone) {
		Integer count = zoneIdToCount.get(zone.getId());
		if (count != null) return count;
		return 0;
	}

	/**
	 * The returned list should not be modified.
	 */
	public List<GameCard> getGameCards (Zone zone) {
		List<GameCard> cards = zoneIdToGameCards.get(zone.getId());
		if (cards == null) {
			cards = new ArrayList<GameCard>();
			zoneIdToGameCards.put(zone.getId(), cards);
		}
		return cards;
	}

	public void moveGameCards (Zone fromZone, List<GameCard> oldGameCards, Zone toZone, List<GameCard> newGameCardList) {
		List<GameCard> newGameCards = new LinkedList<GameCard>(newGameCardList);
		int[] oldGameCardIDs =  new int[0];

		int i = 0;
		if (fromZone != null) {
			List<GameCard> fromZoneGameCards = getGameCards(fromZone);
			List<GameCard> oldGameCardsFiltered = new ArrayList<GameCard>();
			for (GameCard oldGameCard : oldGameCards) {
				if (fromZoneGameCards.contains(oldGameCard) == true)
					oldGameCardsFiltered.add(oldGameCard);
				else
					newGameCards.remove(oldGameCard);
			}
			oldGameCardIDs = new int[newGameCards.size()];
			for (GameCard oldGameCard : oldGameCardsFiltered) {
				fromZoneGameCards.remove(oldGameCard);
				gameCardIdToZone.remove(oldGameCard.getId());
				gameCardIdToGameCard.remove(oldGameCard.getId());
				oldGameCardIDs[i++] = oldGameCard.getId();
			}
		}

		List<GameCard> toZoneGameCards = getGameCards(toZone);
		for (GameCard newGameCard : newGameCards) {
			toZoneGameCards.add(newGameCard);
			gameCardIdToZone.put(newGameCard.getId(), toZone);
			gameCardIdToGameCard.put(newGameCard.getId(), newGameCard);
		}

		MoveZoneCards message = new MoveZoneCards();
		message.fromZoneID = fromZone == null ? -1 : fromZone.getId();
		message.toZone = toZone;
		message.count = newGameCards.size();

		for (Player player : gameServer.getGame().getPlayers()) {
			if (fromZone != null && fromZone.isViewableBy(player.getId()))
				message.oldGameCardIDs = oldGameCardIDs;
			else
				message.oldGameCardIDs = new int[0];

			if (toZone.isViewableBy(player.getId()))
				message.newGameCards = newGameCards.toArray(new GameCard[newGameCards.size()]);
			else
				message.newGameCards = new GameCard[0];

			gameServer.sendToPlayer(message, player);
		}
	}
}
