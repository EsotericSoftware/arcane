
package arcane.server;

import java.util.concurrent.ConcurrentLinkedQueue;

import arcane.network.LobbyGame;
import arcane.network.Player;
import arcane.network.Network.CreateLobbyGame;
import arcane.network.Network.EnterLobby;
import arcane.network.Network.GameListUpdate;
import arcane.network.Network.JoinLobbyGame;
import arcane.network.Network.LeaveLobbyGame;
import arcane.network.Network.PlayerListUpdate;
import arcane.network.Network.StartGame;

import com.captiveimagination.jgn.event.DynamicMessageAdapter;
import com.captiveimagination.jgn.event.DynamicMessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public class LobbyServer extends DynamicMessageAdapter {
	static private short nextGameID = 0;

	private Server server;
	private ConcurrentLinkedQueue<Player> players = new ConcurrentLinkedQueue<Player>();
	private ConcurrentLinkedQueue<Game> games = new ConcurrentLinkedQueue<Game>();
	private ConcurrentLinkedQueue<GameServer> gameServers = new ConcurrentLinkedQueue<GameServer>();

	public LobbyServer (Server server) {
		this.server = server;
		server.addMessageListener(this);
	}

	public void messageReceived (EnterLobby message) {
		Player player = server.getPlayer(message);
		if (players.contains(player)) {
			updatePlayerList(player.getId());
			updateGameList(player.getId());
			return;
		}
		players.add(player);
		updatePlayerList();
		updateGameList();
		System.out.println(player + " joined the lobby.");
	}

	public void messageReceived (CreateLobbyGame message) {
		Player player = server.getPlayer(message);
		leaveLobbyGame(player);
		Game game = new Game();
		game.setGameID(nextGameID++);
		games.add(game);
		joinGame(game, player, message.mainCards, message.sideCards);
		System.out.println(player + " created game: " + game);
	}

	public void messageReceived (JoinLobbyGame message) {
		Player player = server.getPlayer(message);
		leaveLobbyGame(player);
		for (Game game : games) {
			if (game.getId() == message.gameID) {
				joinGame(game, player, message.mainCards, message.sideCards);
				System.out.println(player + " joined game: " + game);
				break;
			}
		}
	}

	private void joinGame (Game game, Player player, int[] mainCards, int[] sideCards) {
		if (!game.addPlayer(player, mainCards, sideCards)) {
			JoinLobbyGame message = new JoinLobbyGame();
			message.gameID = (short)-1;
			server.sendToPlayer(message, player);
			return;
		}

		JoinLobbyGame message = new JoinLobbyGame();
		message.setPlayerId(player.getId());
		message.gameID = game.getId();
		sendToLobby(message);

		updateGameList();
	}

	public void messageReceived (LeaveLobbyGame message) {
		leaveLobbyGame(server.getPlayer(message));
	}

	private void leaveLobbyGame (Player player) {
		for (Game game : games) {
			if (game.getPlayers().remove(player)) {
				System.out.println(player + " left game: " + game);
				if (game.getPlayers().size() == 0) {
					System.out.println("Game abandoned: " + game);
					games.remove(game);
				}
			}
		}
		updateGameList();
	}

	public void messageReceived (StartGame message) {
		Player player = server.getPlayer(message);
		for (Game game : games) {
			if (game.getPlayers().contains(player)) {
				System.out.println(player + " started game: " + game);
				// Remove game and all players in the game from the lobby.
				games.remove(game);
				for (Player gamePlayer : game.getPlayers()) {
					players.remove(gamePlayer);
					System.out.println(gamePlayer + " left the lobby.");
				}
				updatePlayerList();
				updateGameList();
				// Start the game.
				gameServers.add(new GameServer(server, game) {
					protected void gameComplete () {
						super.gameComplete();
						gameServers.remove(this);
					}
				});
				return;
			}
		}
	}

	public void playerDisconnected (Player player) {
		leaveLobbyGame(player);
		if (players.remove(player)) System.out.println(player + " left the lobby.");
		updatePlayerList();
		updateGameList();
		for (GameServer gameServer : gameServers)
			gameServer.playerDisconnected(player);
	}

	private void updatePlayerList () {
		PlayerListUpdate message = new PlayerListUpdate();
		String[] playerNames = new String[players.size()];
		int i = 0;
		for (Player player : players)
			playerNames[i++] = player.getName();
		message.playerNames = playerNames;
		sendToLobby(message);
	}

	private void updateGameList () {
		GameListUpdate message = new GameListUpdate();
		LobbyGame[] lobbyGames = new LobbyGame[games.size()];
		int i = 0;
		for (Game game : games)
			lobbyGames[i++] = game.getLobbyGame();
		message.lobbyGames = lobbyGames;
		sendToLobby(message);
	}

	private void updatePlayerList (short playerID) {
		PlayerListUpdate message = new PlayerListUpdate();
		String[] playerNames = new String[players.size()];
		int i = 0;
		for (Player player : players)
			playerNames[i++] = player.getName();
		message.playerNames = playerNames;
		server.sendToPlayer(message, playerID);
	}

	private void updateGameList (short playerID) {
		GameListUpdate message = new GameListUpdate();
		LobbyGame[] lobbyGames = new LobbyGame[games.size()];
		int i = 0;
		for (Game game : games)
			lobbyGames[i++] = game.getLobbyGame();
		message.lobbyGames = lobbyGames;
		server.sendToPlayer(message, playerID);
	}

	private <T extends Message & PlayerMessage> void sendToLobby (T message) {
		for (Player player : players)
			server.sendToPlayer(message, player.getId());
	}

	public boolean areGamesInProgress () {
		return !gameServers.isEmpty();
	}

	public void handle (MESSAGE_EVENT type, Message message, DynamicMessageListener listener) {
		// Discard messages from unknown players.
		if (!(message instanceof PlayerMessage)) return;
		if (server.getPlayer(message.getPlayerId()) == null) return;
		super.handle(type, message, listener);
	}
}
