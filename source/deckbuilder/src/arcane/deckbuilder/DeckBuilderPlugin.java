
package arcane.deckbuilder;

import arcane.Plugin;
import arcane.deckbuilder.ui.DeckBuilder;
import arcane.ui.util.ProgressDialog;

public abstract class DeckBuilderPlugin extends Plugin {
	public void install (ProgressDialog dialog) {
	}

	abstract public void install (DeckBuilder deckBuilder);
}
