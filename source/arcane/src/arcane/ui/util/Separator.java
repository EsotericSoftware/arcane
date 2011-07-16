
package arcane.ui.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

public class Separator extends JComponent {
	private Dimension size = new Dimension(6, 20);

	protected void paintComponent (Graphics g) {
		int x = 2;
		g.setColor(Color.gray);
		g.drawLine(x, 2, x, getHeight() - 4);
		x++;
		g.setColor(Color.white);
		g.drawLine(x, 2, x, getHeight() - 4);
		super.paintComponent(g);
	}

	public int getWidth () {
		return (int)size.getWidth();
	}

	public int getHeight () {
		return (int)size.getHeight();
	}

	public Dimension getSize () {
		return size;
	}

	public Dimension getPreferredSize () {
		return size;
	}

	public Dimension getMinimumSize () {
		return size;
	}

	public Dimension getMaximumSize () {
		return size;
	}
}
