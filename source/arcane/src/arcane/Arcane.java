
package arcane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import arcane.CardDataStore.CardDataStoreConnection;
import arcane.RulesDataStore.RulesDataStoreConnection;
import arcane.RulingsDataStore.RulingsDataStoreConnection;
import arcane.ui.ManaSymbols;
import arcane.ui.table.FlagRenderer;
import arcane.ui.table.RatingRenderer;
import arcane.ui.util.MessageFrame;
import arcane.ui.util.ProgressDialog;
import arcane.ui.util.TextComponentOutputStream;
import arcane.ui.util.UI;
import arcane.util.CSVReader;
import arcane.util.CSVWriter;
import arcane.util.FileUtil;
import arcane.util.Loader;
import arcane.util.MultiplexOutputStream;
import arcane.util.UnicodeReader;

public class Arcane {
	static public final String version = "0.18";

	private ArcanePreferences prefs;
	private ArcaneTranslation trans;
	private Map<Integer, Card> idToCard = new HashMap<Integer, Card>();
	private Map<String, List<Card>> nameToCards = new HashMap<String, List<Card>>();
	private List<Card> allCards = new ArrayList<Card>();
	private List<String> languages = new ArrayList<String>();
	private List<String> uiLanguages = new ArrayList<String>();
	private Map<String, String> setToMainSet = new LinkedHashMap<String, String>();
	private Map<String, String> setToName = new HashMap<String, String>();
	private Map<String, Integer> setToOrdinal = new LinkedHashMap<String, Integer>();
	private Map<Format, Set<String>> formatToSets = new HashMap<Format, Set<String>>();
	private CardDataStore cardDataStore;
	private RulingsDataStore rulingsDatastore;
	private RulesDataStore rulesDatastore;
	private MessageFrame logFrame;
	private UncaughtExceptionHandler exceptionHandler;
	private List<Plugin> plugins = new ArrayList<Plugin>();
	private PluginClassLoader pluginClassLoader = new PluginClassLoader();
	private Map<String, Set<Format>> cardNameToBannedFormats = new HashMap<String, Set<Format>>();
	private Map<String, Set<Format>> cardNameToRestrictedFormats = new HashMap<String, Set<Format>>();
	private boolean loadRuleData;

	static private Arcane instance = null;

	private Arcane (boolean loadRuleData) {
		this.loadRuleData = loadRuleData;
	}

