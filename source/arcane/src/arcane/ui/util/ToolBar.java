
package arcane.ui.util;

import java.awt.Graphics;

import javax.swing.JToolBar;

public class ToolBar extends JToolBar {
	public void paint (Graphics g) {
		if ((getWidth() <= 0) || (getHeight() <= 0)) return;
		Graphics componentGraphics = getComponentGraphics(g);
		Graphics co = componentGraphics.create();
		try {
			paintComponent(co);

			// Smoke any borders the toolbar UI has drawn!
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());

			paintChildren(co);
		} finally {
			co.dispose();
		}
	}
}
