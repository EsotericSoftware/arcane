
package arcane.deckbuilder.mwsplay;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import arcane.ArcaneException;
import arcane.DecklistFile;
import arcane.deckbuilder.DeckBuilderPlugin;
import arcane.deckbuilder.ui.DeckBuilder;
import arcane.ui.util.Menu;

public class MWSPlayPlugin extends DeckBuilderPlugin {
	public void install (final DeckBuilder deckBuilder) {
		JMenu menu = new Menu(getName());
		{
			JMenuItem loadMenuItem = new JMenuItem("Open with MWSPlay...");
			menu.add(loadMenuItem);
			loadMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					try {
						File tempFile = File.createTempFile("deckbuilder", "mwsplay");
						deckBuilder.saveDecklist(new DecklistFile(tempFile.getAbsolutePath(), "MWS (mwDeck)"), false);
						Runtime.getRuntime().exec("plugins/MWSPlay/MWSPlay/solitaire.bat \"" + tempFile.getAbsolutePath() + "\"");
					} catch (IOException ex) {
						throw new ArcaneException("Error running MWSPlay.", ex);
					}
				}
			});
		}
		deckBuilder.addPluginMenu(menu);
	}

	public String getName () {
		return "MWS Play";
	}
}
