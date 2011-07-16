
package arcane.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.Hashtable;
import java.util.Map;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.network.Network;
import arcane.network.Player;
import arcane.network.Network.RegisterPlayer;
import arcane.server.ui.ServerFrame;
import arcane.util.Util;

import com.captiveimagination.jgn.JGNConfig;
import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.captiveimagination.jgn.event.DynamicMessageAdapter;
import com.captiveimagination.jgn.event.DynamicMessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public class Server extends JGNServer {
	private Map<Short, Player> players = new Hashtable<Short, Player>();
	private LobbyServer lobbyServer = new LobbyServer(this);

	public Server (SocketAddress reliableAddress, SocketAddress fastAddress) throws IOException {
		super(reliableAddress, fastAddress);

		Thread thread = new Thread(new Runnable() {
			public void run () {
				try {
					MulticastSocket socket = new MulticastSocket(Network.getPort());
					socket.setSoTimeout(0);
					DatagramPacket packet = new DatagramPacket(new byte[1], 1);
					while (!socket.isClosed() && isAlive()) {
						socket.receive(packet);
						if (packet.getLength() != 1) continue;
						if (packet.getData()[0] == -128) continue; // Ignore packets broadcast by this server.
						Util.sleep(1000);
						Util.broadcast(new byte[] {-128}, Network.getPort());
					}
				} catch (IOException ex) {
					throw new ArcaneException("Error running broadcast server.", ex);
				}
			}
		}, "BroadcastReceiver");
		thread.setDaemon(true);
		thread.start();

		addClientConnectionListener(new JGNConnectionListener() {
			public void connected (JGNConnection connection) {
			}

			public void disconnected (JGNConnection connection) {
				short playerId = connection.getPlayerId();
				Player player = getPlayer(playerId);
				if (player != null) {
					lobbyServer.playerDisconnected(player);
					players.remove(playerId);
				}
				if (player != null) System.out.println(player + " disconnected.");
			}
		});

		addMessageListener(new DynamicMessageAdapter() {
			public void handle (MESSAGE_EVENT type, Message message, DynamicMessageListener listener) {
				// Discard messages from unknown players, except for RegisterPlayer.
				if (!(message instanceof PlayerMessage)) return;
				if (!(message instanceof RegisterPlayer) && getPlayer(message.getPlayerId()) == null) return;
				super.handle(type, message, listener);
			}

			public void messageReceived (RegisterPlayer message) {
				Player player = message.player;
				if (!Arcane.version.equals(message.version)) {
					System.out.println(player + " rejected, incorrect version: " + message.version);
					RegisterPlayer replyMessage = new RegisterPlayer();
					replyMessage.version = Arcane.version;
					sendToPlayer(replyMessage, message.getPlayerId());
					return;
				}
				player.setId(message.getPlayerId());
				players.put(player.getId(), player);
				System.out.println(player + " connected.");
			}
		});
	}

	public <T extends Message & PlayerMessage> void sendToPlayer (T message, Player player) {
		super.sendToPlayer(message, player.getId());
	}

	public Player getPlayer (short playerId) {
		return players.get(playerId);
	}

	public <T extends Message & PlayerMessage> Player getPlayer (T message) {
		return players.get(message.getPlayerId());
	}

	public void close () {
		try {
			super.close();
		} catch (IOException ignored) {
		}
	}

	public boolean areGamesInProgress () {
		return lobbyServer.areGamesInProgress();
	}

	public static void main (String[] args) throws Exception {
		JGNConfig.ensureJGNConfigured();
		Arcane.setup("data/server.properties", "server.log", false);
		Network.register();
		ServerFrame server = new ServerFrame();
		server.setVisible(true);
		server.start();
	}
}
