
package arcane.client.ui;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import arcane.client.GameClient;
import arcane.client.ManaPool;
import arcane.client.ui.util.CardArea;
import arcane.client.ui.util.CardPanelMouseListener;
import arcane.client.ui.util.PlayArea;
import arcane.network.Player;

public class PlayerPanel extends JPanel {
	static private final int INFO_WIDTH = 129;

	private Player player;
	private boolean isTop;
	private PlayerInfoPanel infoPanel;
	private CardArea graveyardArea;
	private PlayArea playArea;
	private JScrollPane playScroll;
	private ManaPool manaPool = new ManaPool();

	private final GameClient gameClient;

	public PlayerPanel (Player player, boolean isTop, CardPanelMouseListener cardPanelListener, GameClient gameClient) {
		this.player = player;
		this.isTop = isTop;
		this.gameClient = gameClient;
		initializeComponents(cardPanelListener);
	}

	public Player getPlayer () {
		return player;
	}

	private void initializeComponents (CardPanelMouseListener cardPanelListener) {
		if (isTop) setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
		{
			{
				playScroll = new JScrollPane();
				add(playScroll);
				playScroll.setBorder(BorderFactory.createEmptyBorder());
				{
					playArea = new PlayArea(playScroll, gameClient);
					playArea.addCardPanelMouseListener(cardPanelListener);
					playScroll.setViewportView(playArea);
				}
			}
		}
		{
			JScrollPane scrollPane = new JScrollPane() {
				public void layout () {
					//super.doLayout();
					graveyardArea.setCardWidthMin(INFO_WIDTH - CardArea.GUTTER_X * 2
						- graveyardArea.getScrollPane().getVerticalScrollBar().getWidth());
				}
			};
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			if (isTop)
				scrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, Color.black));
			else
				scrollPane.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Color.black));
			add(scrollPane);
			{
				graveyardArea = new CardArea(scrollPane);
				graveyardArea.addCardPanelMouseListener(cardPanelListener);
				graveyardArea.setMaxRows(1);
				graveyardArea.setMaxCoverage(0.94f);
				graveyardArea.setVertical(true);
				scrollPane.setViewportView(graveyardArea);
			}
		}
		{
			infoPanel = new PlayerInfoPanel(player, manaPool);
			add(infoPanel);
			infoPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Color.black));
			infoPanel.setLayout(new GridLayout(3, 1, 5, 5));
		}
	}

	public void layout () {
		int width = getWidth();
		int height = getHeight();
		if (isTop) height--;

		int infoHeight = infoPanel.getPreferredSize().height;
		infoPanel.setSize(INFO_WIDTH, infoHeight);
		if (isTop)
			infoPanel.setLocation(0, 0);
		else
			infoPanel.setLocation(0, height - infoHeight);

		JScrollPane graveyardScroll = graveyardArea.getScrollPane();
		graveyardScroll.setSize(INFO_WIDTH, height - infoHeight);
		if (isTop)
			graveyardScroll.setLocation(0, infoHeight);
		else
			graveyardScroll.setLocation(0, 0);

		playScroll.setSize(width - INFO_WIDTH, height);
		playScroll.setLocation(INFO_WIDTH, 0);
	}

	public PlayerInfoPanel getInfoPanel () {
		return infoPanel;
	}

	public CardArea getGraveyardArea () {
		return graveyardArea;
	}

	public PlayArea getPlayArea () {
		return playArea;
	}
}
