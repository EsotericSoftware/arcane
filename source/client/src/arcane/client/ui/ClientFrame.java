
package arcane.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.Card;
import arcane.client.Client;
import arcane.client.GameClient;
import arcane.client.ui.util.Animation;
import arcane.client.ui.util.Arrow;
import arcane.client.ui.util.CardArea;
import arcane.client.ui.util.CardPanel;
import arcane.client.ui.util.CardPanelMouseListener;
import arcane.network.LobbyGame;
import arcane.network.Player;
import arcane.rulesviewer.ui.RulesViewer;
import arcane.ui.ArcaneFrame;
import arcane.ui.CardInfoPane;
import arcane.ui.util.MessageFrame;
import arcane.ui.util.SplitPane;
import arcane.ui.util.SplitPane.SplitPaneType;
import arcane.util.Loader;

public class ClientFrame extends ArcaneFrame {
	static private final float ARROW_OFFSET = 0.08f;
	static private final int MESSAGE_HEIGHT = 100;
	static private final int PHASE_WIDTH = 49;

	static private List<ClientFrame> instances = new ArrayList<ClientFrame>();

	private GameClient gameClient;
	private MessageFrame helpFrame;
	private JPanel glassPane;
	private JPanel playersSection;
	private ViewPanel viewPanel;
	private CardInfoPane cardInfoPanel;
	private MessagePanel messagePanel;
	private StackArea stackArea;
	private PhasePanel phasePanel;
	private ChatPanel chatPanel;
	private HandArea handArea;
	private ForcePanel forcePanel;

	private CardPanel overCardPanel;
	private List<PlayerPanel> playerPanels = new ArrayList<PlayerPanel>();

	protected CardPanelMouseListener cardPanelListener = new CardPanelMouseListener() {
		public void mouseOver (final CardPanel overPanel, MouseEvent evt) {
			if (!evt.isShiftDown()) {
				Card card = arcane.getCard(overPanel.gameCard.card.name);
				if (!cardInfoPanel.isCurrentCard(card)) {
					cardInfoPanel.setCard(card);
					viewPanel.setCardPanel(overPanel);
				}
			}

			if (evt.isControlDown()) Animation.enlargeCard(overPanel, ClientFrame.this, 0);

			if (overPanel == overCardPanel) return;
			overCardPanel = overPanel;

			// BOZO - Add arrows according to actual targets.
			if (!evt.isControlDown()) {
				if (stackArea.cardPanels.isEmpty() || Animation.isShowingEnlargedCard()) return;
				final CardPanel startPanel = stackArea.cardPanels.get(stackArea.cardPanels.size() - 1);
				if (startPanel == overPanel) return;
				addArrow(startPanel, overPanel);
			}
		}

		public void mouseOut (CardPanel panel, MouseEvent evt) {
			clearArrows();
			Animation.shrinkCard();
			overCardPanel = null;
		}

		public void mouseLeftClicked (CardPanel panel, MouseEvent evt) {
			clearArrows();
		}

		public void mouseRightClicked (CardPanel panel, MouseEvent evt) {
		}

		public void mouseMiddleClicked (CardPanel panel, MouseEvent evt) {
			Animation.enlargeCard(panel, ClientFrame.this, 0);
		}

		public void mouseDragged (CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt) {
			clearArrows();
			Animation.shrinkCard();
		}

		public void mouseDragStart (CardPanel dragPanel, MouseEvent evt) {
		}

		public void mouseDragEnd (CardPanel dragPanel, MouseEvent evt) {
		}
	};

	public ClientFrame (final GameClient gameClient) {
		this.gameClient = gameClient;

		setTitle("Client - Arcane v" + Arcane.version);

		Loader loader = new Loader("Client - Arcane v" + Arcane.version) {
			public void load () {
				initializeMenus();
				initializeComponents();
				loadPreferences();

				// BOZO - Support multiple players.
				PlayerPanel localPlayerArea = new PlayerPanel(gameClient.getLocalPlayer(), false, cardPanelListener, gameClient);
				Player[] players = gameClient.getLobbyGame().getPlayers();
				Player opponent = players[0].getId() == gameClient.getLocalPlayer().getId() ? players[1] : players[0];
				PlayerPanel opponentPlayerArea = new PlayerPanel(opponent, true, cardPanelListener, gameClient);

				playerPanels.add(localPlayerArea);
				playerPanels.add(opponentPlayerArea);

				playersSection.setLayout(new GridLayout(2, 1));
				playersSection.add(opponentPlayerArea);
				playersSection.add(localPlayerArea);
			}
		};
		loader.start("ClientLoader");
		if (loader.failed()) {
			dispose();
			throw new ArcaneException("Arcane Client initialization aborted.");
		}

		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke("ctrl pressed CONTROL"), "ctrlDown");
		getRootPane().getActionMap().put("ctrlDown", new AbstractAction("ctrlDown") {
			public void actionPerformed (ActionEvent evt) {
				if (overCardPanel == null) return;
				Animation.enlargeCard(overCardPanel, ClientFrame.this, 0);
			}
		});

		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released CONTROL"), "ctrlUp");
		getRootPane().getActionMap().put("ctrlUp", new AbstractAction("ctrlUp") {
			public void actionPerformed (ActionEvent evt) {
				Animation.shrinkCard();
			}
		});

