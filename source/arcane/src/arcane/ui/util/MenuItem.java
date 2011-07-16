
package arcane.ui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

import com.sun.java.swing.plaf.windows.WindowsMenuItemUI;

public class MenuItem extends JMenuItem {
	public MenuItem (String text, Image image) {
		this(text);
		setIcon(new ImageIcon(image));
	}

	public MenuItem (String text, int nm) {
		super(text, nm);
		fixUI();
	}

	public MenuItem (String text) {
		super(text);
		fixUI();
	}

	private void fixUI () {
		if (!(getUI() instanceof WindowsMenuItemUI)) return;
		setUI(new WindowsMenuItemUI() {
			protected void paintMenuItem (Graphics g, JComponent c, Icon checkIcon, Icon arrowIcon, Color background,
				Color foreground, int defaultTextIconGap) {
				super.paintMenuItem(g, c, null, arrowIcon, background, foreground, 0);
			}

			protected void paintText (Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
				textRect.x += 6;
				super.paintText(g, menuItem, textRect, text);
			}
		});
	}
}
