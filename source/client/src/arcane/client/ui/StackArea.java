
package arcane.client.ui;

import javax.swing.JScrollPane;

import arcane.client.ui.util.CardArea;

public class StackArea extends CardArea {
	public StackArea (JScrollPane scrollPane) {
		super(scrollPane);
		setMaxRows(1);
		setMaxCoverage(0.95f);
		setVertical(true);
	}
}
