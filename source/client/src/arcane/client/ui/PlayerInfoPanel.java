
package arcane.client.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import arcane.ArcaneException;
import arcane.client.ManaPool;
import arcane.network.Player;
import arcane.network.Zone;
import arcane.ui.util.MouseAdapter;
import arcane.ui.util.UI;

public class PlayerInfoPanel extends JPanel {
	static private final int GUTTER = 4;

	private Player player;
	private JScrollPane zoneScroll;
	private JList zoneList;
	private DefaultListModel zoneModel;
	private JLabel nameLabel;
	private JLabel lifeLabel;
	private JLabel manaLabel;
	private JPanel manaPanel;
	private ManaPool manaPool;

	public PlayerInfoPanel (Player player, ManaPool manaPool) {
		this.player = player;
		this.manaPool = manaPool;

		initializeComponents();

		manaPool.addChangeListener(new ManaPool.ChangeListener() {
			public void changed () {
				manaPanel.repaint();
			}
		});

		zoneList.setVisibleRowCount(0);

		int height = 0;
		height += lifeLabel.getPreferredSize().height;
		height += manaPanel.getPreferredSize().height;
		height += zoneList.getPreferredScrollableViewportSize().height;
		setPreferredSize(new Dimension(0, height));
	}

	public void removeZone (int zoneID) {
		for (int i = 0, n = zoneModel.getSize(); i < n; i++) {
			ZoneEntry zoneEntry = (ZoneEntry)zoneModel.get(i);
			if (zoneEntry.id == zoneID) {
				zoneModel.removeElementAt(i);
				return;
			}
		}
	}

	public void updateZone (Zone zone, String text) {
		try {
			for (int i = 0, n = zoneModel.getSize(); i < n; i++) {
				ZoneEntry zoneEntry = (ZoneEntry)zoneModel.get(i);
				if (zoneEntry.id == zone.getId()) {
					zoneEntry.text = text;
					return;
				}
			}

			ZoneEntry zoneEntry = new ZoneEntry();
			zoneEntry.id = zone.getId();
			zoneEntry.text = text;
			zoneModel.addElement(zoneEntry);
			zoneList.setVisibleRowCount(Math.min(6, zoneModel.size()));

			int height = 0;
			height += lifeLabel.getPreferredSize().height;
			height += manaPanel.getPreferredSize().height;
			height += zoneList.getPreferredScrollableViewportSize().height;
			setPreferredSize(new Dimension(0, height));
		} finally {
			SwingUtilities.invokeLater(new Runnable() {
				public void run () {
					invalidate();
					getParent().validate();
					repaint();
				}
			});
		}
	}

	public Point getZoneLocation (int zoneID) {
		JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();
		for (int i = 0, n = zoneModel.getSize(); i < n; i++) {
			ZoneEntry zoneEntry = (ZoneEntry)zoneModel.get(i);
			if (zoneEntry.id == zoneID) {
				Rectangle b = zoneList.getCellBounds(i, i);
				return SwingUtilities.convertPoint(zoneList, b.x + Math.round(b.width / 2.0f), b.y + Math.round(b.height / 2.0f),
					layeredPane);
			}
		}
		throw new ArcaneException("Unable to find zone in list: " + zoneID);
	}

	private void initializeComponents () {
		zoneScroll = new JScrollPane();
		zoneScroll.setBorder(BorderFactory.createEmptyBorder());
		add(zoneScroll);

		zoneModel = new DefaultListModel();
		zoneList = new JList(zoneModel);
		zoneList.setBackground(null);
		zoneList.setCellRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
				return super.getListCellRendererComponent(list, value, index, false, false);
			}
		});
		zoneScroll.setViewportView(zoneList);

		nameLabel = new JLabel("Name");
		add(nameLabel);

		lifeLabel = new JLabel("20");
		lifeLabel.setFont(Font.decode("Tahoma-bold-13"));
		add(lifeLabel);

		final Image manaCountsImage = UI.getImageIcon("/manaCounts.png").getImage();
		final FontMetrics metrics = getFontMetrics(Font.decode("Tahoma-bold-12"));
		final int y = Math.max(0, metrics.getHeight() - 9);
		manaPanel = new JPanel() {
			protected void paintComponent (Graphics g) {
				super.paintComponent(g);
				g.drawImage(manaCountsImage, GUTTER, y, null);
				drawCentered(g, manaPool.getWhite(), 11 + GUTTER, 2 + y);
				drawCentered(g, manaPool.getBlue(), 31 + GUTTER, 2 + y);
				drawCentered(g, manaPool.getBlack(), 51 + GUTTER, 2 + y);
				drawCentered(g, manaPool.getRed(), 71 + GUTTER, 2 + y);
				drawCentered(g, manaPool.getGreen(), 91 + GUTTER, 2 + y);
				drawCentered(g, manaPool.getColorless(), 111 + GUTTER, 2 + y);
			}

			private void drawCentered (Graphics g, int value, int x, int y) {
				if (value == 0) return;
				drawCentered(g, String.valueOf(value), x, y);
			}

			private void drawCentered (Graphics g, String text, int x, int y) {
				g.drawString(text, x - metrics.stringWidth(text) / 2, y);
			}
		};
		manaPanel.setPreferredSize(new Dimension(119, 21 + y));
		manaPanel.setMinimumSize(new Dimension(119, 21 + y));
		manaPanel.setMaximumSize(new Dimension(119, 21 + y));
		manaPanel.setSize(new Dimension(119, 21 + y));
		manaPanel.addMouseListener(new MouseAdapter() {
			public void mouseActuallyClicked (MouseEvent evt) {
				switch ((evt.getX() - GUTTER) / 20) {
				case 0:
					manaClicked("W");
					break;
				case 1:
					manaClicked("U");
					break;
				case 2:
					manaClicked("B");
					break;
				case 3:
					manaClicked("R");
					break;
				case 4:
					manaClicked("G");
					break;
				default:
					manaClicked("C");
					break;
				}
			}
		});
		add(manaPanel);
	}

	private void manaClicked (String color) {
		manaPool.add(color, 1);
	}

	public void layout () {
		int width = getWidth() - 1;
		int height = getPreferredSize().height;
		int zoneListHeight = Math.min(height, zoneList.getPreferredScrollableViewportSize().height);

		Dimension lifeSize = lifeLabel.getPreferredSize();
		lifeLabel.setLocation(width - lifeSize.width - GUTTER, 0);
		lifeLabel.setSize(lifeSize);

		nameLabel.setLocation(GUTTER, 2);
		nameLabel.setSize(width - lifeSize.width - 3 - GUTTER, nameLabel.getPreferredSize().height);

		manaPanel.setLocation(1, height - zoneListHeight - manaPanel.getHeight());
		manaPanel.setSize(width - 1, manaPanel.getHeight());

		zoneScroll.setLocation(GUTTER - 1, height - zoneListHeight);
		zoneScroll.setSize(width - GUTTER + 1, zoneListHeight);
	}

	static private class ZoneEntry {
		public int id;
		public String text;

		public String toString () {
			return text;
		}
	}
}
