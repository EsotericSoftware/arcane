
package arcane.deckbuilder.magictraders;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.Card;
import arcane.deckbuilder.DeckBuilderPlugin;
import arcane.deckbuilder.ui.DeckBuilder;
import arcane.ui.util.ProgressDialog;

public class MagicTradersPlugin extends DeckBuilderPlugin {
	public void install (final DeckBuilder deckBuilder) {
		JMenu menu = new JMenu(getName());
		{
			JMenuItem loadMenuItem = new JMenuItem("Update prices...");
			menu.add(loadMenuItem);
			loadMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					updatePrices(deckBuilder);
				}
			});
		}
		deckBuilder.addPluginMenu(menu);
		if (new File("plugins/MagicTraders/prices.txt").exists()) loadPricesFromFile();
	}

	public void install (ProgressDialog dialog){

	}

	private void updatePrices (DeckBuilder deckBuilder) {
		final ProgressDialog dialog = new ProgressDialog(deckBuilder, "Magic Traders");
		dialog.setMessage("Downloading pricing information...");
		dialog.setAlwaysOnTop(true);
		dialog.setValue(-1);

		new Thread(new Runnable() {
			public void run () {
				try {
					InputStream input = new URL("http://www.magictraders.com/pricelists/current-magic").openStream();
					OutputStream out = new FileOutputStream("plugins/MagicTraders/prices.txt");
					try {
						byte[] buffer = new byte[2048];
						int bytesRead;
						while ((bytesRead = input.read(buffer)) > 0)
							out.write(buffer, 0, bytesRead);
					} finally {
						out.close();
						input.close();
					}
					dialog.setMessage("Loading prices...");
					loadPricesFromFile();
				} catch (IOException ex) {
					throw new ArcaneException("Error downloading pricing information.", ex);
				} finally {
					dialog.setVisible(false);
				}
			}
		}, "DownloadPricing").start();

		dialog.setVisible(true);
		dialog.dispose();
	}

	private void loadPricesFromFile () {
		try {
		    Map<String, Map<String, Float>> titleToSetToPrice = new HashMap<String, Map<String,Float>>();
		    BufferedReader reader = null;
		    try {
			reader = new BufferedReader(new FileReader("plugins/MagicTraders/prices.txt"));

			while (true) {
				String line = reader.readLine();
				if (line == null) break;
				if (line.startsWith("total:")) continue;
				int commaIndex = line.indexOf(",  ");
				if (commaIndex == -1) continue;

				String title = line.substring(0, commaIndex).toLowerCase();
				String set = null;
				if (title.endsWith(")")) {
					set = title.substring(title.indexOf("(") + 1, title.length() - 1);

					if (set.equals("bk")) continue;

					if (set.equals("st1"))
						set = "ST";
					else if (set.equals("st2"))
						set = "ST";
					else if (set.equals("csp"))
						set = "CLD";
					else if (set.equals("uz")) {
						set = "USG";
					}

					title = title.substring(0, title.indexOf("(") - 1);
				}

				String priceString = line.substring(commaIndex+1, line.indexOf(",", commaIndex+1)).trim();
				float price;
				try {
					price = Float.parseFloat(priceString);
				} catch (NumberFormatException ex) {
					System.out.println("MagicTraders: Invalid price \"" + priceString + "\" for card: " + title);
					continue;
				}

				if (titleToSetToPrice.get(title) == null) titleToSetToPrice.put(title, new HashMap<String, Float>());
				if (titleToSetToPrice.get(title).get("") == null || titleToSetToPrice.get(title).get("") < price)
					titleToSetToPrice.get(title).put("", price);

				if (set != null && set.length() > 0) {
					String mainSet = Arcane.getInstance().getMainSet(set);
					if (mainSet != null)
						titleToSetToPrice.get(title).put(mainSet, price);
					else
						Arcane.getInstance().log("MagicTraders: Unknown set \"" + set + "\" for card: " + title);
				}
			}
		    } finally {
			if(reader != null)
			    reader.close();
		    }

			for (Entry<String, Map<String, Float>> setToPrice : titleToSetToPrice.entrySet()) {
				String title = setToPrice.getKey();
				if(title.contains("token"))
					title = title.substring(0, title.indexOf("token")-1);
				else if(title.equals("ach! hans, run!"))
					title ="\"ach! hans, run!\"";
				else if(title.equals("longest card name ever elemental"))
					title = "our market research shows that players like really long card names so we made th";
				else if(title.equals("no name"))
					continue;
				else if(title.equals("pang tong, young phoenix"))
					title = "pang tong, \"young phoenix\"";
				else if(title.equals("question elemental?"))
					title = "question elemental";
				else if(title.equals("kongming, sleeping dragon"))
					title = "kongming, \"sleeping dragon\"";
				else if(title.equals("who/what/when/where/why"))
					title = title.replace("/", " ");
				try {
					List<Card> cards = Arcane.getInstance().getCards(title);
					Float defaultPrice = setToPrice.getValue().get("");
					for (Card card : cards) {
						Float price = setToPrice.getValue().get(card.set);
						if(price == null)
							price = defaultPrice;
						card.price = price;
					}
				} catch(ArcaneException e) {
					//card not found, skip it
					System.out.println(e);
				}

			}
		} catch (IOException ex) {
			throw new ArcaneException("Error downloading pricing information.", ex);
		}
	}

	public String getName () {
		return "Magic Traders";
	}
}
