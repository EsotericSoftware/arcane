
package arcane.deckbuilder.tcgplayer;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import nu.xom.ParsingException;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.Card;
import arcane.deckbuilder.DeckBuilderPlugin;
import arcane.deckbuilder.ui.DeckBuilder;
import arcane.ui.util.ProgressDialog;
import arcane.util.CSVReader;
import arcane.util.CSVWriter;

public class TCGplayerPlugin extends DeckBuilderPlugin {
		private Map<String, String> sets;

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
			JMenuItem buyMenuItem = new JMenuItem("Buy deck...");
			menu.add(buyMenuItem);
			buyMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					buyDeck(deckBuilder);
				}
			});
		}
		deckBuilder.addPluginMenu(menu);
		if (new File("plugins/TCGplayer/prices.csv").exists()) loadPricesFromFile();
	}

	public void install (ProgressDialog dialog){

	}

	private void buyDeck (DeckBuilder deckBuilder) {
		StringBuilder sb = new StringBuilder("http://store.tcgplayer.com/list/selectproductmagic.aspx?partner=ARCANE&c=");
		String joiner = "";
		for(Entry<Card, Integer> entry : deckBuilder.getDeckCardToQty().entrySet()){
		try {
				sb.append(joiner);
				sb.append(entry.getValue());
				sb.append("%20");
				sb.append(URLEncoder.encode( entry.getKey().name , "UTF8" ));
				joiner = URLEncoder.encode( "||" , "UTF8" );
		} catch (UnsupportedEncodingException e) {
		}
		}
		for(Entry<Card, Integer> entry : deckBuilder.getSideCardToQty().entrySet()){
		try {
				sb.append(joiner);
				sb.append(entry.getValue());
				sb.append("%20");
				sb.append(URLEncoder.encode( entry.getKey().name , "UTF8" ));
				joiner = URLEncoder.encode( "||" , "UTF8" );
		} catch (UnsupportedEncodingException e) {
		}
		}
		if(Desktop.isDesktopSupported()){
		Desktop desktop = Desktop.getDesktop();
		try {
			desktop.browse(new URI(sb.toString()));
		} catch (Exception e) {
			throw new ArcaneException("Error opening browser.", e);
		}
		}
	}

	private void updatePrices (DeckBuilder deckBuilder) {
		final ProgressDialog dialog = new ProgressDialog(deckBuilder, "TCGplayer");
		dialog.setMessage("Downloading pricing information...");
		dialog.setAlwaysOnTop(true);
		dialog.setValue(-1);

		new Thread(new Runnable() {
			public void run () {

				try {
						loadSetNames();
						List<Card> cards = Arcane.getInstance().getCards();
						CSVWriter writer = new CSVWriter(new FileWriter("plugins/TCGplayer/prices.csv"));
						for(Card card : cards){
								try {
								Builder parser = new Builder();
								String set = sets.get(card.set);
								if(set == null)
									set = Arcane.getInstance().getSetName(card.set);
								String name = card.name.replaceAll("Avatar: ", "");
								//Arcane.getInstance().log("http://partner.tcgplayer.com/x/phl.asmx/p?pk=ARCANE&s="+ set + "&p=" + name);
								Document doc = parser.build("http://partner.tcgplayer.com/x/phl.asmx/p?pk=ARCANE&s="+ set + "&p=" + name);
								doc = parser.build(doc.getChild(0).getValue(), "http://store.tcgplayer.com/");
								Nodes nodes = doc.query("//avgprice");

								if(nodes.size() != 1)
									continue;
								writer.writeField(card.name);
								writer.writeField(card.set);
								writer.writeField(nodes.get(0).getValue());
								nodes = doc.query("//link");
								writer.writeField(nodes.get(0).getValue());
								writer.newLine();
								}
								catch (ParsingException ex) {
								System.err.println("Parsing error");
								}
								catch (IOException ex) {
								System.err.println("Could not connect to TCGplayer. The site may be down.");
								}
						}
						writer.close();
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

	private void loadSetNames() {
		if(sets == null) {
			CSVReader reader = null;
				try {
				FileInputStream input = new FileInputStream("plugins/TCGplayer/sets.csv");
				reader = new CSVReader(new InputStreamReader(new BufferedInputStream(input)), ",", "\"", true, false);
				sets = new HashMap<String, String>();
				while (true) {
				List<String> fields = reader.getFields();
				if (fields == null) break;
				if (fields.size() != 2) throw new ArcaneException("Invalid set data: " + fields);
				sets.put(fields.get(0).toLowerCase(), fields.get(1));
			}
				} catch (Exception ex) {
				throw new ArcaneException("Error loading TCGplayer set information.", ex);
			} finally {
			if(reader != null){
				try {
				reader.close();
				} catch (IOException e) {
				}
			}
				}
		}
	}

	private void loadPricesFromFile () {
		CSVReader reader = null;
		try {
		FileInputStream input = new FileInputStream("plugins/TCGplayer/prices.csv");
		reader = new CSVReader(new InputStreamReader(new BufferedInputStream(input)), ",", "\"", true, false);
		while (true) {
			List<String> fields = reader.getFields();
			if (fields == null) break;
			if (fields.size() != 4) throw new ArcaneException("Invalid set data: " + fields + " " + fields.size());
			String title = fields.get(0);
			String set = fields.get(1);
			float price = round(Float.valueOf(fields.get(2)), 2);
			String url = fields.get(3);;
			List<Card> cards = Arcane.getInstance().getCards(title, set);
			for (Card card : cards) {
				card.price = price;
				card.shopUrl = url;
			}
		}
		} catch (Exception ex) {
		throw new ArcaneException("Error loading TCGplayer set information.", ex);
		} finally {
		if(reader != null){
			try {
			reader.close();
			} catch (IOException e) {
			}
		}
		}
	}

	public static float round(float rVal, int rPl) {
		float p = (float)Math.pow(10,rPl);
		rVal = rVal * p;
		float tmp = Math.round(rVal);
		return tmp/p;
		}

	public String getName () {
		return "TCGplayer";
	}
}
