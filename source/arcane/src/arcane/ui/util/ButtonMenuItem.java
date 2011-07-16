
package arcane.ui.util;

import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;

public class ButtonMenuItem extends MenuItem {
	private final Button button;
	private boolean isAlternate;

	public ButtonMenuItem (String text, Button button, boolean isAlternate) {
		this(text, button);
		this.isAlternate = isAlternate;
	}

	public ButtonMenuItem (String text, Button button) {
		super(text);

		this.button = button;

		setIcon(button.getIcon());
	}

	public void doClick (int pressTime) {
		if (isAlternate) button.setNextAlternate(true);
		button.doClick(0);
	}

	public void setModel (ButtonModel newModel) {
		super.setModel(new DefaultButtonModel() {
			public boolean isEnabled () {
				return ButtonMenuItem.this.isEnabled();
			}
		});
	}

	public boolean isEnabled () {
		if (button == null) return true;
		return button.isEnabled();
	}
}
