
package arcane.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import arcane.Arcane;
import arcane.Card;
import arcane.network.LobbyGame;
import arcane.network.Player;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.convert.ArrayConverter;
import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public class Game {
	static private Arcane arcane = Arcane.getInstance();

	private short gameID;
	private Set<Player> players = new LinkedHashSet<Player>();
	private Map<Short, List<Card>> idToLibrary = new HashMap<Short, List<Card>>();
	private Map<Short, List<Card>> idToSideboard = new HashMap<Short, List<Card>>();

	public short getId () {
		return gameID;
	}

	public void setGameID (short gameID) {
		this.gameID = gameID;
	}

	public Set<Player> getPlayers () {
		return players;
	}

	public boolean addPlayer (Player player, int[] mainCardIDs, int[] sideCardIDs) {
		List<Card> libraryCards = new ArrayList<Card>();
		for (int cardID : mainCardIDs)
			libraryCards.add(arcane.getCard(cardID));
		List<Card> sideboardCards = new ArrayList<Card>();
		for (int cardID : sideCardIDs)
			sideboardCards.add(arcane.getCard(cardID));
		if (!isDecklistValid(libraryCards, sideboardCards)) return false;
		players.add(player);
		idToLibrary.put(player.getId(), libraryCards);
		idToSideboard.put(player.getId(), sideboardCards);
		return true;
	}

	public List<Card> getLibrary (short playerID) {
		return idToLibrary.get(playerID);
	}

	public List<Card> getSideboard (short playerID) {
		return idToSideboard.get(playerID);
	}

	public Player getPlayer (short playerID) {
		for (Player player : players)
			if (player.getId() == playerID) return player;
		return null;
	}

	public <T extends Message & PlayerMessage> Player getPlayer (T message) {
		return getPlayer(message.getPlayerId());
	}

	public String toString () {
		return String.valueOf(gameID);
	}

	private boolean isDecklistValid (List<Card> libraryCards, List<Card> sideboardCards) {
		Map<String, Integer> nameToCount = new HashMap<String, Integer>();
		int main = 0, side = 0;
		for (Card card : libraryCards) {
			main++;
			Integer qty = nameToCount.get(card.englishName);
			if (qty == null)
				qty = 1;
			else
				qty++;
			nameToCount.put(card.englishName, qty);
		}
		for (Card card : sideboardCards) {
			side++;
			Integer qty = nameToCount.get(card.englishName);
			if (qty == null)
				qty = 1;
			else
				qty++;
			nameToCount.put(card.englishName, qty);
		}
		for (Entry<String, Integer> entry : nameToCount.entrySet()) {
			Card card = arcane.getCard(entry.getKey());
			if (isTooMany(card, entry.getValue())) return false;
		}
		return main >= 60 && (side == 0 || side == 15);
	}

	private boolean isTooMany (Card card, int qty) {
		if (qty < 4) return false;
		if (card.isBasicLand()) return false;
		if (card.legal.contains("deck can have any number of cards")) return false;
		return true;
	}

	public void readObjectData (ByteBuffer buffer) throws ConversionException {
		players = new LinkedHashSet<Player>(Arrays.asList(new ArrayConverter().readObjectData(buffer, Player[].class)));
	}

	public void writeObjectData (MessageClient client, ByteBuffer buffer) throws ConversionException {
		new ArrayConverter().writeObjectData(client, players.toArray(new Player[players.size()]), buffer);
	}

	public LobbyGame getLobbyGame () {
		LobbyGame lobbyGame = new LobbyGame();
		lobbyGame.setGameID(gameID);
		lobbyGame.setPlayers(players.toArray(new Player[players.size()]));
		return lobbyGame;
	}
}
