
package arcane.network;

import com.captiveimagination.jgn.convert.type.FieldSerializable;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public final class LobbyGame implements FieldSerializable {

	private short gameID;
	private Player[] players;

	public short getId () {
		return gameID;
	}

	public void setGameID (short gameID) {
		this.gameID = gameID;
	}

	public short getGameID () {
		return gameID;
	}

	public void setPlayers (Player[] players) {
		this.players = players;
	}

	public Player[] getPlayers () {
		return players;
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
}