	static public void setup (String prefsFileName, String logFileName, boolean loadRuleData) {
		if (instance != null) return;
		File logDir = new File("logs");
		logDir.mkdir();
		File logFile = new File(logDir, logFileName);
		logFile.delete();
		try {
			OutputStream logFileStream = new BufferedOutputStream(new FileOutputStream(logFile));
			System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logFileStream), true));
			System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logFileStream), true));
		} catch (FileNotFoundException ex) {
			throw new ArcaneException("Error setting up logging.", ex);
		}

		System.out.println("Arcane v" + version);
		System.out.println();

		UI.setSystemLookAndFeel();
		//UI.setDefaultFont(Font.decode("Tahoma-11"));

		instance = new Arcane(loadRuleData);
		Loader loader = instance.new DataLoader("Arcane v" + Arcane.version, prefsFileName);
		loader.start("ArcaneLoader");
		if (loader.failed()) throw new ArcaneException("Arcane initialization aborted.");
	}

	static public synchronized Arcane getInstance () {
		if (instance == null) throw new IllegalStateException("Arcane not setup.");
		return instance;
	}

	/**
	 * Returns the standard set abbreviation (interned) for the specified set.
	 */
	public String getMainSet (String set) {
		if (set == null) return "";
		return setToMainSet.get(set.toLowerCase());
	}

	public Set<String> getAlternateSets (String set) {
		set = getMainSet(set);
		Set<String> alternateSets = new LinkedHashSet<String>();
		for (Entry<String, String> entry : setToMainSet.entrySet())
			if (entry.getValue() == set) alternateSets.add(entry.getKey());
		return alternateSets;
	}

	public String getSetName (String set) {
		return setToName.get(getMainSet(set));
	}

	public Integer getSetOrdinal (String set) {
		return setToOrdinal.get(getMainSet(set));
	}

	public Set<String> getSets () {
		return setToOrdinal.keySet();
	}

	public Set<String> getSets (String cardName) {
		Set<String> sets = new LinkedHashSet<String>();
		for (Card card : getCards(cardName))
			sets.add(card.set);
		return sets;
	}

	public Set<String> getVersions (String cardName, String set) {
		Set<String> versions = new HashSet<String>();
		for (Card card : getCards(cardName, set))
			versions.add(card.version);
		return versions;
	}

	public Set<String> getFormatSets (Format format) {
		return formatToSets.get(format);
	}

	public List<String> getLanguages () {
		return languages;
	}

	public List<String> getUILanguages () {
		return uiLanguages;
	}

	public List<Card> getCards () {
		return allCards;
	}

	public List<Card> getCards (String name) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		List<Card> cards = nameToCards.get(name.toLowerCase());
		if (cards == null) throw new ArcaneException("Card not found: " + name);
		return cards;
	}

	public List<Card> getCards (String name, String set) throws ArcaneException {
		if (set == null) return getCards(name);
		set = getMainSet(set);
		List<Card> cards = new ArrayList<Card>();
		for (Card card : getCards(name))
			if (card.set.equals(set)) cards.add(card);
		if (cards.isEmpty()) throw new ArcaneException("Card \"" + name + "\" not found with set: " + set);
		return cards;
	}

	public Card getCard (int cardID) {
		Card card = idToCard.get(cardID);
		if (card == null) throw new ArcaneException("Card ID not found: " + cardID);
		return card;
	}

	public Card getCard (String name) {
		return getCards(name).get(0);
	}

	public Card getRandomCard () {
		return allCards.get(new Random().nextInt(allCards.size()));
	}

	public Card getCard (String name, String set) {
		if (set == null) return getCard(name);
		set = getMainSet(set);
		for (Card card : getCards(name))
			if (card.set.equals(set)) return card;
		throw new ArcaneException("Card \"" + name + "\" not found with set: " + set);
	}

	public Card getCard (String name, String set, String version) {
		if ("".equals(version)) return getCard(name, set);
		for (Card card : getCards(name, set))
			if (card.version.equals(version)) return card;
		throw new ArcaneException("Card \"" + name + "\" not found in set \"" + set + "\" with version: " + version);
	}

	public ArcanePreferences getPrefs () {
		return prefs;
	}

	public ArcaneTranslation getTrans () {
		return trans;
	}

	public CardDataStoreConnection getCardDataStoreConnection () throws SQLException {
		return cardDataStore.getThreadConnection();
	}

	public RulesDataStoreConnection getRulesDataStoreConnection () throws SQLException {
		return rulesDatastore.getThreadConnection();
	}

	public RulingsDataStoreConnection getRulingsDataStoreConnection () throws SQLException {
		return rulingsDatastore.getThreadConnection();
	}

	public MessageFrame getLogFrame () {
		return logFrame;
	}

	public void log (String message) {
		System.out.println(message);
	}

	public void log (Exception ex) {
		ex.printStackTrace();
	}

	public void log (String message, Exception ex) {
		System.out.println(message);
		ex.printStackTrace();
	}

	public void logError (Exception ex) {
		exceptionHandler.uncaughtException(Thread.currentThread(), ex);
	}

	public void logError (String message) {
		logError(new ArcaneException(message));
	}

	public void logError (String message, Exception ex) {
		logError(new ArcaneException(message, ex));
	}

	public void showLogFrame () {
		if (logFrame != null) logFrame.setVisible(true);
	}

	public boolean isBanned (String englishCardName, Format format) {
		Set<Format> bannedFormats = cardNameToBannedFormats.get(englishCardName.toLowerCase());
		if (bannedFormats == null) return false;
		return bannedFormats.contains(format);
	}

	public boolean isRestricted (String englishCardName, Format format) {
		Set<Format> restrictedFormats = cardNameToRestrictedFormats.get(englishCardName.toLowerCase());
		if (restrictedFormats == null) return false;
		return restrictedFormats.contains(format);
	}

	public void saveUserData () throws IOException {
		boolean wroteEntries = false;
		CSVWriter writer = new CSVWriter(new FileWriter("userData.csv"));
		writer.writeField("Name");
		writer.writeField("Set");
		writer.writeField("Pic");
		writer.writeField("Owned Qty");
		writer.writeField("Rating");
		writer.writeField("Flags");
		writer.newLine();
		for (Card card : getCards()) {
			if (card.rating > 0 || card.flags.length() > 0) {
				writer.writeField(card.englishName);
				writer.writeField(); // set
				writer.writeField(); // pictureNumber
				writer.writeField(); // owned qty

				if (card.rating > 0)
					writer.writeField(card.rating);
				else
					writer.writeField();

				if (card.flags.length() > 0)
					writer.writeField(card.flags);
				else
					writer.writeField();
				writer.newLine();
				wroteEntries = true;
			}

			if (card.ownedQty > 0) {
				writer.writeField(card.englishName);
				writer.writeField(card.set);
				if (!"".equals(card.version))
					writer.writeField(card.version);
				else
					writer.writeField();
				writer.writeField(card.ownedQty);
				writer.newLine();
				wroteEntries = true;
			}
		}
		writer.close();
		if (!wroteEntries) new File("userData.csv").delete();
	}

	public int getTotalOwnedQty (String cardName) {
		int qty = 0;
		for (Card card : getCards(cardName))
			qty += card.ownedQty;
		return qty;
	}

	public List<Plugin> getPluginsList(){
		return plugins;
	}

	/**
	 * Looks up a class that might be defined in a plugin JAR.
	 */
	public Class<?> getPluginClass (String pluginClassName) throws ClassNotFoundException {
		return Class.forName(pluginClassName, true, pluginClassLoader);
	}

	private class DataLoader extends Loader {
		private String prefsFileName;
		private String transFileName;
		private String dataDir;

		public DataLoader (String title, String prefsFileName) {
			super(title);
			this.prefsFileName = prefsFileName;
			dataDir = new File("data-test").exists() ? "data-test/" : "data/";
		}

		public void load () throws Exception {
			// Check if we are running from the right directory.
			if (!new File("data").exists()) {
				MessageFrame errorFrame = getErrorFrame();
				errorFrame.appendText("Data file not found:\n");
				errorFrame.appendText(new File("data").getAbsolutePath());
				errorFrame.appendText("\n\n");
				errorFrame.appendText("This application must be started from its own directory.\n");
				errorFrame.appendText("\n");
				errorFrame.appendText("Current directory:\n");
				errorFrame.appendText(new File("").getAbsolutePath());
				errorFrame.appendText("\n\n");
				URL url = Arcane.class.getProtectionDomain().getCodeSource().getLocation();
				errorFrame.appendText("Required directory:\n");
				File dir = new File(url.toURI());
				if (dir.isFile()) dir = dir.getParentFile();
				dir = dir.getParentFile();
				errorFrame.appendText(dir.getAbsolutePath());
				errorFrame.appendText("\n\n");
				errorFrame.appendText("To fix the problem, try running this application from the command line.\n");
				errorFrame.appendText("Open a command line, change to the required directory, then execute:\n");
				errorFrame.appendText("java -jar Xxx.jar\n");
				errorFrame.appendText("Replace Xxx.jar with the JAR file you want to run.\n\n\n");
				errorFrame.setVisible(true);
				cancel();
				return;
			}

			// Displays errors logged before and after initialization.
			logFrame = new MessageFrame("Log Viewer - Arcane v" + Arcane.version);
			logFrame.addButton("Clear").addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					logFrame.editorPane.setText("");
				}
			});
			logFrame.addButton("Close");
			logFrame.editorPane.setContentType("text/plain; charset=UTF-8");
			logFrame.editorPane.putClientProperty( javax.swing.JEditorPane.HONOR_DISPLAY_PROPERTIES, true );
			TextComponentOutputStream logStream = new TextComponentOutputStream(logFrame.editorPane);
			System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logStream), true));
			System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logStream), true));

			//use the same window for logs and errors
			setErrorFrame(logFrame);

			exceptionHandler = new UncaughtExceptionHandler() {
				public void uncaughtException (Thread thread, Throwable ex) {
					ex.printStackTrace();
					if (logFrame != null && prefs.showLogOnError) {
						logFrame.setVisible(true);
						logFrame.scrollToEnd();
					}
				}
			};
			Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

			ManaSymbols.loadImages();
			RatingRenderer.loadImages();
			FlagRenderer.loadImages();

			prefs = new ArcanePreferences(prefsFileName);

			// Reset some data when versions change.
			if (!prefs.version.equals(version)) {
				dialog.setMessage("Updating version...");
				new File("arcane.properties").delete();
				FileUtil.deleteDirectory(new File(dataDir + "rulesdb"));
			}

			transFileName = "data/" + prefs.uiLanguage.toLowerCase() + ".lang";
			trans = new ArcaneTranslation(transFileName);

			// Reset rule data if it changed.
			long rulesStamp = new File(dataDir + "rule-cards.txt").lastModified();
			long rulingsStamp = new File(dataDir + "rule-general.txt").lastModified();
			if (rulesStamp != prefs.rulesTimestamp || rulingsStamp != prefs.rulingsTimestamp)
				FileUtil.deleteDirectory(new File(dataDir + "rulesdb"));
			prefs.rulesTimestamp = rulesStamp;
			prefs.rulingsTimestamp = rulingsStamp;

			if (isCancelled()) return;

			if (loadRuleData) {
				// Load rule data.
				dialog.setMessage("Initializing rules datastore...");
				rulesDatastore = new RulesDataStore();
				rulesDatastore.populate(dialog, dataDir);
				if (isCancelled()) return;

				dialog.setMessage("Initializing rulings datastore...");
				rulingsDatastore = new RulingsDataStore();
				rulingsDatastore.populate(dialog, dataDir);
				if (isCancelled()) return;
			}

			// Load card data.
			loadCardData(dialog);

			if (isCancelled()) return;

			// Load plugins.
			loadPlugins();

			if (isCancelled()) return;
		}

		private void loadPlugins () throws IOException {
			File pluginsDir = new File("plugins");
			if (!pluginsDir.exists() || !pluginsDir.isDirectory()) return;

			dialog.setMessage("Initializing plugins...");

			FilenameFilter jarFilter = new FilenameFilter() {
				public boolean accept (File dir, String filename) {
					return filename.toLowerCase().endsWith(".jar");
				}
			};
			for (File pluginDir : pluginsDir.listFiles()) {
				if (!pluginDir.isDirectory()) continue;
				for (File pluginJar : pluginDir.listFiles(jarFilter)) {
					ZipInputStream input = new ZipInputStream(new FileInputStream(pluginJar));
					while (true) {
						ZipEntry entry = input.getNextEntry();
						if (entry == null) break;
						pluginClassLoader.addURL(pluginJar.toURI().toURL());
						if (!entry.isDirectory() && entry.getName().equalsIgnoreCase("plugin.txt")) {
						    	BufferedReader reader = null;
							try {
							    try{
							    	reader = new BufferedReader(new InputStreamReader(input));
								String pluginClassName = reader.readLine();
								Class<?> pluginClass = getPluginClass(pluginClassName);
								Plugin plugin = (Plugin)pluginClass.newInstance();
								dialog.setMessage("Initializing plugin: " + plugin.getName());
								plugin.install(dialog);
								plugins.add(plugin);
							    } finally {
								if(reader != null)
								    reader.close();
							    }
							} catch (Exception ex) {
								logError("Error loading plugin file: " + pluginJar.getAbsolutePath(), ex);
							}
							break;
						}
					}
				}
			}
			for (Plugin plugin : plugins)
				plugin.loadPreferences();
		}

		private void loadCardData (ProgressDialog dialog) throws IOException, SQLException {
			dialog.setMessage("Loading card data...");

			loadSets();

			cardDataStore = new CardDataStore();
			allCards = cardDataStore.populate(dialog, dataDir);

			if (isCancelled()) return;
			dialog.setValue(-1);

			for (Card card : allCards) {
				idToCard.put(card.id, card);
				List<Card> cards = nameToCards.get(card.name.toLowerCase());
				if (cards == null) {
					cards = new ArrayList<Card>();
					nameToCards.put(card.name.toLowerCase(), cards);
				}
				cards.add(card);
			}

			// Sort the cards by set and picture number.
			Comparator<Card> cardSetComparator = new Comparator<Card>() {
				public int compare (Card card1, Card card2) {
					int ordinal1 = setToOrdinal.get(card1.set);
					int ordinal2 = setToOrdinal.get(card2.set);
					int diff = ordinal1 - ordinal2;
					if (diff != 0) return diff;
					return card1.version.compareTo(card2.version);
				}
			};
			for (Card card : getCards())
				Collections.sort(getCards(card.name), cardSetComparator);

			loadUserData();

			loadManaProduced(dataDir + "titleToLandColors.csv");
			loadManaProduced(dataDir + "titleToCardColors.csv");

			loadFormats();

			loadLanguage(dialog);
		}

		private void loadSets () throws IOException, SQLException {
			int ordinal = 1000;
			BufferedReader reader = new BufferedReader(new UnicodeReader(new FileInputStream(dataDir + "sets.txt"), "UTF-8"));
			while (true) {
				String line = reader.readLine();
				if (line == null) break;
				line = line.trim();
				if (line.length() == 0) continue;
				if (line.charAt(0) == '#') continue;
				int spaceIndex = line.indexOf(' ');
				if (spaceIndex == 0) throw new ArcaneException("Error parsing \"data/sets.txt\". Invalid entry: " + line);

				String[] abbreviations = line.substring(0, spaceIndex).split(",");

				String mainSet = "";
				for (String set : abbreviations)
					if (mainSet.length() < set.length() &&  set.length()<=3) mainSet = set;
				mainSet = mainSet.toLowerCase().intern();

				String name = line.substring(spaceIndex + 1);

				// Everything before alpha starts at 1000.
				if(ordinal >= 1000 &&  "Alpha".equals(name)) ordinal = 0;

				setToOrdinal.put(mainSet, ordinal++);

				setToName.put(mainSet, name);

				for (int i = 0; i < abbreviations.length; i++)
					setToMainSet.put(abbreviations[i].toLowerCase(), mainSet);
				setToMainSet.put(name.toLowerCase(), mainSet);
			}
			reader.close();
		}

		private void loadLanguage (ProgressDialog dialog) {
			FilenameFilter cardsFilter = new FilenameFilter() {
				public boolean accept (File dir, String filename) {
					return filename.toLowerCase().startsWith("cards-") && filename.toLowerCase().endsWith(".csv")
						&& filename.length() > 10;
				}
			};
			languages.add("English");
			for (File cardsFile : new File("data").listFiles(cardsFilter)) {
				languages.add(cardsFile.getName().substring(6, 7).toUpperCase()
					+ cardsFile.getName().substring(7, cardsFile.getName().length() - 4));
			}

			FilenameFilter uiLanguagesFilter = new FilenameFilter() {
				public boolean accept (File dir, String filename) {
					return filename.toLowerCase().endsWith(".lang") && !filename.toLowerCase().startsWith("english");
				}
			};
			uiLanguages.add("English");
			for (File uiLangFile : new File("data").listFiles(uiLanguagesFilter)) {
				uiLanguages.add(uiLangFile.getName().substring(0, 1).toUpperCase()
					+ uiLangFile.getName().substring(1, uiLangFile.getName().length() - 5));
			}

			if (prefs.isEnglishLanguage()) return;

			dialog.setMessage("Loading language: " + prefs.language);

			File languageFile = new File(dataDir + "cards-" + prefs.language.toLowerCase() + ".csv");
			if (!languageFile.exists()) {
				logError("Missing language file: " + languageFile.getAbsolutePath());
				return;
			}
			try {
				CardDataStoreConnection conn = cardDataStore.getThreadConnection();
				CSVReader reader = new CSVReader(new UnicodeReader(new FileInputStream(languageFile), "UTF-8"), ",", "\"", true, true);
				while (true) {
					List<String> fields = reader.getFields();
					if (fields == null) break;
					if (fields.size() < 4) {
						//logError("Invalid row for language \"" + prefs.language + "\": " + fields);
						log("Invalid row for language \"" + prefs.language + "\": " + fields);
						continue;
					}
					String name = fields.get(0);
					String newName = fields.get(1);
					if (name.toLowerCase().equals("name") && newName.toLowerCase().equals("native name")) continue;
					if (newName.length() == 0) newName = name;
					String newType = fields.get(2);
					String newLegal = fields.get(3);
					conn.updateCardLanguage(name, newName, newType, newLegal);
					try{
						List<Card> cards = getCards(name);
						for (Card card : cards) {
							card.name = newName;
							card.type = newType;
							card.legal = newLegal;
						}
						List<Card> tmp = nameToCards.get(newName.toLowerCase());
						if(tmp != null && !tmp.get(0).englishName.equals(name))
							log("Duplicate localized name for card: " + name + "\t" + tmp.get(0).englishName);
						nameToCards.put(newName.toLowerCase(), cards);
					} catch (ArcaneException e){
						log("Localized data for missing card: " + name);
					}
				}
			} catch (SQLException ex) {
				logError("Error loading language: " + prefs.language, ex);
			} catch (IOException ex) {
				logError("Error loading language: " + prefs.language, ex);
			}
		}

		private void loadUserData () throws IOException {
			if (!new File("userData.csv").exists()) return;
			CSVReader csvReader = new CSVReader(new FileReader("userData.csv"), ",", "\"", true, false);
			csvReader.getFields();
			while (true) {
				List<String> fields = csvReader.getFields();
				if (fields == null) break;
				if (fields.size() < 4) continue;

				String cardName = fields.get(0);
				String set = fields.get(1);

				int rating = -1;
				String flags = null;
				String version = "";
				int qty = 0;
				if (set.length() == 0) {
					if (fields.get(4).length() > 0) rating = Integer.parseInt(fields.get(4));
					flags = fields.get(5);
				} else {
					version = "";
					if (fields.get(2).length() > 0) version = fields.get(2);
					qty = Integer.parseInt(fields.get(3));
				}

				for (Card card : getCards(cardName)) {
					if (rating != -1) card.rating = rating;
					if (flags != null) card.flags = flags;
					if (card.set.equals(set) && card.version.equals(version)) card.ownedQty = qty;
				}
			}
			csvReader.close();
		}

		private void loadManaProduced (String fileName) throws IOException, SQLException {
			CardDataStoreConnection conn = cardDataStore.newConnection();
			CSVReader reader = new CSVReader(new UnicodeReader(new FileInputStream(fileName), "UTF-8"), ",", "\"", true, true);
			while (true) {
				List<String> fields = reader.getFields();
				if (fields == null) break;
				if (fields.size() < 2) continue;
				String cardName = fields.get(0);
				String colors = fields.get(1);
				List<Card> cards = getCards(cardName);
				for (Card card : cards) {
					StringBuffer buffer = new StringBuffer(16);
					for (int i = 0, n = colors.length(); i < n; i++) {
						if (colors.charAt(i) == 'A') continue;
						buffer.append('{');
						buffer.append(colors.charAt(i));
						buffer.append('}');
					}
					card.manaProduced = buffer.toString();
					conn.updateManaProduced(card.name, card.manaProduced);
				}
			}
			reader.close();
			conn.close();
		}

		private void loadFormats () throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(dataDir + "formats.txt"));
			try {
				while (true) {
					Format currentFormat = Format.getByText(reader.readLine());
					if (currentFormat == null) return;
					String line;
					String state = null;
					while (true) {
						line = reader.readLine();
						if (line == null) return;
						line = line.trim();
						if (line.length() == 0) break;
						if (line.equals("BANNED") || line.equals("RESTRICTED") || line.equals("SETS")) {
							state = line;
							continue;
						}
						if (state == null) throw new ArcaneException("Error parsing \"data/formats.txt\". Invalid section: " + line);
						if (state.equals("BANNED")) {
							String cardName = line.toLowerCase();
							getCards(line);
							Set<Format> bannedFormats = cardNameToBannedFormats.get(cardName);
							if (bannedFormats == null) {
								bannedFormats = new HashSet<Format>();
								cardNameToBannedFormats.put(cardName, bannedFormats);
							}
							bannedFormats.add(currentFormat);
						} else if (state.equals("RESTRICTED")) {
							String cardName = line.toLowerCase();
							getCards(cardName);
							Set<Format> restrictedFormats = cardNameToRestrictedFormats.get(cardName);
							if (restrictedFormats == null) {
								restrictedFormats = new HashSet<Format>();
								cardNameToRestrictedFormats.put(cardName, restrictedFormats);
							}
							restrictedFormats.add(currentFormat);
						} else if (state.equals("SETS")) {
							String set = setToMainSet.get(line.toLowerCase());
							if (!setToOrdinal.containsKey(set))
								throw new ArcaneException("Error parsing \"data/formats.txt\". Invalid set: " + line);
							Set<String> sets = getFormatSets(currentFormat);
							if (sets == null) {
								sets = new HashSet<String>();
								formatToSets.put(currentFormat, sets);
							}
							sets.add(set);
						}
					}
				}
			} finally {
				reader.close();
			}
		}
	}

	static private class PluginClassLoader extends URLClassLoader {
		public PluginClassLoader () {
			super(new URL[0], Arcane.class.getClassLoader());
		}

		public void addURL (URL url) {
			super.addURL(url);
		}
	}
}
