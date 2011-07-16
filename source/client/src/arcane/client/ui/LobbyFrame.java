
package arcane.client.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.DecklistFile;
import arcane.client.Client;
import arcane.client.GameClient;
import arcane.network.LobbyGame;
import arcane.network.Network;
import arcane.network.Network.CreateLobbyGame;
import arcane.network.Network.EnterLobby;
import arcane.network.Network.GameListUpdate;
import arcane.network.Network.JoinLobbyGame;
import arcane.network.Network.LeaveLobbyGame;
import arcane.network.Network.PlayerListUpdate;
import arcane.network.Network.RegisterPlayer;
import arcane.network.Network.StartGame;
import arcane.network.Player;
import arcane.ui.ArcaneFrame;
import arcane.util.Loader;
import arcane.util.Util;

import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.event.DynamicMessageAdapter;
import com.captiveimagination.jgn.event.MessageListener;

public class LobbyFrame extends ArcaneFrame {
	private Client client;
	private Player localPlayer;
	private MessageListener messageListener;
	private JGNConnectionListener disconnectListener;
	private short joinedGameID = -1;
	private JMenuItem connectMenuItem;
	private JMenuItem autoConnectMenuItem;
	private JMenuItem disconnectMenuItem;

	public LobbyFrame () {
		this(null, null);
	}

