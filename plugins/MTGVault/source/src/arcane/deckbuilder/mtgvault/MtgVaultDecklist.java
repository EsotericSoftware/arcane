
package arcane.deckbuilder.mtgvault;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.Card;
import arcane.Decklist;
import arcane.DecklistCard;
import arcane.ui.util.ProgressDialog;
import arcane.util.CSVReader;

public class MtgVaultDecklist implements Decklist {
	public String name;
	public String id;

	private List<DecklistCard> decklistCards = new ArrayList<DecklistCard>();

	public MtgVaultDecklist (String name, String id) {
		this.name = name;
		this.id = id;
	}

	public List<DecklistCard> getDecklistCards () {
		return decklistCards;
	}

	public void open () throws IOException {
		final ProgressDialog dialog = new ProgressDialog("MTG Vault");
		dialog.setMessage("Loading deck from vault: " + name);
		dialog.setAlwaysOnTop(true);
		dialog.setValue(-1);

		decklistCards.clear();

		class LoadDecklist implements Runnable {
			public IOException ex;

			public void run () {
				try {
					StringBuffer errorBuffer = new StringBuffer(64);
					MtgVaultPlugin plugin = MtgVaultPlugin.getInstance();
					if (plugin == null) return;
					CSVReader reader = plugin.getCsvReader("loadDeck", "deckid=" + id);
					while (true) {
						List<String> fields = reader.getFields();
						if (fields == null) break;
						if (fields.size() != 3) throw new ArcaneException("MTG Vault returned invalid data: " + fields);

						String cardName = fields.get(1).replace("''", "'");
						String set = Arcane.getInstance().getMainSet(fields.get(2));

						int qty = 1;
						try {
							qty = Integer.parseInt(fields.get(0));
						} catch (NumberFormatException ignored) {
							errorBuffer.append("Invalid qty: " + cardName + "\n");
						}

						Card card = Arcane.getInstance().getCard(cardName, set);
						for (int i = 0; i < qty; i++)
							decklistCards.add(new DecklistCard(card, false));
					}
					reader.close();
					if (errorBuffer.length() > 0) throw new ArcaneException(errorBuffer.toString());
				} catch (IOException ex) {
					this.ex = ex;
				} finally {
					dialog.setVisible(false);
				}
			}
		}
		LoadDecklist runnable = new LoadDecklist();
		new Thread(runnable, "LoadFromVault").start();

		dialog.setVisible(true);
		dialog.dispose();

		if (runnable.ex != null) throw runnable.ex;
	}

	public void save (List<Card> deckCards, Map<Card, Integer> deckCardToQty, List<Card> sideCards, Map<Card, Integer> sideCardToQty) throws IOException {
		final ProgressDialog dialog = new ProgressDialog("MTG Vault");
		dialog.setMessage("Saving deck to vault: " + name);
		dialog.setAlwaysOnTop(true);
		dialog.setValue(-1);

		class SaveDecklist implements Runnable {
			public IOException ex;

			public void run () {
				try {
					MtgVaultPlugin plugin = MtgVaultPlugin.getInstance();
					if (plugin == null) return;
					// BOZO - Save deck.
					dialog.setMessage("Saving to MTG Vault is not yet implemented!");
					try {
						Thread.sleep(2500);
					} catch (InterruptedException ignored) {
					}
					/*
					 * CSVReader reader; if (id == null) reader = plugin.getCsvReader("saveDeck", "deckname=" + name); else reader =
					 * plugin.getCsvReader("saveDeck", "deckid=" + id); reader.close();
					 */
					if (false) throw new IOException();
				} catch (IOException ex) {
					this.ex = ex;
				} finally {
					dialog.setVisible(false);
				}
			}
		}
		SaveDecklist runnable = new SaveDecklist();
		new Thread(runnable, "SaveToVault").start();

		dialog.setVisible(true);
		dialog.dispose();

		if (runnable.ex != null) throw runnable.ex;
	}

	public boolean exists () {
		return true;
	}

	public String getData () {
		return name + "|" + id;
	}

	public String getName () {
		return name;
	}

	public boolean isOpenable () {
		return true;
	}

	public boolean equals (Object obj) {
		if (!(obj instanceof MtgVaultDecklist)) return false;
		if (id == null) return false;
		return id.equals(((MtgVaultDecklist)obj).id);
	}

	public String toString () {
		return name;
	}
}
