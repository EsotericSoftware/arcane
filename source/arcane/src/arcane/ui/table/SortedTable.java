
package arcane.ui.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import arcane.Arcane;
import arcane.Card;
import arcane.CardProperty;

public abstract class SortedTable extends JTable {
	static protected enum Direction {
		descending, ascending, unsorted
	}

	static public StringComparator stringComparator = new StringComparator();
	static public NumberComparator numberComparator = new NumberComparator();
	static public CastingCostComparator castingCostComparator = new CastingCostComparator();
	static public PowerComparator powerComparator = new PowerComparator();
	static public ToughnessComparator toughnessComparator = new ToughnessComparator();
	static public ManaProducedComparator manaProducedComparator = new ManaProducedComparator();
	static public FlagComparator flagComparator = new FlagComparator();
	static public RarityComparator rarityComparator = new RarityComparator();
	static public CollectorNumberComparator collectorNumberComparator = new CollectorNumberComparator();
	static public OwnedQtyComparator ownedQtyComparator = new OwnedQtyComparator();

	static private final SortedColumn unsortedColumn = new SortedColumn(-1, Direction.unsorted);

	public boolean allowUnsorted = true;
	public List<SortedColumn> sortedColumns = new ArrayList<SortedColumn>();
	public CardTableModel model;

	protected Comparator<Card> comparator = new RowComparator();

	private boolean sortingDisabled;
	private List<Card> selectedCards;

	public void setSelectedCard (Card card) {
		int rowIndex = model.getRowIndex(card);
		if (rowIndex == -1)
			selectionModel.clearSelection();
		else {
			selectionModel.setSelectionInterval(rowIndex, rowIndex);
			scrollRectToVisible(getCellRect(rowIndex, 0, true));
		}
	}

	public boolean setSelectedCards (String cardName) {
		return setSelectedCards(model.getCards(cardName));
	}

	public boolean setSelectedCards (Collection<Card> cards) {
		if (cards.size() > 100) return false; // Don't hang just to select cards.
		int lowestIndex = Integer.MAX_VALUE;
		int highestIndex = -1;
		List<Integer> indices = new ArrayList<Integer>();
		for (Card card : cards) {
			int rowIndex = model.getRowIndex(card);
			if (rowIndex != -1) indices.add(rowIndex);
			lowestIndex = Math.min(lowestIndex, rowIndex);
			highestIndex = Math.max(highestIndex, rowIndex);
		}
		if (indices.size() == 0) return false;
		selectionModel.clearSelection();
		for (int rowIndex : indices)
			selectionModel.addSelectionInterval(rowIndex, rowIndex);
		Rectangle lowRect = getCellRect(lowestIndex, 0, true);
		Rectangle highRect = getCellRect(highestIndex, 0, true);
		scrollRectToVisible(lowRect);
		scrollRectToVisible(new Rectangle(0, lowRect.y, 10, highRect.y + highRect.height - lowRect.y));
		return true;
	}

	public Card getSelectedCard () {
		int rowIndex = getSelectedRow();
		if (rowIndex == -1) return null;
		return model.viewCards.get(rowIndex);
	}

	public List<Card> getSelectedCards () {
		List<Card> cards = new ArrayList<Card>();
		for (int rowIndex : getSelectedRows()) {
			if (rowIndex >= model.viewCards.size()) continue;
			cards.add(model.viewCards.get(rowIndex));
		}
		return cards;
	}

	public void setModel (TableModel model) {
		super.setModel(model);
		model.addTableModelListener(new TableModelListener() {
			public void tableChanged (TableModelEvent evt) {
				if (sortingDisabled) return;
				if (evt.getFirstRow() == 0 && evt.getLastRow() == Integer.MAX_VALUE) sort();
			}
		});
	}

