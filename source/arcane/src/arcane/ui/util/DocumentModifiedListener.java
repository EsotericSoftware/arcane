
package arcane.ui.util;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class DocumentModifiedListener implements DocumentListener {
	public void changedUpdate (DocumentEvent e) {
	}

	public void insertUpdate (DocumentEvent e) {
		changed();
	}

	public void removeUpdate (DocumentEvent e) {
		changed();
	}

	abstract public void changed ();
}
