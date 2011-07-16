
package arcane.ui.table;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import arcane.Card;
import arcane.ui.util.UI;

public class FlagRenderer extends PaddedRenderer {
	static public Map<Integer, Flag> indexToFlag = new LinkedHashMap<Integer, Flag>();
	static public Image greyFlagImage;

	private String flags;
	private int overFlag;
	private int overRow;
	private int row;

	public FlagRenderer (final CardTable table, final int modelFlagColumn) {
		super(0, 0);

		final MouseMotionAdapter mouseMotionListener = new MouseMotionAdapter() {
			public void mouseMoved (MouseEvent evt) {
				Point mousePosition = evt.getPoint();
				int row = table.rowAtPoint(mousePosition);
				int column = table.columnAtPoint(mousePosition);
				if (row == -1 || column == -1) return;
				int oldOverRow = overRow;
				int oldOverFlag = overFlag;
				if (column == getViewFlagColumn(table, modelFlagColumn)) {
					Rectangle cellRect = table.getCellRect(row, column, true);
					overFlag = (mousePosition.x - cellRect.x - 2) / 15;
					if (overFlag > 4) {
						overFlag = 0;
						overRow = -1;
					} else
						overRow = row;
				} else {
					overFlag = 0;
					overRow = -1;
				}
				if (overRow != oldOverRow || overFlag != oldOverFlag) {
					updateCell(table, overRow, modelFlagColumn);
					updateCell(table, oldOverRow, modelFlagColumn);
				}
			}
		};
		table.addMouseMotionListener(mouseMotionListener);

		final MouseAdapter mouseClickListener = new MouseAdapter() {
			public void mouseExited (MouseEvent evt) {
				overFlag = 0;
				updateCell(table, overRow, modelFlagColumn);
				overRow = -1;
			}

			public void mouseClicked (MouseEvent evt) {
				if (evt.getButton() != 1) return;
				Point mousePosition = evt.getPoint();
				int row = table.rowAtPoint(mousePosition);
				int column = table.columnAtPoint(mousePosition);
				if (row == -1 || column != getViewFlagColumn(table, modelFlagColumn)) return;
				Rectangle cellRect = table.getCellRect(row, column, true);
				int clickedFlagIndex = (mousePosition.x - cellRect.x - 2) / 15;
				if (clickedFlagIndex > 4) return;
				Flag clickedFlag = indexToFlag.get(clickedFlagIndex);
				Card card = table.model.viewCards.get(row);
				card.setFlag(clickedFlag.letter, !card.flags.contains(clickedFlag.letter));
				// BOZO
				// DeckBuilder.updateRows(card.name);
				table.getSelectionModel().clearSelection();
				table.getSelectionModel().setSelectionInterval(row, row);
			}
		};
		table.addMouseListener(mouseClickListener);

		table.model.addTableModelListener(new TableModelListener() {
			public void tableChanged (TableModelEvent evt) {
				if (evt.getFirstRow() != TableModelEvent.HEADER_ROW) return;
				// When the table structure changes, release resources held for the column.
				table.removeMouseMotionListener(mouseMotionListener);
				table.removeMouseListener(mouseClickListener);
			}
		});
	}

	private int getViewFlagColumn (CardTable table, int modelFlagColumn) {
		for (int i = 0, n = table.getColumnCount(); i < n; i++)
			if (table.getColumnModel().getColumn(i).getModelIndex() == modelFlagColumn) return i;
		throw new RuntimeException("Flag column not found.");
	}

	private void updateCell (CardTable table, int row, int column) {
		if (row == -1) return;
		for (int rowIndex : table.model.getRowIndices(table.model.viewCards.get(row).name))
			table.model.fireTableCellUpdated(rowIndex, column);
	}

	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		for (int i = 0; i < 5; i++) {
			Flag flag = indexToFlag.get(i);
			Image image;
			if ((row == overRow && i == overFlag) || (flags != null && flags.contains(flag.letter)))
				image = flag.image;
			else
				image = greyFlagImage;
			g.drawImage(image, 2 + i * 15, 1, null);
		}
	};

	public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
		int column) {
		flags = (String)value;
		this.row = row;
		return super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
	}

	static public void loadImages () {
		greyFlagImage = UI.getImageIcon("images/flag_grey.png").getImage();
		indexToFlag.put(0, new Flag("Blue", UI.getImageIcon("images/flag_b.png").getImage()));
		indexToFlag.put(1, new Flag("Green", UI.getImageIcon("images/flag_g.png").getImage()));
		indexToFlag.put(2, new Flag("Yellow", UI.getImageIcon("images/flag_y.png").getImage()));
		indexToFlag.put(3, new Flag("Orange", UI.getImageIcon("images/flag_o.png").getImage()));
		indexToFlag.put(4, new Flag("Red", UI.getImageIcon("images/flag_r.png").getImage()));
	}

	static public class Flag {
		public String name;
		public String letter;
		public Image image;

		public Flag (String name, Image image) {
			this.name = name;
			this.letter = name.substring(0, 1).toLowerCase();
			this.image = image;
		}
	}
}
