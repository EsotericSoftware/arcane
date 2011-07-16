
package arcane.network;

import arcane.Arcane;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.RealtimeMessage;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public class Network {
	static public int getPort () {
		return Arcane.getInstance().getPrefs().getInt("network.port", 52038);
	}

	static public void register () {
		JGN.register(GameCard.class);
		JGN.register(LobbyGame.class);
		JGN.register(MessagePanelType.class);
		JGN.register(Phase.class);
		JGN.register(Player.class);
		JGN.register(Zone.class);

		JGN.register(RegisterPlayer.class);
		JGN.register(EnterLobby.class);
		JGN.register(PlayerListUpdate.class);
		JGN.register(GameListUpdate.class);
		JGN.register(CreateLobbyGame.class);
		JGN.register(JoinLobbyGame.class);
		JGN.register(LeaveLobbyGame.class);
		JGN.register(StartGame.class);
		JGN.register(LeaveGame.class);
		JGN.register(Chat.class);
		JGN.register(PassPriority.class);
		JGN.register(GivePriority.class);
		JGN.register(SetMessage.class);
		JGN.register(SetPhase.class);
		JGN.register(SetPriorityStops.class);
		JGN.register(MoveZoneCards.class);
		JGN.register(DrawCard.class);
		JGN.register(TapCard.class);
	}

	static public class RegisterPlayer extends Message implements PlayerMessage {
		public Player player;
		public String version;
	}

	// Lobby:

	static public class EnterLobby extends Message implements PlayerMessage {
	}

	static public class PlayerListUpdate extends RealtimeMessage implements PlayerMessage {
		public String[] playerNames;
	}

	static public class GameListUpdate extends RealtimeMessage implements PlayerMessage {
		public LobbyGame[] lobbyGames;
	}

	static public class CreateLobbyGame extends Message implements PlayerMessage {
		public int[] mainCards;
		public int[] sideCards;
	}

	static public class JoinLobbyGame extends Message implements PlayerMessage {
		public short gameID;
		public int[] mainCards;
		public int[] sideCards;
	}

	static public class LeaveLobbyGame extends Message implements PlayerMessage {
	}

	static public class StartGame extends Message implements PlayerMessage {
		public LobbyGame lobbyGame;
	}

	// ----
	// Game:

	/**
	 * Used for messages sent from the client to the server for a specific game.
	 */
	static public abstract class GameServerMessage extends Message implements PlayerMessage {
		public short gameID = -1;
	}

	static public class LeaveGame extends GameServerMessage {
	}

	static public class Chat extends GameServerMessage {
		public String text;

		public Chat () {
			setPlayerId((short)-2);
		}

		public Chat (String text) {
			this.text = text;
			setPlayerId((short)-2);
		}
	}

	static public class SetMessage extends Message implements PlayerMessage {
		public String text;
		public MessagePanelType inputType;

		public SetMessage () {
		}

		public SetMessage (String text, MessagePanelType inputType) {
			this.text = text;
			this.inputType = inputType;
		}
	}

	static public class PassPriority extends GameServerMessage {
	}

	static public class GivePriority extends Message implements PlayerMessage {
	}

	static public class SetPriorityStops extends GameServerMessage {
		public Phase[] priorityStops;
	}

	static public class SetPhase extends Message implements PlayerMessage {
		public Phase phase;

		public SetPhase () {
		}

		public SetPhase (Phase phase) {
			this.phase = phase;
		}
	}

	static public class MoveZoneCards extends GameServerMessage implements PlayerMessage {
		public int fromZoneID;
		public int count;
		public int[] oldGameCardIDs;
		public Zone toZone;
		public GameCard newGameCards[];
	}

	static public class DrawCard extends GameServerMessage implements PlayerMessage {
	}

	static public class TapCard extends GameServerMessage implements PlayerMessage {
		public int gameCardID;
	}
}
