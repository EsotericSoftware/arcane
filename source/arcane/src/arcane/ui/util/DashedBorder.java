package arcane.ui.util;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.border.LineBorder;

public class DashedBorder extends LineBorder {
	private BasicStroke stroke;

	public DashedBorder (Color color, int length) {
		super(color);
		stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1, new float[] {length, length}, 0);
	}

	public void paintBorder (Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2d = (Graphics2D)g.create();
		g2d.setStroke(stroke);
		super.paintBorder(c, g2d, x, y, width, height);
		g2d.dispose();
	}
}
