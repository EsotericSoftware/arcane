
package arcane.ui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.sun.java.swing.plaf.windows.WindowsMenuUI;

public class Menu extends JMenu {
	public Menu (String text, Image image) {
		this(text);
		setIcon(new ImageIcon(image));
	}

	public Menu (String text) {
		super(text);

		// BOZO! - Fix.
//		if (!(getUI() instanceof WindowsMenuUI)) return;
//		setUI(new WindowsMenuUI() {
//			protected void paintMenuItem (Graphics g, JComponent c, Icon checkIcon, Icon arrowIcon, Color background,
//				Color foreground, int defaultTextIconGap) {
//				super.paintMenuItem(g, c, null, arrowIcon, background, foreground, 0);
//			}
//
//			protected void paintText (Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
//				textRect.x += 6;
//				super.paintText(g, menuItem, textRect, text);
//			}
//		});
	}
}
