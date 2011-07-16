
package arcane.ui.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

public abstract class LayoutManager implements java.awt.LayoutManager {
	public void addLayoutComponent (String name, Component comp) {
	}

	public void layoutContainer (Container parent) {
		layout();
	}

	public Dimension minimumLayoutSize (Container parent) {
		return null;
	}

	public Dimension preferredLayoutSize (Container parent) {
		return null;
	}

	public void removeLayoutComponent (Component comp) {
	}

	abstract public void layout ();
}
