
package arcane;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import arcane.util.CSVReader;
import arcane.util.FileUtil;

public class DecklistFile implements Decklist {
	static private StringTemplateGroup templates = new StringTemplateGroup("templates", "templates");
	static private JFileChooser openFileChooser;

	public final File file;
	public final String templateName;

	private List<DecklistCard> decklistCards = new ArrayList<DecklistCard>();

	public DecklistFile (String path, String templateName) {
		this.file = new File(path);
		this.templateName = templateName;
	}

	public void open () throws IOException {
		if (!file.exists()) return;

		decklistCards.clear();

		BufferedReader reader = new BufferedReader(new FileReader(file));
		String extension = FileUtil.getExtension(file.getName()).toLowerCase();

		if (extension.equals("csv"))
			openCSV(reader);
		else if (extension.equals("mwdeck"))
			openMWS(reader);
		else if (extension.equals("txt"))
			openMTGO(reader);
		else if (extension.equals("dec"))
			openDEC(reader);
		else if (extension.equals("dck"))
			openMWS(reader);
		else
			throw new UnsupportedOperationException("Unable to open this file type: " + extension);
	}

	public List<DecklistCard> getDecklistCards () {
		return decklistCards;
	}

	public int[] getCardIDs (boolean sideboard) {
		List<Integer> cardIDs = new ArrayList<Integer>();
		for (DecklistCard card : decklistCards)
			if (card.isSideboard() == sideboard) cardIDs.add(card.getId());
		int[] ids = new int[cardIDs.size()];
		int i = 0;
		for (Integer id : cardIDs)
			ids[i++] = id;
		return ids;
	}

	private void openDEC (BufferedReader reader) throws IOException {
		StringBuffer errorBuffer = new StringBuffer(64);
		Pattern linePattern = Pattern.compile("(\\d+) (.+)");
		while (true) {
			String line = reader.readLine();
			if (line == null) break;

			boolean side = line.startsWith("SB:");
			if (side) line = line.substring(3);

			Matcher lineMatcher = linePattern.matcher(line.trim());
			if (!lineMatcher.matches()) continue;

			String cardName = lineMatcher.group(2);

			int qty = 1;
			try {
				qty = Integer.parseInt(lineMatcher.group(1));
			} catch (NumberFormatException ignored) {
				errorBuffer.append("Invalid qty: " + cardName + "\n");
			}

			Card card = Arcane.getInstance().getCard(cardName);
			if (card == null)
				errorBuffer.append("Unknown card: " + cardName + "\n");
			else {
				for (int i = 0; i < qty; i++)
					decklistCards.add(new DecklistCard(card, side));
			}
		}
		reader.close();
		if (errorBuffer.length() > 0) throw new ArcaneException(errorBuffer.toString());
	}

	private void openMWS (BufferedReader reader) throws IOException {
		StringBuffer errorBuffer = new StringBuffer(64);
		Pattern linePattern = Pattern.compile("(\\d+) \\[(.+)\\] (.+)");
		Pattern namePattern = Pattern.compile("(.+) \\((\\d+)\\)");
		while (true) {
			String line = reader.readLine();
			if (line == null) break;

			boolean side = line.startsWith("SB:");
			if (side) line = line.substring(3);

			Matcher lineMatcher = linePattern.matcher(line.trim());
			if (!lineMatcher.matches()) continue;

			String cardName = lineMatcher.group(3);

			int qty = 1;
			try {
				qty = Integer.parseInt(lineMatcher.group(1));
			} catch (NumberFormatException ignored) {
				errorBuffer.append("Invalid qty: " + cardName + "\n");
			}

			String version = "";
			Matcher nameMatcher = namePattern.matcher(cardName);
			if (nameMatcher.matches()) {
				cardName = nameMatcher.group(1);
				if (nameMatcher.groupCount() > 1) {
					try {
						version = nameMatcher.group(2);
					} catch (NumberFormatException ignored) {
					}
				}
			}

			String set = Arcane.getInstance().getMainSet(lineMatcher.group(2));

			Card card = Arcane.getInstance().getCard(cardName, set, version);
			for (int i = 0; i < qty; i++)
				decklistCards.add(new DecklistCard(card, side));
		}
		reader.close();
		if (errorBuffer.length() > 0) throw new ArcaneException(errorBuffer.toString());
	}