		instances.add(this);
	}

	public void addArrow (CardPanel startPanel, CardPanel endPanel) {
		Point startPos = SwingUtilities.convertPoint(startPanel.getParent(), startPanel.getCardLocation(), glassPane);
		int startOffset = Math.round(startPanel.getCardWidth() * ARROW_OFFSET);
		if (startPanel.tapped) {
			startPos.x += startPanel.getCardHeight() - startOffset;
			startPos.y += startPanel.getCardHeight() - startOffset;
		} else {
			startPos.x += startPanel.getCardWidth() - startOffset;
			startPos.y += startOffset;
		}
		Point endPos = SwingUtilities.convertPoint(endPanel.getParent(), endPanel.getCardLocation(), glassPane);
		int endOffset = Math.round(endPanel.getCardWidth() * ARROW_OFFSET);
		if (endPanel.tapped) {
			endPos.x += endPanel.getCardHeight() - endOffset;
			endPos.y += (endPanel.getCardHeight() - endPanel.getCardWidth()) + endOffset;
		} else {
			endPos.x += endOffset;
			endPos.y += endOffset;
		}
		Arrow arrow = new Arrow();
		arrow.setArrowLocation(startPos.x, startPos.y, endPos.x, endPos.y);
		glassPane.add(arrow, BorderLayout.CENTER);
		glassPane.doLayout();
	}

	public void clearArrows () {
		glassPane.removeAll();
		glassPane.repaint();
	}

	protected void initializeComponents () {
		helpFrame = new MessageFrame("Help - Client - Arcane v" + Arcane.version);
		helpFrame.setHTML(true);
		helpFrame.addButton("Close");
		helpFrame.setText(ClientFrame.class.getResource("/help-client.html"));

		setLayout(new BorderLayout());

		glassPane = new JPanel() {
			// Allows events through the panel.
			public boolean contains (int x, int y) {
				return false;
			}
		};
		getRootPane().setGlassPane(glassPane);
		glassPane.setVisible(true);
		glassPane.setOpaque(false);
		glassPane.setLayout(new BorderLayout());

		SplitPane mainSplit = new SplitPane(SplitPane.HORIZONTAL_SPLIT);
		add(mainSplit, BorderLayout.CENTER);
		{
			JSplitPane playerSectionSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			mainSplit.add(playerSectionSplit, SplitPane.RIGHT);
			playerSectionSplit.setResizeWeight(0.75);
			playerSectionSplit.setBorder(BorderFactory.createEmptyBorder());
			{
				JPanel handSection = new JPanel();
				handSection.setLayout(new BorderLayout());
				playerSectionSplit.add(handSection, JSplitPane.BOTTOM);
				{
					JPanel forceSection = new JPanel();
					forceSection.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
					forceSection.setMinimumSize(new Dimension());
					forceSection.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.black));
					handSection.add(forceSection, BorderLayout.WEST);
					{
						forcePanel = new ForcePanel(gameClient);
						forceSection.add(forcePanel);
					}
				}
				{
					JScrollPane handScroll = new JScrollPane();
					handScroll.setBorder(BorderFactory.createEmptyBorder());
					handSection.add(handScroll, BorderLayout.CENTER);
					{
						handArea = new HandArea(handScroll, this);
						handArea.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.black));
						handArea.addCardPanelMouseListener(cardPanelListener);
						handScroll.setViewportView(handArea);
					}
				}
			}
			{
				playersSection = new JPanel();
				playerSectionSplit.add(playersSection, JSplitPane.TOP);
			}
		}
		{
			SplitPane previewSplit = new SplitPane(SplitPane.VERTICAL_SPLIT);
			mainSplit.add(previewSplit, SplitPane.LEFT);
			{
				viewPanel = new ViewPanel();
				previewSplit.add(viewPanel, SplitPane.TOP);
			}
			{
				SplitPane cardInfoSplit = new SplitPane(SplitPane.VERTICAL_SPLIT);
				cardInfoSplit.setType(SplitPaneType.percentage);
				previewSplit.add(cardInfoSplit, SplitPane.BOTTOM);
				{
					JScrollPane scrollPane = new JScrollPane();
					cardInfoSplit.add(scrollPane, SplitPane.TOP);
					{
						cardInfoPanel = new CardInfoPane() {
							protected void showRule (String rule) {
								new RulesViewer().showRule(rule);
							}
						};
						scrollPane.setViewportView(cardInfoPanel);
						cardInfoPanel.setShowSingleSet(true);
					}
				}
				{
					SplitPane messageAndStackSplit = new SplitPane(SplitPane.VERTICAL_SPLIT);
					cardInfoSplit.add(messageAndStackSplit, SplitPane.BOTTOM);
					messageAndStackSplit.setType(SplitPaneType.percentage);
					{
						final JPanel messageAndStackSection = new JPanel() {
							public void layout () {
								int width = getWidth();
								int height = getHeight();

								messagePanel.setSize(width, MESSAGE_HEIGHT - 5);

								phasePanel.setLocation(0, MESSAGE_HEIGHT);
								phasePanel.setSize(PHASE_WIDTH, height - MESSAGE_HEIGHT);

								JScrollPane stackScroll = stackArea.getScrollPane();
								stackScroll.setLocation(PHASE_WIDTH, MESSAGE_HEIGHT);
								stackScroll.setSize(width - PHASE_WIDTH, height - MESSAGE_HEIGHT);
								stackArea.layout();
							}
						};
						messageAndStackSection.setMinimumSize(new Dimension());
						messageAndStackSplit.add(messageAndStackSection, SplitPane.TOP);
						{
							messagePanel = new MessagePanel(gameClient);
							messageAndStackSection.add(messagePanel);
						}
						{
							phasePanel = new PhasePanel(gameClient);
							messageAndStackSection.add(phasePanel);
						}
						{
							JScrollPane scrollPane = new JScrollPane() {
								public void layout () {
									//super.doLayout();
									stackArea.setCardWidthMin(messageAndStackSection.getWidth() - PHASE_WIDTH - CardArea.GUTTER_X * 2
										- stackArea.getScrollPane().getVerticalScrollBar().getWidth());
								}
							};
							messageAndStackSection.add(scrollPane);
							scrollPane.setBorder(BorderFactory.createEmptyBorder());
							{
								stackArea = new StackArea(scrollPane);
								scrollPane.setViewportView(stackArea);
								stackArea.setBackground(null);
								stackArea.addCardPanelMouseListener(cardPanelListener);
							}
						}
					}
					{
						chatPanel = new ChatPanel(gameClient);
						messageAndStackSplit.add(chatPanel, SplitPane.BOTTOM);
					}
				}
			}
		}
	}

	protected void initializeMenus () {
		super.initializeMenus();

		fileMenu.add(new JPopupMenu.Separator(), 0);
		{
			JMenuItem menuItem = new JMenuItem("End Game");
			fileMenu.add(menuItem, 0);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					exit();
				}
			});
		}

		{
			JMenu helpMenu = new JMenu();
			menuBar.add(helpMenu);
			helpMenu.setText("Help");
			helpMenu.setMnemonic(KeyEvent.VK_H);
			{
				JMenuItem menuItem = new JMenuItem("Help...", KeyEvent.VK_H);
				menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
				helpMenu.add(menuItem);
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed (ActionEvent evt) {
						helpFrame.setVisible(true);
					}
				});
			}
		}
	}

	private void loadPreferences () {
		prefs.loadFrameState(this, "client", 800, 728);

		SplitPane.setScrollPaneInfo(getContentPane(), prefs.get("client.splitpanes",
			"135,152,0.30637254901960786,0.8798586572438163,"));
	}

	protected void savePreferences () {
		prefs.saveFrameState(this, "client");

		prefs.set("client.splitpanes", SplitPane.getScrollPaneInfo(getContentPane()));
	}

	public CardInfoPane getCardInfoPanel () {
		return cardInfoPanel;
	}

	public MessagePanel getMessagePanel () {
		return messagePanel;
	}

	public StackArea getStackArea () {
		return stackArea;
	}

	public PhasePanel getPhasePanel () {
		return phasePanel;
	}

	public ChatPanel getChatPanel () {
		return chatPanel;
	}

	public HandArea getHandArea () {
		return handArea;
	}

	public GameClient getGameClient () {
		return gameClient;
	}

	public PlayerPanel getPlayerPanel (short playerID) {
		for (PlayerPanel playerPanel : playerPanels)
			if (playerPanel.getPlayer().getId() == playerID) return playerPanel;
		throw new IllegalArgumentException("Player does not exist: " + playerID);
	}

	public boolean exit () {
		boolean exit = super.exit();
		if (exit) {
			instances.remove(this);
		}
		return exit;
	}

	public static void main (String[] args) throws Exception {
		Arcane.setup("data/arcane.properties", "arcane.log", true);
		Player player = new Player();
		player.setName("Nate");
		new GameClient(new Client(), player, new LobbyGame());
	}
}
