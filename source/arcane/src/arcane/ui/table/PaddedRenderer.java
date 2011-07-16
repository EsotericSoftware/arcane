
package arcane.ui.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import arcane.Card;

/**
 * Disables focus border and shifts cell contents down and/or right.
 */
public class PaddedRenderer extends DefaultTableCellRenderer {
	private final int down;
	private final int right;
	private Pattern replaceDashEntityPattern = Pattern.compile("&#821.;");
	private Color illegalColor = new Color(255, 208, 208);
	private Color illegalSelectedColor = new Color(238, 0, 0);

	public PaddedRenderer (int down, int right) {
		super();
		this.down = down;
		this.right = right;
	}

	protected void paintComponent (Graphics g) {
		if (ui != null) {
			Graphics scratchGraphics = g.create();
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			scratchGraphics.translate(right, down);
			try {
				ui.update(scratchGraphics, this);
			} finally {
				scratchGraphics.dispose();
			}
		}
	}

	public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
		int column) {

		if (value != null) value = replaceDashEntityPattern.matcher(value.toString()).replaceAll("-");

		super.getTableCellRendererComponent(table, value, isSelected, false, row, column);

		CardTable cardTable = (CardTable)table;
		Card card = cardTable.model.viewCards.get(row);
		if (cardTable.illegalCards.contains(card)) {
		    	setForeground(Color.BLACK);
			if (isSelected)
				setBackground(illegalSelectedColor);
			else
				setBackground(illegalColor);
		} else if (isSelected){
			super.setBackground(table.getSelectionBackground());
			super.setForeground(table.getSelectionForeground());
		}
		else {
			setBackground(table.getBackground());
			setForeground(table.getForeground());
		}

		return this;
	}
}
