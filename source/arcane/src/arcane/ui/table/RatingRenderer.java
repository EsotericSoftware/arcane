
package arcane.ui.table;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import arcane.Card;
import arcane.ui.util.UI;

public class RatingRenderer extends PaddedRenderer {
	static public Image starImage;

	static private Image greyStarImage;
	static private Image overStarImage;

	private int rating;
	private int overRating;
	private int overRow;
	private int row;

	public RatingRenderer (final CardTable table, final int modelRatingColumn) {
		super(0, 0);

		final MouseMotionAdapter mouseMotionListener = new MouseMotionAdapter() {
			public void mouseMoved (MouseEvent evt) {
				Point mousePosition = evt.getPoint();
				int row = table.rowAtPoint(mousePosition);
				int column = table.columnAtPoint(mousePosition);
				if (row == -1 || column == -1) return;
				int oldOverRow = overRow;
				int oldOverRating = overRating;
				if (column == getViewRatingColumn(table, modelRatingColumn)) {
					Rectangle cellRect = table.getCellRect(row, column, true);
					overRating = (mousePosition.x - cellRect.x - 2) / 15 + 1;
					if (overRating > 5) {
						overRating = 0;
						overRow = -1;
					} else
						overRow = row;
				} else {
					overRating = 0;
					overRow = -1;
				}
				if (overRow != oldOverRow || overRating != oldOverRating) {
					updateCell(table, overRow, modelRatingColumn);
					updateCell(table, oldOverRow, modelRatingColumn);
				}
			}
		};
		table.addMouseMotionListener(mouseMotionListener);

		final MouseAdapter mouseClickListener = new MouseAdapter() {
			public void mouseExited (MouseEvent evt) {
				overRating = 0;
				updateCell(table, overRow, modelRatingColumn);
				overRow = -1;
			}

			public void mouseClicked (MouseEvent evt) {
				if (evt.getButton() != 1) return;
				Point mousePosition = evt.getPoint();
				int row = table.rowAtPoint(mousePosition);
				int column = table.columnAtPoint(mousePosition);
				if (row == -1 || column != getViewRatingColumn(table, modelRatingColumn)) return;
				Rectangle cellRect = table.getCellRect(row, column, true);
				int clickedRating = (mousePosition.x - cellRect.x - 2) / 15 + 1;
				if (clickedRating > 5) return;
				Card card = table.model.viewCards.get(row);
				if (card.rating == clickedRating) clickedRating = 0;
				card.rating = clickedRating;
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

	private int getViewRatingColumn (CardTable table, int modelRatingColumn) {
		for (int i = 0, n = table.getColumnCount(); i < n; i++)
			if (table.getColumnModel().getColumn(i).getModelIndex() == modelRatingColumn) return i;
		throw new RuntimeException("Rating column not found.");
	}

	private void updateCell (CardTable table, int row, int column) {
		if (row == -1) return;
		for (int rowIndex : table.model.getRowIndices(table.model.viewCards.get(row).name))
			table.model.fireTableCellUpdated(rowIndex, column);
	}

	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		for (int i = 0; i < 5; i++) {
			Image image;
			if (row == overRow && i < overRating)
				image = overStarImage;
			else if (i < rating && (row != overRow || overRating == 0))
				image = starImage;
			else
				image = greyStarImage;
			g.drawImage(image, 2 + i * 15, 1, null);
		}
	};

	public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
		int column) {
		rating = (Integer)value;
		this.row = row;
		return super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
	}

	static public void loadImages () {
		starImage = UI.getImageIcon("images/star.png").getImage();
		greyStarImage = UI.getImageIcon("images/star_grey.png").getImage();
		overStarImage = UI.getImageIcon("images/star_over.png").getImage();
	}
}
