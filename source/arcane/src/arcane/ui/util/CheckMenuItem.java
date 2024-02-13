
package arcane.ui.util;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.MenuSelectionManager;

import com.sun.java.swing.plaf.windows.WindowsCheckBoxMenuItemUI;

/**
 * JCheckBoxMenuItem that doesn't close when checked or unchecked.
 */
public class CheckMenuItem extends JCheckBoxMenuItem {
	static private final MenuSelectionManager dontCloseSelectionManager = new MenuSelectionManager() {
		public void clearSelectedPath () {
			super.clearSelectedPath();
		}
	};

	public CheckMenuItem (String text) {
		super(text);
		// BOZO! - Fix.
//		if (!(getUI() instanceof WindowsCheckBoxMenuItemUI)) return;
//		setUI(new WindowsCheckBoxMenuItemUI() {
//			protected void doClick (MenuSelectionManager msm) {
//				super.doClick(CheckMenuItem.dontCloseSelectionManager);
//				setArmed(true);
//			}
//		});
	}
}
