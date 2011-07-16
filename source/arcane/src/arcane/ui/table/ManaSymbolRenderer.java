
package arcane.ui.table;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JTable;

import arcane.ui.ManaSymbols;

public class ManaSymbolRenderer extends PaddedRenderer {
	private String manaCost;

	public ManaSymbolRenderer () {
		super(0, 0);
	}

	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		if (manaCost == null || manaCost.length() == 0) return;
		ManaSymbols.draw(g, manaCost, 3, 1);
	};

	public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
		int column) {

		manaCost = (String)value;

		String text = "";
		if (manaCost.equals("{C}")) {
			text = "Colorless";
			manaCost = null;
		}

		return super.getTableCellRendererComponent(table, text, isSelected, false, row, column);
	}
}