	public void setTableHeader (final JTableHeader header) {
		super.setTableHeader(header);
		((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

		// Don't wrap table header renderer if using XP theme (Sun bug 6429812).
		if (!header.getDefaultRenderer().getClass().getName().contains("XPDefaultRenderer"))
			header.setDefaultRenderer(new SortableHeaderRenderer(header.getDefaultRenderer()));

		header.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent evt) {
				if (evt.getButton() != 1) return;
				TableColumnModel columnModel = header.getColumnModel();
				int viewColumn = columnModel.getColumnIndexAtX(evt.getX());
				if (viewColumn == -1) return;
				int columnIndex = columnModel.getColumn(viewColumn).getModelIndex();
				if (columnIndex == -1) return;
				Direction direction = getSortDirection(columnIndex);
				if (!evt.isControlDown() && !evt.isAltDown()) sortedColumns.clear();
				switch (direction) {
				case unsorted:
					direction = Direction.ascending;
					break;
				case ascending:
					direction = Direction.descending;
					break;
				case descending:
					if (allowUnsorted)
						direction = Direction.unsorted;
					else
						direction = Direction.ascending;
					break;
				}
				setSortDirection(columnIndex, direction);
			}
		});

		selectionModel.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent evt) {
				if (evt.getValueIsAdjusting()) return;
				selectedCards = getSelectedCards();
			}
		});
	}

	public void setSortDirection (int columnIndex, Direction direction) {
		SortedColumn sortedColumn = getSortedColumn(columnIndex);
		if (sortedColumn != unsortedColumn) sortedColumns.remove(sortedColumn);
		if (direction != Direction.unsorted) sortedColumns.add(new SortedColumn(columnIndex, direction));
		sort();
	}

	public void sort () {
		// A sorted column could have been removed.
		for (Iterator<SortedColumn> iter = sortedColumns.iterator(); iter.hasNext();)
			if (iter.next().index >= model.properties.length) iter.remove();

		// Collapse same cards into single rows and collect quantities.
		List<Card> viewCards = new ArrayList<Card>();
		model.cardToQty = new HashMap<Card, Integer>();
		for (Card card : model.unsortedCards) {
			Integer qty = model.cardToQty.get(card);
			if (qty == null) {
				viewCards.add(card);
				qty = 0;
			}
			qty++;
			model.cardToQty.put(card, qty);
		}

		model.viewCards = viewCards;
		Collections.sort(model.viewCards, comparator);
		sorted();
	}

	public void sorted () {
		List<Card> oldSelectedCards = selectedCards;
		sortingDisabled = true;
		((AbstractTableModel)getModel()).fireTableDataChanged();
		sortingDisabled = false;
		if (tableHeader != null) {
			tableHeader.repaint();
			if (oldSelectedCards != null) setSelectedCards(oldSelectedCards);
		}
	}

	public Comparator<Card> getComparator (int columnIndex) {
		final CardProperty property = model.properties[columnIndex];
		switch (property) {
		case castingCost:
			return castingCostComparator;
		case price:
		case qty:
		case convertedCost:
		case rating:
			numberComparator.property = property;
			return numberComparator;
		case ownedQty:
		    	ownedQtyComparator.uniqueOnly = model.isUniqueOnly;
			return ownedQtyComparator;
		case power:
		case pt:
			return powerComparator;
		case touhgness:
			return toughnessComparator;
		case color:
		case creatureType:
		case set:
		case setName:
		case legal:
		case name:
		case type:
		case version:
			stringComparator.property = property;
			return stringComparator;
		case collectorNumber:
			return collectorNumberComparator;
		case flags:
			return flagComparator;
		case manaProduced:
			return manaProducedComparator;
		case rarity:
			return rarityComparator;
		}
		throw new RuntimeException("Invalid property: " + property);
	}

	private SortedColumn getSortedColumn (int columnIndex) {
		for (SortedColumn sortedColumn : sortedColumns)
			if (sortedColumn.index == columnIndex) return sortedColumn;
		return unsortedColumn;
	}

	private Direction getSortDirection (int columnIndex) {
		return getSortedColumn(columnIndex).direction;
	}

	protected static class SortedColumn {
		public int index;
		public Direction direction;

		public SortedColumn (int columnIndex, Direction direction) {
			this.index = columnIndex;
			this.direction = direction;
		}
	}

	private class SortableHeaderRenderer implements TableCellRenderer {
		private TableCellRenderer renderer;

		public SortableHeaderRenderer (TableCellRenderer renderer) {
			this.renderer = renderer;
		}

		public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
			int column) {
			Component component = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (component instanceof JLabel) {
				JLabel label = (JLabel)component;
				label.setHorizontalTextPosition(JLabel.LEFT);
				int modelColumn = table.convertColumnIndexToModel(column);
				SortedColumn sortedColumn = getSortedColumn(modelColumn);
				if (sortedColumn == unsortedColumn)
					label.setIcon(null);
				else
					label.setIcon(new Arrow(sortedColumn.direction == Direction.descending, label.getFont().getSize(), sortedColumns
						.indexOf(sortedColumn)));
			}
			return component;
		}
	}

	static private class Arrow implements Icon {
		private boolean descending;
		private int size;
		private int priority;

		public Arrow (boolean descending, int size, int priority) {
			this.descending = descending;
			this.size = size;
			this.priority = priority;
		}

		public void paintIcon (Component c, Graphics g, int x, int y) {
			Color color = c == null ? Color.GRAY : c.getBackground();
			// In a compound sort, make each succesive triangle 20% smaller than the previous one.
			int dx = (int)(size / 2.0 * Math.pow(0.8, priority));
			int dy = descending ? dx : -dx;
			// Align icon (roughly) with font baseline.
			y = y + 4 * size / 6 + (descending ? -dy : 1);
			int shift = descending ? 1 : -1;
			g.translate(x, y);
			// Right diagonal.
			g.setColor(color.darker());
			g.drawLine(dx / 2, dy, 0, 0);
			g.drawLine(dx / 2, dy + shift, 0, shift);
			// Left diagonal.
			g.setColor(color.brighter());
			g.drawLine(dx / 2, dy, dx, 0);
			g.drawLine(dx / 2, dy + shift, dx, shift);
			// Horizontal line.
			if (descending)
				g.setColor(color.darker().darker());
			else
				g.setColor(color.brighter().brighter());
			g.drawLine(dx, 0, 0, 0);
			g.setColor(color);
			g.translate(-x, -y);
		}

		public int getIconWidth () {
			return size;
		}

		public int getIconHeight () {
			return size;
		}
	}

	private class RowComparator implements Comparator<Card> {
		public int compare (Card o1, Card o2) {
			for (SortedColumn sortedColumn : sortedColumns) {
				// Define null is less than everything, except null.
				int comparison;
				if (o1 == null && o2 == null)
					comparison = 0;
				else if (o1 == null)
					comparison = -1;
				else if (o2 == null)
					comparison = 1;
				else
					comparison = getComparator(sortedColumn.index).compare(o1, o2);
				if (comparison != 0) return sortedColumn.direction == Direction.descending ? -comparison : comparison;
			}
			return 0;
		}
	}

	static public class StringComparator implements Comparator<Card> {
		public CardProperty property;

		public int compare (Card card1, Card card2) {
			String value1 = (String)card1.getValue(property);
			if (value1 == null) value1 = "";
			String value2 = (String)card2.getValue(property);
			if (value2 == null) value2 = "";
			return String.CASE_INSENSITIVE_ORDER.compare(value1, value2);
		}
	};

	static public class FlagComparator implements Comparator<Card> {
		public int compare (Card card1, Card card2) {
			int value1 = 0;
			String flags1 = card1.flags;
			for (int i = 0, n = flags1.length(); i < n; i++) {
				switch (flags1.charAt(i)) {
				case 'b':
					value1 += 10000;
					break;
				case 'g':
					value1 += 1000;
					break;
				case 'y':
					value1 += 100;
					break;
				case 'o':
					value1 += 10;
					break;
				case 'r':
					value1 += 1;
					break;
				}
			}

			int value2 = 0;
			String flags2 = card2.flags;
			for (int i = 0, n = flags2.length(); i < n; i++) {
				switch (flags2.charAt(i)) {
				case 'b':
					value2 += 10000;
					break;
				case 'g':
					value2 += 1000;
					break;
				case 'y':
					value2 += 100;
					break;
				case 'o':
					value2 += 10;
					break;
				case 'r':
					value2 += 1;
					break;
				}
			}

			return value1 - value2;
		}
	};

	static public class RarityComparator implements Comparator<Card> {
		public int compare (Card card1, Card card2) {
			int value1 = 0;
			switch (card1.rarity.charAt(0)) {
			case 'M':
				value1 = 4;
				break;
			case 'R':
				value1 = 3;
				break;
			case 'U':
				value1 = 2;
				break;
			case 'C':
				value1 = 1;
				break;
			}

			int value2 = 0;
			switch (card2.rarity.charAt(0)) {
			case 'M':
				value2 = 4;
				break;
			case 'R':
				value2 = 3;
				break;
			case 'U':
				value2 = 2;
				break;
			case 'C':
				value2 = 1;
				break;
			}

			return value1 - value2;
		}
	};

	static public class NumberComparator implements Comparator<Card> {
		public CardProperty property;

		public int compare (Card card1, Card card2) {
			float value1 = ((Number)card1.getValue(property)).floatValue();
			float value2 = ((Number)card2.getValue(property)).floatValue();
			float difference = value1 - value2;
			if (difference > 0) return 1;
			if (difference < 0) return -1;
			return 0;
		}
	};

	static public class PowerComparator implements Comparator<Card> {
		public int compare (Card card1, Card card2) {
			int value1 = card1.power;
			if (value1 == 0) {
				if (card1.pt.length() == 0)
					value1 = -2;
				else if (card1.pt.charAt(0) != '0') {
					value1 = -1;
				}
			}
			int value2 = card2.power;
			if (value2 == 0) {
				if (card2.pt.length() == 0)
					value2 = -2;
				else if (card2.pt.charAt(0) != '0') {
					value2 = -1;
				}
			}
			int comparison = value1 - value2;
			if (comparison != 0) return comparison;
			return toughnessComparator.compare(card1, card2);
		}
	};

	static public class ToughnessComparator implements Comparator<Card> {
		public int compare (Card card1, Card card2) {
			int value1 = card1.toughness;
			if (value1 == 0) {
				if (card1.pt.length() == 0)
					value1 = -2;
				else if (card1.pt.charAt(card1.pt.length() - 1) != '0') {
					value1 = -1;
				}
			}
			int value2 = card2.toughness;
			if (value2 == 0) {
				if (card2.pt.length() == 0)
					value2 = -2;
				else if (card2.pt.charAt(card2.pt.length() - 1) != '0') {
					value2 = -1;
				}
			}
			return value1 - value2;
		}
	};

	static public class CollectorNumberComparator implements Comparator<Card> {
		public int compare (Card card1, Card card2) {
			stringComparator.property = CardProperty.set;
			int difference = stringComparator.compare(card1, card2);
			if (difference != 0) return difference;
			if ((card1.collectorNumber == null || card1.collectorNumber.length() == 0)
				&& (card2.collectorNumber == null || card2.collectorNumber.length() == 0)) {
				stringComparator.property = CardProperty.name;
				return stringComparator.compare(card1, card2);
			}
			if (card1.collectorNumber == null || card1.collectorNumber.length() == 0) return 1;
			if (card2.collectorNumber == null || card2.collectorNumber.length() == 0) return -1;
			int value1 = 0;
			try {
				if (card1.collectorNumber.contains("/")) value1 = Integer.parseInt(card1.collectorNumber.split("/")[0]);
			} catch (NumberFormatException ignored) {
			}
			int value2 = 0;
			try {
				if (card2.collectorNumber.contains("/")) value2 = Integer.parseInt(card2.collectorNumber.split("/")[0]);
			} catch (NumberFormatException ignored) {
			}
			return value1 - value2;
		}
	};

	static public class OwnedQtyComparator implements Comparator<Card> {
		public boolean uniqueOnly;

		public int compare (Card card1, Card card2) {
		    	float value1 = 0, value2 = 0;
		    	if(uniqueOnly){
		    	    Arcane arcane = Arcane.getInstance();
		    	    value1 = (arcane.getTotalOwnedQty(card1.name));
		    	    value2 = (arcane.getTotalOwnedQty(card2.name));
		    	} else {
		    	    value1 = ((Number)card1.getValue(CardProperty.ownedQty)).floatValue();
		    	    value2 = ((Number)card2.getValue(CardProperty.ownedQty)).floatValue();
		    	}
			float difference = value1 - value2;
			if (difference > 0) return 1;
			if (difference < 0) return -1;
			return 0;
		}
	};

	static public class CastingCostComparator implements Comparator<Card> {
		public int compare (Card card1, Card card2) {
			int comparison = card1.convertedCastingCost - card2.convertedCastingCost;
			if (comparison != 0) return comparison;

			// Sort no casting cost cards (eg, lands) below zero casting cost cards.
			if (card1.convertedCastingCost == 0) {
				if (card1.castingCost.length() > 0) {
					if (card2.castingCost.length() == 0) return 1;
				} else if (card2.castingCost.length() > 0) return -1;
			}

			int value1 = 0;
			String cost1 = card1.castingCost;
			outerLoop: for (int i = 0, n = cost1.length() - 1; i < n; i++) {
				switch (cost1.charAt(i)) {
				case '/':
					break outerLoop;
				case 'W':
					value1 += 1;
					break;
				case 'U':
					value1 += 10;
					break;
				case 'B':
					value1 += 100;
					break;
				case 'R':
					value1 += 1000;
					break;
				case 'G':
					value1 += 10000;
					break;
				case 'X':
					value1 += 100000;
					break;
				}
			}

			int value2 = 0;
			String cost2 = card2.castingCost;
			outerLoop: for (int i = 0, n = cost2.length() - 1; i < n; i++) {
				switch (cost2.charAt(i)) {
				case '/':
					break outerLoop;
				case 'W':
					value2 += 1;
					break;
				case 'U':
					value2 += 10;
					break;
				case 'B':
					value2 += 100;
					break;
				case 'R':
					value2 += 1000;
					break;
				case 'G':
					value2 += 10000;
					break;
				case 'X':
					value2 += 100000;
					break;
				}
			}

			return value1 - value2;
		}
	};

	static public class ManaProducedComparator implements Comparator<Card> {
		public int compare (Card card1, Card card2) {
			int comparison = card1.manaProduced.length() - card2.manaProduced.length();
			if (comparison != 0) return comparison;

			int value1 = 0;
			String cost1 = card1.manaProduced;
			for (int i = 0, n = cost1.length() - 1; i < n; i++) {
				switch (cost1.charAt(i)) {
				case 'A':
					value1 += 1;
					break;
				case 'C':
					value1 += 10;
					break;
				case 'W':
					value1 += 100;
					break;
				case 'U':
					value1 += 1000;
					break;
				case 'B':
					value1 += 10000;
					break;
				case 'R':
					value1 += 100000;
					break;
				case 'G':
					value1 += 1000000;
					break;
				}
			}

			int value2 = 0;
			String cost2 = card2.manaProduced;
			for (int i = 0, n = cost2.length() - 1; i < n; i++) {
				switch (cost2.charAt(i)) {
				case 'A':
					value2 += 1;
					break;
				case 'C':
					value2 += 10;
					break;
				case 'W':
					value2 += 100;
					break;
				case 'U':
					value2 += 1000;
					break;
				case 'B':
					value2 += 10000;
					break;
				case 'R':
					value2 += 100000;
					break;
				case 'G':
					value2 += 1000000;
					break;
				}
			}

			return value1 - value2;
		}
	};
}
