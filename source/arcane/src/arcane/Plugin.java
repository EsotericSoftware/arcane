
package arcane;

import arcane.ui.util.ProgressDialog;

public abstract class Plugin {
	abstract public void install (ProgressDialog dialog);

	abstract public String getName ();

	public void savePreferences () {
	}

	public void loadPreferences () {
	}
}
