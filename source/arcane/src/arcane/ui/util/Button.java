
package arcane.ui.util;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

public class Button extends JButton {
	private Boolean nextAlternate;

	public Button () {
		addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent evt) {
				if (evt.getButton() != 3) return;
				setNextAlternate(true);
				doClick();
			}
		});
	}

	public void setNextAlternate (Boolean nextAlternate) {
		this.nextAlternate = nextAlternate;
	}

	public boolean isAlternate (ActionEvent evt) {
		if (nextAlternate != null) {
			try {
				return nextAlternate;
			} finally {
				nextAlternate = null;
			}
		}
		return (evt.getModifiers() & InputEvent.ALT_MASK) != 0;
	}
}
