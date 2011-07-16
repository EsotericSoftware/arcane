
package arcane.client;

import javax.swing.JOptionPane;

import arcane.client.ui.ClientFrame;
import arcane.client.ui.LobbyFrame;
import arcane.client.ui.util.Animation;
import arcane.client.ui.util.PlayArea;
import arcane.network.GameCard;
import arcane.network.LobbyGame;
import arcane.network.MessagePanelType;
import arcane.network.Network.Chat;
import arcane.network.Network.GameServerMessage;
import arcane.network.Network.GivePriority;
import arcane.network.Network.LeaveGame;
import arcane.network.Network.SetMessage;
import arcane.network.Network.SetPhase;
import arcane.network.Network.TapCard;
import arcane.network.Player;

import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.event.DynamicMessageAdapter;
import com.captiveimagination.jgn.message.Message;

public class GameClient extends DynamicMessageAdapter {
	private Client client;
	private Player localPlayer;
	private LobbyGame lobbyGame;
	private ClientFrame frame;
	private ZoneListener zoneListener;

	private JGNConnectionListener disconnectListener = new JGNConnectionListener() {
		public void disconnected (JGNConnection connection) {
			JOptionPane.showMessageDialog(frame, "The connection with the server has been lost.", "Connection Lost",
				JOptionPane.WARNING_MESSAGE);
			frame.exit();
		}

		public void connected (JGNConnection connection) {
		}
	};

	public GameClient (final Client client, final Player localPlayer, LobbyGame lobbyGame) {
		this.client = client;
		this.localPlayer = localPlayer;
		this.lobbyGame = lobbyGame;

		frame = new ClientFrame(this) {
			public boolean exit () {
				if (client.getServerConnection().isConnected()) {
					int result = JOptionPane.showConfirmDialog(this, "The game in progress will be terminated.", "Confirm End Game",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if (result != JOptionPane.OK_OPTION) return false;
				}
				boolean exit = super.exit();
				if (exit) {
					if (client.getServerConnection().isConnected()) sendToServer(new LeaveGame());
					client.removeMessageListener(GameClient.this);
					client.removeMessageListener(zoneListener);
					client.removeServerConnectionListener(disconnectListener);
					new LobbyFrame(GameClient.this.client, localPlayer).setVisible(true);
				}
				return exit;
			}
		};
		frame.setVisible(true);
		frame.getChatPanel().addMessage("Game started.");

		zoneListener = new ZoneListener(frame);

		client.addMessageListener(this);
		client.addMessageListener(zoneListener);
		client.addServerConnectionListener(disconnectListener);
	}

	// ------------- Messages -------------

	public void messageReceived (TapCard message) {
		for (Player player : lobbyGame.getPlayers()) {
			PlayArea playArea = frame.getPlayerPanel(player.getId()).getPlayArea();
			int playZoneID = playArea.getZoneID();
			for (GameCard gameCard : zoneListener.getGameCards(playZoneID)) {
				if (gameCard.getId() == message.gameCardID) {
					Animation.tapCardToggle(playArea.getCardPanel(message.gameCardID));
					return;
				}
			}
		}
	}

	public void messageReceived (SetPhase message) {
		if (message.phase == null) System.out.println();
		frame.getPhasePanel().setPhase(message.phase);
	}

	public void messageReceived (GivePriority message) {
		frame.getMessagePanel().setText("You have priority.", MessagePanelType.pass);
	}

	public void messageReceived (SetMessage message) {
		frame.getMessagePanel().setText(message.text, message.inputType);
	}

	public void messageReceived (Chat message) {
		String text = message.text;
		Player player = lobbyGame.getPlayer(message);
		if (player != null) text = player.getName() + ": " + text;
		frame.getChatPanel().addMessage(text);
	}

	// ------------------------------------

	public long sendToServer (Message message) {
		if (message instanceof GameServerMessage) ((GameServerMessage)message).gameID = lobbyGame.getId();
		return client.sendToServer(message);
	}

	public Player getLocalPlayer () {
		return localPlayer;
	}

	public LobbyGame getLobbyGame () {
		return lobbyGame;
	}
}