	public LobbyFrame (Client existingClient, Player existingLocalPlayer) {
		setTitle("Lobby - Arcane - v" + Arcane.version);

		initializeMenus();
		initializeComponents();
		initializeEvents();
		loadPreferences();

		getContentPane().setVisible(false);

		messageListener = new DynamicMessageAdapter() {
			public void messageReceived (RegisterPlayer message) {
				JOptionPane.showMessageDialog(LobbyFrame.this, "You are not running the correct version of Arcane to connect to "
					+ "this server.\nYour version: " + Arcane.version + "\nServer version: " + message.version, //
					"Incorrect Version", JOptionPane.WARNING_MESSAGE);
				disconnect();
			}

			public void messageReceived (final PlayerListUpdate message) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						int selectedIndex = playersList.getSelectedIndex();
						playersListModel.removeAllElements();
						for (String playerName : message.playerNames)
							playersListModel.addElement(playerName);
						if (selectedIndex < message.playerNames.length) playersList.setSelectedIndex(selectedIndex);
					}
				});
			}

			public void messageReceived (final GameListUpdate message) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						int selectedIndex = gamesList.getSelectedIndex();
						gamesListModel.removeAllElements();
						startButton.setEnabled(false);
						for (LobbyGame lobbyGame : message.lobbyGames) {
							gamesListModel.addElement(new GameEntry(lobbyGame));
							if (lobbyGame.getId() == joinedGameID && lobbyGame.getPlayers().length > 1) startButton.setEnabled(true);
						}
						if (selectedIndex < message.lobbyGames.length) gamesList.setSelectedIndex(selectedIndex);
					}
				});
			}

			public void messageReceived (JoinLobbyGame message) {
				if (message.gameID == -1) {
					JOptionPane.showMessageDialog(LobbyFrame.this, "The chosen decklist is not valid for the game.",
						"Invalid Decklist", JOptionPane.WARNING_MESSAGE);
					return;
				}
				if (message.getPlayerId() == client.getPlayerId()) {
					joinedGameID = message.gameID;
					System.out.println("Joined game.");
					joinedGame(joinedGameID);
				} else if (joinedGameID == message.gameID) {
					// Sound.play("joinGame");
				}
			}

			public void messageReceived (StartGame message) {
				Client openClient = client;
				// Disconnect the listeners and set to null so exit() won't close it.
				client.removeMessageListener(messageListener);
				client.removeServerConnectionListener(disconnectListener);
				client = null;
				new GameClient(openClient, localPlayer, message.lobbyGame);
				exit();
			}
		};

		disconnectListener = new JGNConnectionListener() {
			public void disconnected (JGNConnection connection) {
				JOptionPane.showMessageDialog(LobbyFrame.this, "The connection with the server has been lost.", "Connection Lost",
					JOptionPane.WARNING_MESSAGE);
				disconnect();
			}

			public void connected (JGNConnection connection) {
			}
		};

		if (existingClient != null && existingClient.getServerConnection().isConnected())
			setClient(existingClient, existingLocalPlayer);
		else {
			if (prefs.getBoolean("lobby.startup.reconnect", false) && prefs.get("lobby.host", null) != null) {
				if (prefs.get("lobby.last.connect", "connect").equals("connect"))
					connect(prefs.get("lobby.host", null), prefs.get("lobby.name", null));
				else
					autoConnect(prefs.get("lobby.name", null));
			}
		}
	}

	private void loadPreferences () {
		prefs.loadFrameState(this, "lobby", 480, 360);
	}

	protected void savePreferences () {
		prefs.saveFrameState(this, "lobby");
	}

	public boolean exit () {
		disconnect();
		prefs.save();
		setVisible(false);
		dispose();
		return true;
	}

	private void disconnect () {
		getContentPane().setVisible(false);
		connectMenuItem.setVisible(true);
		autoConnectMenuItem.setVisible(true);
		disconnectMenuItem.setVisible(false);

		createGameButton.setVisible(true);
		startButton.setVisible(false);
		joinGameButton.setVisible(true);
		leaveGameButton.setVisible(false);
		joinedGameID = -1;

		if (client != null) {
			client.removeMessageListener(messageListener);
			client.removeServerConnectionListener(disconnectListener);
			if (client.getServerConnection().isConnected()) System.out.println("Disconnected from server.");
			client.close();
			client = null;
		}
	}

	public void connect (String value, String name) {
		if (value == null || value.trim().length() == 0) {
			value = (String)JOptionPane.showInputDialog(null, "Enter the server host name or IP address.", "Connect",
				JOptionPane.QUESTION_MESSAGE, null, null, prefs.get("lobby.host", "localhost"));
		}
		if (value == null || value.trim().length() == 0) return;
		final String host = value.trim();
		prefs.set("lobby.host", host);
		class ConnectLoader extends Loader {
			public Client client;

			public ConnectLoader () {
				super("Client - Arcane v" + Arcane.version);
			}

			public void load () {
				dialog.setMessage("Connecting to: " + host);
				try {
					client = new Client();
					if (isCancelled()) return;
					client.connectAndWait(host);
				} catch (Exception ex) {
					if (isCancelled()) return;
					cancel();
					arcane.log(ex);
				}
				if (isCancelled() && client != null) {
					client.close();
					client = null;
				}
			}
		}
		ConnectLoader loader = new ConnectLoader();
		loader.start("ConnectLoader");
		if (loader.failed()) {
			JOptionPane.showMessageDialog(LobbyFrame.this, "Unable to connect to host: " + host, "Connect",
				JOptionPane.WARNING_MESSAGE);
			return;
		}

		Client client = loader.client;
		System.out.println("Connected to server: " + value);

		if (name == null || name.trim().length() == 0) {
			name = (String)JOptionPane.showInputDialog(null, "Enter your name.", "Connect", JOptionPane.QUESTION_MESSAGE, null,
				null, prefs.get("lobby.name", ""));
		}
		if (name == null || name.trim().length() == 0) {
			client.close();
			return;
		}
		name = name.trim();
		prefs.set("lobby.name", name);

		prefs.save();

		Player player = new Player();
		player.setId(client.getPlayerId());
		player.setName(name);

		RegisterPlayer message = new RegisterPlayer();
		message.version = Arcane.version;
		message.player = player;
		client.sendToServer(message);
		System.out.println("Registered player: " + player.getName());

		setClient(client, player);
	}

	private void setClient (Client client, Player localPlayer) {
		this.client = client;
		this.localPlayer = localPlayer;

		client.addMessageListener(messageListener);
		client.addServerConnectionListener(disconnectListener);
		this.client = client;
		client.sendToServer(new EnterLobby());
		System.out.println("Joined lobby.");

		connectMenuItem.setVisible(false);
		autoConnectMenuItem.setVisible(false);
		disconnectMenuItem.setVisible(true);
		getContentPane().setVisible(true);
	}

	private void autoConnect (String name) {
		class AutoConnectLoader extends Loader {
			public String host;

			public AutoConnectLoader () {
				super("Client - Arcane v" + Arcane.version);
			}

			public void load () {
				dialog.setMessage("Searching for LAN servers...");
				try {
					Util.broadcast(new byte[] {0}, Network.getPort());
					MulticastSocket socket = new MulticastSocket(Network.getPort());
					socket.setSoTimeout(5000);
					DatagramPacket packet = new DatagramPacket(new byte[0], 0);
					try {
						socket.receive(packet);
						host = ((InetSocketAddress)packet.getSocketAddress()).getHostName();
					} catch (SocketTimeoutException ex) {
						cancel();
					}
				} catch (IOException ex) {
					throw new ArcaneException("Error during LAN server search.", ex);
				}
			}
		}
		AutoConnectLoader loader = new AutoConnectLoader();
		loader.start("AutoConnectLoader");
		if (loader.failed()) {
			JOptionPane.showMessageDialog(LobbyFrame.this, "No LAN servers could be found.", "Connect", JOptionPane.WARNING_MESSAGE);
			return;
		}
		connect(loader.host, name);
	}

	public void createGame (DecklistFile decklistFile) {
		if (decklistFile == null) return;
		try {
			decklistFile.open();
		} catch (IOException ex) {
			throw new ArcaneException(ex);
		}

		createGameButton.setVisible(false);
		startButton.setVisible(true);
		startButton.setEnabled(false);
		joinGameButton.setVisible(false);
		leaveGameButton.setVisible(true);

		CreateLobbyGame message = new CreateLobbyGame();
		message.mainCards = decklistFile.getCardIDs(false);
		message.sideCards = decklistFile.getCardIDs(true);
		client.sendToServer(message);
	}

	public void joinGame (short gameID, DecklistFile decklistFile) {
		joinedGameID = gameID;

		JoinLobbyGame message = new JoinLobbyGame();
		message.gameID = joinedGameID;
		message.mainCards = decklistFile.getCardIDs(false);
		message.sideCards = decklistFile.getCardIDs(true);
		client.sendToServer(message);
	}

	public void startGame () {
		client.sendToServer(new StartGame());
	}

	protected void joinedGame (short joinedGameID) {
	}

	private void initializeEvents () {
		gamesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent evt) {
				GameEntry entry = (GameEntry)gamesList.getSelectedValue();
				joinGameButton.setEnabled(entry != null && entry.lobbyGame.getPlayers().length < 2);
			}
		});
		createGameButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				DecklistFile decklistFile = DecklistFile.open(LobbyFrame.this, null);
				createGame(decklistFile);
			}
		});
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				startButton.setEnabled(false);
				leaveGameButton.setEnabled(false);
				startGame();
			}
		});
		joinGameButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				DecklistFile decklistFile = DecklistFile.open(LobbyFrame.this, null);
				if (decklistFile == null) return;
				try {
					decklistFile.open();
				} catch (IOException ex) {
					throw new ArcaneException(ex);
				}

				createGameButton.setVisible(false);
				startButton.setVisible(true);
				startButton.setEnabled(true);
				joinGameButton.setVisible(false);
				leaveGameButton.setVisible(true);

				joinGame(((GameEntry)gamesList.getSelectedValue()).lobbyGame.getId(), decklistFile);
			}
		});
		leaveGameButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				createGameButton.setVisible(true);
				startButton.setVisible(false);
				joinGameButton.setVisible(true);
				leaveGameButton.setVisible(false);

				joinedGameID = -1;

				client.sendToServer(new LeaveLobbyGame());
				System.out.println("Left game.");
			}
		});
	}

	private void initializeComponents () {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Dimension screen = getToolkit().getScreenSize();
		setLocation((int)screen.getWidth() / 2 - getWidth() / 2, (int)screen.getHeight() / 2 - getHeight() / 2);

		GridBagLayout layout = new GridBagLayout();
		this.setLayout(layout);
		{
			JPanel outerSection = new JPanel();
			GridLayout gameSectionLayout = new GridLayout(1, 2);
			gameSectionLayout.setColumns(2);
			gameSectionLayout.setHgap(3);
			outerSection.setLayout(gameSectionLayout);
			this.add(outerSection, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 6, 6, 6), 0, 0));
			outerSection.setEnabled(false);
			{
				JPanel gamesSection = new JPanel();
				outerSection.add(gamesSection);
				gamesSection.setLayout(new GridBagLayout());
				gamesSection.setBorder(BorderFactory.createTitledBorder("Games"));
				{
					createGameButton = new JButton();
					gamesSection.add(createGameButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(0, 6, 6, 0), 0, 0));
					createGameButton.setText("Create");
				}
				{
					startButton = new JButton();
					gamesSection.add(startButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(0, 6, 6, 6), 0, 0));
					startButton.setText("Start");
					startButton.setVisible(false);
				}
				{
					joinGameButton = new JButton();
					gamesSection.add(joinGameButton, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(0, 0, 6, 6), 0, 0));
					joinGameButton.setText("Join");
					joinGameButton.setEnabled(false);
				}
				{
					leaveGameButton = new JButton();
					gamesSection.add(leaveGameButton, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(0, 0, 6, 6), 0, 0));
					leaveGameButton.setText("Leave");
					leaveGameButton.setVisible(false);
				}
				{
					JScrollPane gamesScroll = new JScrollPane();
					gamesSection.add(gamesScroll, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(0, 6, 6, 6), 0, 0));
					{
						gamesListModel = new DefaultComboBoxModel();
						gamesList = new JList();
						gamesScroll.setViewportView(gamesList);
						gamesList.setModel(gamesListModel);
					}
				}
			}
			{
				JPanel playersSection = new JPanel();
				outerSection.add(playersSection);
				GridBagLayout playersSectionLayout = new GridBagLayout();
				playersSection.setLayout(playersSectionLayout);
				playersSection.setBorder(BorderFactory.createTitledBorder("Players"));
				{
					JScrollPane playersScroll = new JScrollPane();
					playersSection.add(playersScroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(0, 6, 6, 6), 0, 0));
					{
						playersListModel = new DefaultComboBoxModel();
						playersList = new JList();
						playersList.setFocusable(false);
						playersScroll.setViewportView(playersList);
						playersList.setModel(playersListModel);
					}
				}
			}
		}
	}

	protected void initializeMenus () {
		initializeMenuBar();
		initializeFileMenu();
		initializeSettingsMenu();
		initializeToolsMenu();
	}

	protected void initializeFileMenu () {
		super.initializeFileMenu();
		fileMenu.add(new JPopupMenu.Separator(), 0);
		{
			JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Reconnect on startup");
			menuItem.setModel(new JToggleButton.ToggleButtonModel() {
				public boolean isSelected () {
					return prefs.getBoolean("lobby.startup.reconnect", false);
				}

				public void setSelected (boolean b) {
					super.setSelected(b);
					prefs.set("lobby.startup.reconnect", b);
				}
			});
			fileMenu.add(menuItem, 0);
		}
		{
			autoConnectMenuItem = new JMenuItem("Search for LAN servers...", KeyEvent.VK_S);
			autoConnectMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
			fileMenu.add(autoConnectMenuItem, 0);
			autoConnectMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					autoConnect(null);
					if (client != null) prefs.set("lobby.last.connect", "auto");
				}
			});
		}
		{
			disconnectMenuItem = new JMenuItem("Disconnect");
			fileMenu.add(disconnectMenuItem, 0);
			disconnectMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					disconnect();
				}
			});
			disconnectMenuItem.setVisible(false);
		}
		{
			connectMenuItem = new JMenuItem("Connect...", KeyEvent.VK_C);
			connectMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK));
			fileMenu.add(connectMenuItem, 0);
			connectMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					connect(null, null);
					if (client != null) prefs.set("lobby.last.connect", "connect");
				}
			});
		}
	}

	private JButton joinGameButton;
	private JButton leaveGameButton;
	private JButton createGameButton;
	private JButton startButton;
	private JList playersList;
	private DefaultComboBoxModel playersListModel;
	private JList gamesList;
	private DefaultComboBoxModel gamesListModel;

	private class GameEntry {
		public LobbyGame lobbyGame;

		public GameEntry (LobbyGame game) {
			this.lobbyGame = game;
		}

		public String toString () {
			StringBuilder buffer = new StringBuilder(100);
			for (Player player : lobbyGame.getPlayers()) {
				buffer.append(player.getName());
				buffer.append(", ");
			}
			if (buffer.length() > 0) buffer.setLength(buffer.length() - 2);
			return buffer.toString();
		}
	}
}