	private void openMTGO (BufferedReader reader) throws IOException {
		StringBuffer errorBuffer = new StringBuffer(64);
		Pattern pattern = Pattern.compile("(\\d+) (.+)");
		boolean mainDeck = true;
		while (true) {
			String line = reader.readLine();
			if (line == null) break;

			line = line.trim();

			if (line.equals("Sideboard")) {
				mainDeck = false;
				continue;
			}

			line = line.replaceAll(" \\(\\d+\\)$", "");

			Matcher matcher = pattern.matcher(line);
			if (!matcher.matches()) continue;

			int qty = 1;
			try {
				qty = Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException ignored) {
			}

			String cardName = matcher.group(2);
			Card card = Arcane.getInstance().getCard(cardName);
			for (int i = 0; i < qty; i++)
				decklistCards.add(new DecklistCard(card, !mainDeck));
		}
		reader.close();
		if (errorBuffer.length() > 0) throw new ArcaneException(errorBuffer.toString());
	}

	private void openCSV (BufferedReader fileReader) throws IOException {
		Pattern namePattern = Pattern.compile("(.+) \\((.+?)\\)");
		CSVReader reader = new CSVReader(fileReader);
		boolean mainDeck = true;
		StringBuffer errorBuffer = new StringBuffer(64);
		while (true) {
			List<String> fields = reader.getFields();
			if (fields == null) break;

			int qty = 1;
			String cardName = "";

			int size = fields.size();
			if (size == 1)
				cardName = fields.get(0);
			else if (size > 1) {
				try {
					qty = Integer.parseInt(fields.get(0));
				} catch (NumberFormatException ignored) {
				}
				cardName = fields.get(1);
			}
			if (cardName.trim().length() == 0) {
				if (mainDeck) {
					mainDeck = false;
					continue;
				} else
					break;
			}

			String version = "";
			Matcher nameMatcher = namePattern.matcher(cardName);
			if (nameMatcher.matches()) {
				cardName = nameMatcher.group(1);
				if (nameMatcher.groupCount() > 1) {
					try {
						version = nameMatcher.group(2);
					} catch (NumberFormatException ignored) {
					}
				}
			}

			String set = null;
			if (fields.size() > 2) {
				set = fields.get(2).toLowerCase();
				set = Arcane.getInstance().getMainSet(set);
			}

			Card card = Arcane.getInstance().getCard(cardName, set, version);
			for (int i = 0; i < qty; i++)
				decklistCards.add(new DecklistCard(card, !mainDeck));
		}
		reader.close();

		if (errorBuffer.length() > 0) throw new ArcaneException(errorBuffer.toString());
	}

	public boolean exists () {
		return file.exists();
	}

	public String getData () {
		return file.getAbsolutePath() + '|' + templateName;
	}

	public String getName () {
		String name = file.getName();
		if (name.lastIndexOf('.') != -1) name = name.substring(0, name.lastIndexOf('.'));
		return name;
	}

	public boolean isOpenable () {
		return templateName.equals("CSV (csv)") || templateName.equals("MWS (mwDeck)") || templateName.equals("MTGO (txt)")
			|| templateName.equals("Apprentice (dec)") || templateName.equals("Online PlayTable (dck)");
	}

	public boolean equals (Object obj) {
		if (!(obj instanceof DecklistFile)) return false;
		return ((DecklistFile)obj).file.getAbsolutePath().equals(file.getAbsolutePath());
	}

	public String toString () {
		return file.getAbsolutePath();
	}

	public Map<Card, Integer> computeCardToQty(List<Card> deckCards){
		Map<Card, Integer> deckCardToQty = new HashMap<Card, Integer>();
		for (Card card : deckCards) {
			Integer qty = deckCardToQty.get(card);
			if (qty == null) {
				qty = 0;
			}
			qty++;
			deckCardToQty.put(card, qty);
		}
		return deckCardToQty;
	}

