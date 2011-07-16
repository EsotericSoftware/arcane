
package arcane.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import arcane.Card;
import arcane.network.GameCard;
import arcane.network.MessagePanelType;
import arcane.network.Phase;
import arcane.network.Player;
import arcane.network.Zone;
import arcane.network.Network.Chat;
import arcane.network.Network.DrawCard;
import arcane.network.Network.GameServerMessage;
import arcane.network.Network.GivePriority;
import arcane.network.Network.LeaveGame;
import arcane.network.Network.MoveZoneCards;
import arcane.network.Network.PassPriority;
import arcane.network.Network.SetMessage;
import arcane.network.Network.SetPhase;
import arcane.network.Network.SetPriorityStops;
import arcane.network.Network.StartGame;
import arcane.network.Network.TapCard;
import arcane.util.Util;

import com.captiveimagination.jgn.event.DynamicMessageAdapter;
import com.captiveimagination.jgn.event.DynamicMessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public class GameServer extends DynamicMessageAdapter {
	private Server server;
	private Game game;
	private LinkedList<Player> turns = new LinkedList<Player>();
	private Object priorityLock = new Object();
	private Player priorityPlayer;
	private Set<Player> passedPlayers = new HashSet<Player>();
	private LinkedList<StackFrame> stack = new LinkedList<StackFrame>();
	private Phase phase;
	private ZoneManager zoneManager = new ZoneManager(this);

	public GameServer (final Server server, final Game game) {
		this.server = server;
		this.game = game;

		Thread thread = new Thread(new Runnable() {
			public void run () {
				initialize();
				try {
					while (true) {
						if (turns.isEmpty()) turns.addAll(game.getPlayers());
						for (Player turnPlayer : turns)
							processTurn(turnPlayer);
					}
				} finally {
					gameComplete();
				}
			}
		}, "Game" + game.getId());
		thread.setDaemon(true);
		thread.start();
	}

	private void initialize () {
		server.addMessageListener(GameServer.this);
		System.out.println("Game started: " + game);
		StartGame startGame = new StartGame();
		startGame.lobbyGame = game.getLobbyGame();
		sendToGame(startGame);

		for (Player player : game.getPlayers()) {
			List<GameCard> libraryCards = new ArrayList<GameCard>();
			for (Card card : game.getLibrary(player.getId()))
				libraryCards.add(new GameCard(card, player));
			Collections.shuffle(libraryCards);
			Zone library = zoneManager.getZone(player, "Library");
			library.setViewable(false);
			zoneManager.moveGameCards(null, null, library, libraryCards);

			Zone play = zoneManager.getZone(player, "Play");
			zoneManager.moveGameCards(null, null, play, new ArrayList<GameCard>());

			Zone graveyard = zoneManager.getZone(player, "Graveyard");
			zoneManager.moveGameCards(null, null, graveyard, new ArrayList<GameCard>());

			Zone hand = zoneManager.getZone(player, "Hand");
			hand.setPublic(false);
			List<GameCard> handCards = new ArrayList<GameCard>();
			for (int i = 0; i < 7; i++)
				handCards.add(libraryCards.get(i));
			zoneManager.moveGameCards(library, handCards, hand, handCards);
		}
	}

	private void processTurn (Player turnPlayer) {
		sendToGame(new Chat(turnPlayer.getName() + "'s turn."));
		// Determine play order.
		List<Player> priortyOrder = new ArrayList<Player>();
		int insertIndex = 0;
		for (Player player : game.getPlayers()) {
			if (player == turnPlayer) insertIndex = 0;
			priortyOrder.add(insertIndex++, player);
		}
		// Reset turn.
		phase = Phase.untap;
		passedPlayers.clear();
		passedPlayers.addAll(game.getPlayers());
		// * Untap step.
		// Go through each phase until the turn is over.
		while (true) {
			// If all players have passed since a frame was put on the stack...
			if (passedPlayers.size() >= game.getPlayers().size()) {
				if (stack.isEmpty()) {
					// * Mana burn.
					// Change phase.
					phase = Phase.getNextPhase(phase);
					if (phase == null) break;
					// * Handle first strike.
					// * Skip declare blockers and combat damage steps if there are no attackers.
					sendToGame(new SetPhase(phase));
					// * Phase triggers.
				} else {
					// * Resolve stack frame.
				}
			}
			for (Player player : priortyOrder) {
				// Auto pass if the stack is empty and there is no stop set.
				if (stack.isEmpty() && !player.getPriorityStops().contains(phase) && !player.getPriorityStops().isEmpty()) { // *
					// Remove
					// isEmpty
					// check.
					passedPlayers.add(player);
					continue;
				}
				// * Check state based effects right before giving priority.
				synchronized (priorityLock) {
					priorityPlayer = player;
					sendToGameExcept(new SetMessage("Waiting for " + player.getName() + ".", MessagePanelType.none), player);
					server.sendToPlayer(new GivePriority(), player);
					while (priorityPlayer != null)
						Util.wait(priorityLock);
				}
			}
		}
	}

	// ------------- Messages -------------

	public void messageReceived (TapCard message) {
		// BOZO - Only allow players to tap their own cards.
		GameCard gameCard = zoneManager.getGameCard(message.gameCardID);
		if( message.getPlayerId() == gameCard.getController().getId() )
			sendToGame(message);
	}
	
	public void messageReceived (DrawCard message) {
		Zone library = zoneManager.getZone(message.getPlayerId(), "Library");
		Zone hand = zoneManager.getZone(message.getPlayerId(), "Hand");
		List<GameCard> libraryCards = zoneManager.getGameCards(library);
		if (libraryCards.isEmpty()) return;
		ArrayList<GameCard> c = new ArrayList<GameCard>();
		c.add(libraryCards.get(0));
		zoneManager.moveGameCards(library, c, hand, c);
	}

	public void messageReceived (MoveZoneCards message) {
		Zone fromZone = zoneManager.getZone(message.fromZoneID);
		if (fromZone.getPlayerID() != message.getPlayerId()) return;
		zoneManager.moveGameCards(fromZone, Arrays.asList(message.newGameCards), message.toZone, Arrays
			.asList(message.newGameCards));
	}

	public void messageReceived (PassPriority message) {
		synchronized (priorityLock) {
			if (priorityPlayer == null || priorityPlayer.getId() != message.getPlayerId()) return;
			passedPlayers.add(server.getPlayer(message));
			priorityPlayer = null;
			priorityLock.notifyAll();
		}
	}

	public void messageReceived (SetPriorityStops message) {
		server.getPlayer(message).setPriorityStops(new HashSet<Phase>(Arrays.asList(message.priorityStops)));
	}

	public void messageReceived (Chat message) {
		sendToGame(message);
	}

	public void messageReceived (LeaveGame message) {
		playerDisconnected(server.getPlayer(message));
	}

	// ------------------------------------

	protected void gameComplete () {
		server.removeMessageListener(this);
		System.out.println("Game complete: " + this);
		sendToGame(new Chat("Game complete."));
	}

	public void playerDisconnected (Player player) {
		if (!game.getPlayers().contains(player)) return;
		game.getPlayers().remove(player);
		System.out.println(player + " left game: " + game);
		if (game.getPlayers().isEmpty())
			gameComplete();
		else
			sendToGame(new Chat(player.getName() + " has left the game."));
	}

	public <T extends Message & PlayerMessage> void sendToGame (T message) {
		for (Player player : game.getPlayers())
			server.sendToPlayer(message, player.getId());
	}

	public <T extends Message & PlayerMessage> void sendToPlayer (T message, Player player) {
		server.sendToPlayer(message, player.getId());
	}

	public <T extends Message & PlayerMessage> void sendToGameExcept (T message, Player... exceptPlayers) {
		outer: //
		for (Player player : game.getPlayers()) {
			for (Player exceptPlayer : exceptPlayers)
				if (player.equals(exceptPlayer)) continue outer;
			server.sendToPlayer(message, player.getId());
		}
	}

	public void handle (MESSAGE_EVENT type, Message mess, DynamicMessageListener listener) {
		// Must be a GameMessage with this game's ID and sent by a player in this game.
		if (!(mess instanceof GameServerMessage)) return;
		GameServerMessage message = (GameServerMessage)mess;
		if (message.gameID != game.getId()) return;
		if (!game.getPlayers().contains(server.getPlayer(message))) return;
		super.handle(type, message, listener);
	}

	public Game getGame () {
		return game;
	}

	public String toString () {
		return game.toString();
	}
}
