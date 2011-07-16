
package arcane.ui.util;

import java.awt.event.MouseEvent;

public class MouseAdapter extends java.awt.event.MouseAdapter {
	private boolean mouseDown;

	public final void mouseClicked (MouseEvent evt) {
	}

	public void mouseActuallyClicked (MouseEvent evt) {
	}

	public void mouseExited (MouseEvent evt) {
		mouseDown = false;
	}

	public void mousePressed (MouseEvent evt) {
		mouseDown = true;
	}

	public void mouseReleased (MouseEvent evt) {
		if (mouseDown) {
			mouseActuallyClicked(evt);
			mouseDown = false;
		}
	}
}