	public void save (List<Card> deckCards, Map<Card, Integer> deckCardToQty, List<Card> sideCards, Map<Card, Integer> sideCardToQty) throws IOException {
		decklistCards.clear();

		List<Card> templateDeckCards = new ArrayList<Card>();
		if(deckCardToQty != null){
			for (Card card : deckCards){
				int qty = deckCardToQty.get(card);
				for (int i = 0; i < qty; i++)
					decklistCards.add(new DecklistCard(card, false));

				templateDeckCards.add(new TemplateCard(card, qty));
			}
		} else{
			Map<Card, Integer> tmpDeckCardToQty = computeCardToQty(deckCards);
			for (Map.Entry<Card, Integer> entry : tmpDeckCardToQty.entrySet()){
			    	Card card = entry.getKey();
				int qty = entry.getValue();
				for (int i = 0; i < qty; i++)
					decklistCards.add(new DecklistCard(card, false));

				templateDeckCards.add(new TemplateCard(card, qty));
			}
		}

		List<Card> templateSideCards = new ArrayList<Card>();
		if(deckCardToQty != null){
			for (Card card : sideCards){
				int qty = sideCardToQty.get(card);
				for (int i = 0; i < qty; i++)
					decklistCards.add(new DecklistCard(card, true));

				templateSideCards.add(new TemplateCard(card, qty));
			}
		} else{
			Map<Card, Integer> tmpSideCardToQty = computeCardToQty(sideCards);
			for (Map.Entry<Card, Integer> entry : tmpSideCardToQty.entrySet()){
			    	Card card = entry.getKey();
				int qty = entry.getValue();
				for (int i = 0; i < qty; i++)
					decklistCards.add(new DecklistCard(card, true));

				templateSideCards.add(new TemplateCard(card, qty));
			}
		}

		StringTemplate template = templates.getInstanceOf(templateName);
		template.setAttribute("deckName", file.getName().replaceAll("\\.[^\\.]*$", ""));
		template.setAttribute("deckCards", templateDeckCards);
		template.setAttribute("sideCards", templateSideCards);

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(template.toString());
		writer.close();
	}

	public void addCards (List<Card> newCards) {
		try {
			List<Card>[] decklistCards = DecklistCard.getCards(getDecklistCards());
			List<Card> mainCards = decklistCards[0];
			List<Card> sideCards = decklistCards[1];
			for (Card newCard : newCards) {
				if (mainCards.contains(newCard) || sideCards.contains(newCard)) continue;
				mainCards.add(newCard);
			}
			save(mainCards, null, sideCards, null);
		} catch (IOException ex) {
			throw new ArcaneException("Error adding cards to decklist: " + file.getAbsolutePath(), ex);
		}
	}

	static public synchronized DecklistFile open (Component parent, DecklistFile currentFile) {
		if (openFileChooser == null) {
			openFileChooser = new JFileChooser("decks");
			openFileChooser.setAcceptAllFileFilterUsed(false);
			openFileChooser.addChoosableFileFilter(FileUtil.chooserFileEndsWith("Apprentice (*.dec)", ".dec"));
			openFileChooser.addChoosableFileFilter(FileUtil.chooserFileEndsWith("MTGO (*.txt)", ".txt"));
			openFileChooser.addChoosableFileFilter(FileUtil.chooserFileEndsWith("MWS (*.mwDeck)", ".mwdeck"));
			openFileChooser.addChoosableFileFilter(FileUtil.chooserFileEndsWith("CSV (*.csv)", ".csv"));
			openFileChooser.addChoosableFileFilter(FileUtil.chooserFileEndsWith("Online PlayTable (*.dck)", ".dck"));
		}

		if (currentFile != null) {
			openFileChooser.setCurrentDirectory(currentFile.file);
			for (FileFilter fileFilter : openFileChooser.getChoosableFileFilters()) {
				if (fileFilter.getDescription().replace("*.", "").equals(currentFile.templateName)) {
					openFileChooser.removeChoosableFileFilter(fileFilter);
					openFileChooser.addChoosableFileFilter(fileFilter);
					break;
				}
			}
		}

		int result = openFileChooser.showOpenDialog(parent);
		if (result != JFileChooser.APPROVE_OPTION) return null;

		String path = openFileChooser.getSelectedFile().getAbsolutePath();
		String templateName = openFileChooser.getFileFilter().getDescription().replace("*.", "");

		if (!openFileChooser.getSelectedFile().exists()) {
			// Append the template's extension to the path if it has no extension.
			String extension = FileUtil.getExtension(path);
			if (extension == null) {
				Matcher matcher = Pattern.compile(".+\\((.+)\\)$").matcher(templateName);
				matcher.matches();
				File file = new File(path);
				path = new File(file.getParent(), file.getName() + '.' + matcher.group(1)).getAbsolutePath();
			}
		}

		return new DecklistFile(path, templateName);
	}
}
