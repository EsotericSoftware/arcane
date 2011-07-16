
package arcane.deckbuilder.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.ArcanePreferences.CardImageType;
import arcane.Card;
import arcane.CardDataStore.CardDataStoreConnection;
import arcane.Decklist;
import arcane.DecklistCard;
import arcane.DecklistFile;
import arcane.Format;
import arcane.Plugin;
import arcane.deckbuilder.DeckBuilderPlugin;
import arcane.ui.table.CardTable;
import arcane.ui.table.FlagRenderer;
import arcane.ui.table.FlagRenderer.Flag;
import arcane.ui.table.RatingRenderer;
import arcane.ui.table.SortedTable;
import arcane.ui.util.ButtonMenuItem;
import arcane.ui.util.DocumentModifiedListener;
import arcane.ui.util.Menu;
import arcane.ui.util.MenuItem;
import arcane.ui.util.MessageFrame;
import arcane.ui.util.SplitPane;
import arcane.ui.util.UI;
import arcane.util.CSVReader;
import arcane.util.CSVWriter;
import arcane.util.FileUtil;
import arcane.util.Loader;
import arcane.util.UnicodeReader;
import arcane.util.Util;
import com.google.common.collect.MapMaker;

public class DeckBuilder extends DeckBuilderUI {
	static private List<DeckBuilder> instances = new ArrayList<DeckBuilder>();

	private Decklist currentDecklist;
	private boolean searchDisabled;
	private long lastSearchRefresh;
	private long lastQuickSearchRefresh;
	private LinkedList<DeckAction> history = new LinkedList<DeckAction>();
	private int historyIndex = -1;
	private List<Card> quickSearchCards;
	private int quickSearchIndex;
	private String lastQuickSearch;
	private LinkedList<Decklist> lastOpenedDecklists = new LinkedList<Decklist>();
	private Map<JMenu, CardTable> addToMenus = new HashMap<JMenu, CardTable>();
	private JFileChooser saveFileChooser;
	private boolean isDirty;
	private DeckStats deckStats;
	private Image deckInfoImage;
	private Image illegalT1, illegalT15, illegalT1X, illegalT2;
	private Image cautionT1, cautionT15, cautionT1X, cautionT2;
	private boolean ignoreSelections;
	private MessageFrame helpFrame;
	private ConcurrentMap<Card, BufferedImage> imageCache;

	public DeckBuilder () {
		Loader loader = new Loader("Deck Builder - Arcane v" + Arcane.version) {
			public void load () throws Exception {
				dialog.setMessage("Loading...");

				deckInfoImage = UI.getImageIcon("/deckInfo.png").getImage();
				illegalT1 = UI.getImageIcon("/legality/illegal t1.png").getImage();
				illegalT15 = UI.getImageIcon("/legality/illegal t15.png").getImage();
				illegalT1X = UI.getImageIcon("/legality/illegal t1x.png").getImage();
				illegalT2 = UI.getImageIcon("/legality/illegal t2.png").getImage();
				cautionT1 = UI.getImageIcon("/legality/caution t1.png").getImage();
				cautionT15 = UI.getImageIcon("/legality/caution t15.png").getImage();
				cautionT1X = UI.getImageIcon("/legality/caution t1x.png").getImage();
				cautionT2 = UI.getImageIcon("/legality/caution t2.png").getImage();

				helpFrame = new MessageFrame("Help - Deck Builder - Arcane v" + Arcane.version);
				helpFrame.setHTML(true);
				helpFrame.addButton("Close");
				helpFrame.setText(DeckBuilder.class.getResource("/help-deckbuilder.html"));

				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				initializeMenus();
				loadPlugins();
				initializeComponents();
				initializePopupMenus();
				initializeEvents();
				initializeImageCache();
				updateCardImageType();
				updateButtons();

				saveFileChooser = new JFileChooser("decks");
				saveFileChooser.setAcceptAllFileFilterUsed(false);
				Pattern pattern = Pattern.compile("(.+)\\((.+)\\)\\.st$");
				for (File file : new File("templates").listFiles(FileUtil.filenameEndsWith(".st"))) {
					Matcher matcher = pattern.matcher(file.getName());
					if (!matcher.matches()) continue;
					String name = matcher.group(1).trim();
					String extension = matcher.group(2);
					saveFileChooser.addChoosableFileFilter(FileUtil.chooserFileEndsWith(name + " (*." + extension + ")",
						'.' + extension));
				}

				for (String set : arcane.getSets())
					setsListModel.insertElementAt(new SetEntry(set), 0);
				setsList.getSelectionModel().setSelectionInterval(0, arcane.getSets().size() - 1);

				loadPreferences();
			}
		};
		loader.start("DeckBuilderLoader");
		if (loader.failed()) {
			dispose();
			throw new ArcaneException("Arcane Deck Builder initialization aborted.");
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run () {
				if (lastOpenedDecklists.isEmpty())
					loadDecklist(null);
				else
					loadDecklist(lastOpenedDecklists.getFirst());

				updateDeckCount();
				resetSearch();
				instances.add(DeckBuilder.this);
			}
		});
	}

	protected void initializeImageCache() {
		imageCache = new MapMaker().softValues().makeMap();
	}

	private void setCurrentDecklist (Decklist decklist) {
		String name;
		if (decklist == null || !decklist.exists())
			name = "Untitled";
		else {
			if (!decklist.isOpenable()) return;
			name = decklist.getName();
		}
		if (isDirty) name += '*';
		setTitle(name + " - Deck Builder - Arcane v" + Arcane.version);
		currentDecklist = decklist;
	}

	public Decklist getCurrentDecklist () {
		return currentDecklist;
	}

	private void updateMRU () {
		// Remove existing MRU entries.
		while (fileMenu.getItemCount() > 8)
			fileMenu.remove(6);
		for (JMenu addToMenu : addToMenus.keySet())
			while (addToMenu.getItemCount() > 1)
				addToMenu.remove(0);

		// Remove entries with missing files.
		for (Iterator<Decklist> iter = lastOpenedDecklists.iterator(); iter.hasNext();)
			if (!iter.next().exists()) iter.remove();

		if (lastOpenedDecklists.isEmpty()) return;

		// Trim to 9 entries.
		while (lastOpenedDecklists.size() > 9)
			lastOpenedDecklists.removeLast();

		// File menu items.
		for (int i = lastOpenedDecklists.size() - 1; i >= 0; i--) {
			final Decklist decklist = lastOpenedDecklists.get(i);
			JMenuItem menuItem = new JMenuItem((i + 1) + " " + decklist.getName());
			menuItem.setMnemonic(String.valueOf(i + 1).charAt(0));
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					if (isDirty) {
						int result = JOptionPane.showConfirmDialog(DeckBuilder.this, "Unsaved changes will be lost.", "Confirm Open",
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
						if (result != JOptionPane.OK_OPTION) return;
					}
					loadDecklist(decklist);
				}
			});
			fileMenu.add(menuItem, 6);
		}
		fileMenu.add(new JPopupMenu.Separator(), 6);

		// "Add to" menu items.
		for (final JMenu addToMenu : addToMenus.keySet()) {
			for (int i = lastOpenedDecklists.size() - 1; i >= 0; i--) {
				final Decklist decklist = lastOpenedDecklists.get(i);
				if (currentDecklist != null && currentDecklist.equals(decklist)) continue;
				if (!(decklist instanceof DecklistFile)) continue;
				JMenuItem menuItem = new MenuItem(decklist.getName());
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed (ActionEvent evt) {
						((DecklistFile)decklist).addCards(addToMenus.get(addToMenu).getSelectedCards());
						lastOpenedDecklists.remove(decklist);
						lastOpenedDecklists.addFirst(decklist);
						updateMRU();
					}
				});
				addToMenu.insert(menuItem, 0);
			}
			if (addToMenu.getItemCount() > 1) addToMenu.add(new JPopupMenu.Separator(), addToMenu.getItemCount() - 1);
		}
	}

	public void addDecklistCard (Card card, boolean sideboard) {
		CardTable table = (sideboard ? sideTable : deckTable);
		table.model.unsortedCards.add(card);
	}

	public void saveDecklist (Decklist decklist, boolean updateMRU) {
		if (decklist == null) throw new IllegalArgumentException("decklist cannot be null.");

		try {
			decklist.save(deckTable.model.viewCards, deckTable.model.cardToQty,  sideTable.model.viewCards, sideTable.model.cardToQty);
		} catch (IOException ex) {
			throw new ArcaneException("Error saving decklist: " + decklist, ex);
		}

		if (updateMRU) {
			if (decklist.isOpenable()) {
				lastOpenedDecklists.remove(decklist);
				lastOpenedDecklists.addFirst(decklist);
				isDirty = false;
			}
			updateMRU();
			setCurrentDecklist(decklist);
		}
	}

	public void loadDecklist (Decklist decklist) {
		deckTable.model.unsortedCards = new ArrayList<Card>();
		deckTable.model.viewCards = new ArrayList<Card>();
		sideTable.model.unsortedCards = new ArrayList<Card>();
		sideTable.model.viewCards = new ArrayList<Card>();
		try {
			if (decklist != null && decklist.isOpenable()) {
				decklist.open();
				List<Card>[] cards = DecklistCard.getCards(decklist.getDecklistCards());
				for (Card card : cards[0])
					addDecklistCard(card, false);
				for (Card card : cards[1])
					addDecklistCard(card, true);
			}
		} catch (Exception ex) {
			arcane.logError(new ArcaneException("Error opening decklist: " + decklist, ex));
		} finally {
			history.clear();
			historyIndex = -1;
			deckTable.model.fireTableDataChanged();
			sideTable.model.fireTableDataChanged();
			deckTable.sort();
			sideTable.sort();
			updateDeckCount();
			if (decklist != null && decklist.isOpenable()) {
				lastOpenedDecklists.remove(decklist);
				lastOpenedDecklists.addFirst(decklist);
			}
			isDirty = false;
			setCurrentDecklist(decklist);
			updateMRU();
			updateButtons();
		}
	}

	private void loadPlugins () {
		List<Plugin> plugins = arcane.getPluginsList();
		for (Plugin plugin : plugins){
			try{
				DeckBuilderPlugin deckPlugin = (DeckBuilderPlugin)plugin;
				deckPlugin.install(DeckBuilder.this);
			} catch (ClassCastException ex){
				//nothing
			} catch (Exception ex){
				arcane.logError("Error loading plugin: " + plugin.getName(), ex);
				//throw new ArcaneException("Error loading plugin: " + plugin.getName(), ex);
			}
		}

	}

	private void loadPreferences () {
		prefs.loadFrameState(this, "deckbuilder", 800, 705);

		SplitPane.setScrollPaneInfo(getContentPane(), prefs.get("deckbuilder.splitpanes", "217,239,0.5231481,0.6816817,"));

		String fileNames = prefs.get("deckbuilder.last.opened", "arcane.DecklistFile*decks/Fun With Fungus.csv|CSV (csv)");
		// String fileNames = prefs.get("deckbuilder.last.opened", "");
		CSVReader reader = new CSVReader(new StringReader(fileNames));
		try {
			List<String> fields = reader.getFields();
			if (fields != null) {
				for (String value : fields) {
					String className = value.split("\\*")[0];
					Object[] args = value.split("\\*")[1].split("\\|");
					try {
						Decklist decklist = (Decklist)arcane.getPluginClass(className).getConstructors()[0].newInstance(args);
						if (!decklist.exists()) continue;
						lastOpenedDecklists.addLast(decklist);
					} catch (Exception ex) {
						arcane.logError("Error loading class for MRU entry: " + className, ex);
					}
				}
			}
		} catch (IOException ex) {
			arcane.logError("Error loading MRU list.", ex);
		}

		Map<String, String> cardsTableValues = new HashMap<String, String>();
		cardsTableValues.put("columns", prefs.get("deckbuilder.cards.table.columns", "name,castingCost"));
		cardsTableValues.put("widths", prefs.get("deckbuilder.cards.table.widths", "106,61"));
		cardsTableValues.put("auto.resize", prefs.get("deckbuilder.cards.table.auto.resize", "2"));
		cardsTableValues.put("sort", prefs.get("deckbuilder.cards.table.sort", "0,ascending"));
		cardsTable.setPreferences(cardsTableValues);

		Map<String, String> deckTableValues = new HashMap<String, String>();
		deckTableValues.put("columns", prefs.get("deckbuilder.deck.table.columns", "qty,name,castingCost,type"));
		deckTableValues.put("widths", prefs.get("deckbuilder.deck.table.widths", "30,133,61,87"));
		deckTableValues.put("auto.resize", prefs.get("deckbuilder.deck.table.auto.resize", "1"));
		deckTableValues.put("sort", prefs.get("deckbuilder.deck.table.sort", ""));
		deckTable.setPreferences(deckTableValues);

		reader = new CSVReader(new StringReader(prefs.get("deckbuilder.search.history", "")));
		try {
			List<String> fields = reader.getFields();
			if (fields != null) {
				for (String text : fields)
					searchComboModel.addElement(text);
			}
		} catch (IOException ex) {
			arcane.logError("Error loading search history.", ex);
		}

		uniqueOnlyButton.setSelected(prefs.getBoolean("deckbuilder.only.unique.cards", true));
		ownedOnlyButton.setSelected(prefs.getBoolean("deckbuilder.only.owned.cards", false));

		linkTablesButton.setSelected(prefs.getBoolean("deckbuilder.link.tables", false));

		presetCombo.setSelectedItem(Format.valueOf(prefs.get("deckbuilder.set.preset", "all")));

		reader = new CSVReader(new StringReader(prefs.get("deckbuilder.set.selected", "")));
		try {
			List<String> fields = reader.getFields();
			if (fields != null) {
				setsList.clearSelection();
				for (String set : fields) {
					int index = setsListModel.getIndexOf(new SetEntry(set));
					setsList.addSelectionInterval(index, index);
				}
			}
		} catch (IOException ex) {
			arcane.logError("Error loading selected sets.", ex);
		}

		alwaysMatchEnglishMenuItem.setSelected(prefs.getBoolean("deckbuilder.card.always.match.english", false));

		if (prefs.isEnglishLanguage()) {
			alwaysMatchEnglishMenuItem.setEnabled(false);
			alwaysMatchEnglishMenuItem.setSelected(false);
		}
	}

	protected void savePreferences () {
		prefs.saveFrameState(this, "deckbuilder");

		prefs.set("deckbuilder.splitpanes", SplitPane.getScrollPaneInfo(getContentPane()));

		if (!lastOpenedDecklists.isEmpty()) {
			StringWriter stringWriter = new StringWriter();
			CSVWriter writer = new CSVWriter(stringWriter);
			for (Decklist decklist : lastOpenedDecklists)
				writer.writeField(decklist.getClass().getName() + "*" + decklist.getData());
			prefs.set("deckbuilder.last.opened", stringWriter.toString());
		}

		Map<String, String> cardsTableValues = cardsTable.getPreferences();
		prefs.set("deckbuilder.cards.table.columns", cardsTableValues.get("columns"));
		prefs.set("deckbuilder.cards.table.widths", cardsTableValues.get("widths"));
		prefs.set("deckbuilder.cards.table.auto.resize", cardsTableValues.get("auto.resize"));
		prefs.set("deckbuilder.cards.table.sort", cardsTableValues.get("sort"));

		Map<String, String> deckTableValues = deckTable.getPreferences();
		prefs.set("deckbuilder.deck.table.columns", deckTableValues.get("columns"));
		prefs.set("deckbuilder.deck.table.widths", deckTableValues.get("widths"));
		prefs.set("deckbuilder.deck.table.auto.resize", deckTableValues.get("auto.resize"));
		prefs.set("deckbuilder.deck.table.sort", deckTableValues.get("sort"));

		StringWriter stringWriter = new StringWriter();
		CSVWriter writer = new CSVWriter(stringWriter);
		for (int i = 0, n = searchComboModel.getSize(); i < n; i++) {
			String text = ((String)searchComboModel.getElementAt(i)).trim();
			if (text.length() == 0) continue;
			writer.writeField(text);
		}
		prefs.set("deckbuilder.search.history", stringWriter.getBuffer().toString());

		prefs.set("deckbuilder.only.unique.cards", String.valueOf(uniqueOnlyButton.isSelected()));

		prefs.set("deckbuilder.only.owned.cards", String.valueOf(ownedOnlyButton.isSelected()));

		prefs.set("deckbuilder.link.tables", String.valueOf(linkTablesButton.isSelected()));

		prefs.set("deckbuilder.set.preset", ((Format)presetCombo.getSelectedItem()).name());

		stringWriter = new StringWriter();
		if (presetCombo.getSelectedItem() == Format.custom) {
			writer = new CSVWriter(stringWriter);
			for (Object entry : setsList.getSelectedValues())
				writer.writeField(((SetEntry)entry).set);
		}
		prefs.set("deckbuilder.set.selected", stringWriter.getBuffer().toString());

		prefs.set("deckbuilder.card.always.match.english", String.valueOf(alwaysMatchEnglishMenuItem.isSelected()));
	}

	public void updateCardTable () {
		if (searchDisabled) return;
		lastSearchRefresh = System.currentTimeMillis();
		final long currentRefreshNumber = lastSearchRefresh;
		final Card selectedCard = cardsTable.getSelectedRowCount() == 1 ? cardsTable.model.viewCards.get(cardsTable
			.getSelectedRow()) : null;
		Util.threadPool.submit(new Runnable() {
			public void run () {
				if (currentRefreshNumber != lastSearchRefresh) return;

				final Object selectedItem = searchCombo.getEditor().getItem();
				searchCombo.hidePopup();
				searchCombo.setSelectedItem(selectedItem);
				final String text = selectedItem != null ? ((String)selectedItem).trim() : "";
				UI.setTitle(cardsGroup, "Cards (...)");
				cardsTable.removeAll();

				// Build where clause.
				StringBuffer where = new StringBuffer(64);
				if (exactButton.isSelected()) {
					where.append("(1=1");
					if (!whiteButton.isSelected()) where.append(" AND (castingCost NOT LIKE '%w%' AND color!='white')");
					if (!blueButton.isSelected()) where.append(" AND (castingCost NOT LIKE '%u%' AND color!='blue')");
					if (!blackButton.isSelected()) where.append(" AND (castingCost NOT LIKE '%b%' AND color!='black')");
					if (!redButton.isSelected()) where.append(" AND (castingCost NOT LIKE '%r%' AND color!='red')");
					if (!greenButton.isSelected()) where.append(" AND (castingCost NOT LIKE '%g%' AND color!='green')");
					if (!colorlessButton.isSelected()) where.append(" AND (color != 'artifact' AND color != 'land')");
				} else {
					where.append("(1=2");
					if (whiteButton.isSelected()) where.append(" OR (castingCost LIKE '%w%' OR color='white')");
					if (blueButton.isSelected()) where.append(" OR (castingCost LIKE '%u%' OR color='blue')");
					if (blackButton.isSelected()) where.append(" OR (castingCost LIKE '%b%' OR color='black')");
					if (redButton.isSelected()) where.append(" OR (castingCost LIKE '%r%' OR color='red')");
					if (greenButton.isSelected()) where.append(" OR (castingCost LIKE '%g%' OR color='green')");
					if (colorlessButton.isSelected()) where.append(" OR (color = 'artifact' OR color = 'land')");
				}
				where.append(") AND (1=2");
				if (landButton.isSelected()) where.append(" OR (color = 'land')");
				if (artifactButton.isSelected()) where.append(" OR (color = 'artifact' OR englishType LIKE '%artifact%')");
				if (creatureButton.isSelected()) where.append(" OR (englishType LIKE '%creature%')");
				if (sorceryButton.isSelected()) where.append(" OR (englishType LIKE '%sorcery%')");
				if (instantButton.isSelected()) where.append(" OR (englishType LIKE '%instant%')");
				if (enchantButton.isSelected()) where.append(" OR (englishType LIKE '%enchantment%' AND type NOT LIKE '%aura%')");
				if (auraButton.isSelected()) where.append(" OR (englishType LIKE '%aura%')");
				if (planeswalkerButton.isSelected()) where.append(" OR (englishType LIKE '%planeswalker%')");
				where.append(") AND (1=2");
				if (commonButton.isSelected()) where.append(" OR rarity='c'");
				if (uncommonButton.isSelected()) where.append(" OR rarity='u'");
				if (rareButton.isSelected()) where.append(" OR rarity='r'");
				if (mythicrareButton.isSelected()) where.append(" OR rarity='m'");
				where.append(")");

				if (multiColorButton.isSelected()) where.append(" AND color='gold'");

				where.append(" AND (1");
				Set<String> requiredSets = new HashSet<String>();
				List<Object[]> ratings = new ArrayList<Object[]>();
				List<Character> flags = new ArrayList<Character>();
				try {
					CSVReader reader = new CSVReader(new StringReader(text), " ", "\"", true, false);
					List<String> fields = reader.getFields();
					if (fields != null) {
						String fieldOperator = " AND";
						for (String field : fields) {
							String[] nameValue = field.split("[=<>]");
							if (nameValue.length == 2) {
								nameValue[0] = nameValue[0].toLowerCase();
								nameValue[1] = nameValue[1].toLowerCase();
								boolean quote = false;
								String operator = field.substring(nameValue[0].length(), nameValue[0].length() + 1);
								String columnName = null;
								if (nameValue[0].equals("power") || nameValue[0].equals("p"))
									columnName = "power";
								else if (nameValue[0].equals("tough") || nameValue[0].equals("toughness") || nameValue[0].equals("t"))
									columnName = "toughness";
								else if (nameValue[0].equals("r") || nameValue[0].equals("rating") || nameValue[0].equals("ratings")) {
									if (nameValue[1].length() != 1) continue;
									try {
										ratings.add(new Object[] {operator.charAt(0), Integer.valueOf(nameValue[1])});
									} catch (NumberFormatException ignored) {
									}
									continue;
								} else if (nameValue[0].equals("f") || nameValue[0].equals("flag") || nameValue[0].equals("flags")) {
									for (int i = 0, n = nameValue[1].length(); i < n; i++)
										flags.add(nameValue[1].charAt(i));
									continue;
								} else if (nameValue[0].equals("ccost") || nameValue[0].equals("cc")
									|| nameValue[0].equals("convertedcost") || nameValue[0].equals("convertedcastingcost")) {
									columnName = "convertedCastingCost";
								} else if (nameValue[0].equals("cost") || nameValue[0].equals("c") || nameValue[0].equals("castingcost")) {
									columnName = "castingCost";
									StringBuffer value = new StringBuffer(nameValue[1]);
									if (value.indexOf("{") == -1) {
										for (int i = value.length() - 1; i > 0; i--) {
											value.insert(i, '}');
											value.insert(i + 1, '{');
										}
										value.insert(0, '{');
										value.append('}');
									}
									nameValue[1] = value.toString();
									if (nameValue[1].contains("*")) {
										nameValue[1] = nameValue[1].replace("*", "%");
										operator = "LIKE";
									}
									quote = true;
								} else if (nameValue[0].equals("pt")) {
									nameValue[1] = nameValue[1].replace('\\', '/');
									String[] split = nameValue[1].split("\\/");
									if (split.length == 2) {
										where.append(fieldOperator);
										where.append(" (power = ");
										where.append(sql(split[0]));
										where.append(" AND toughness = ");
										where.append(sql(split[1]));
										where.append(')');
										fieldOperator = " AND";
										continue;
									}
								} else if (nameValue[0].equals("s") || nameValue[0].equals("set")) {
									requiredSets.add(arcane.getMainSet(nameValue[1]));
									continue;
								} else if (nameValue[0].equals("m") || nameValue[0].equals("mana") || nameValue[0].equals("manaproduced")) {
									String colors = nameValue[1];
									if (colors.length() == 0) continue;
									if (colors.equals("colorless")) colors = "c";
									where.append(fieldOperator);
									where.append(" (1 ");
									for (int i = 0, n = colors.length(); i < n; i++) {
										where.append(" AND manaProduced LIKE '%");
										char color = colors.charAt(i);
										if (color == '\'') color = ' ';
										where.append(color);
										where.append("%'");
									}
									where.append(')');
									continue;
								}
								if (columnName != null) {
									where.append(fieldOperator);
									where.append(" (");
									where.append(columnName);
									where.append(" ");
									where.append(operator);
									where.append(' ');
									if (quote) where.append('\'');
									where.append(sql(nameValue[1]));
									if (quote) where.append('\'');
									where.append(')');
									fieldOperator = " AND";
									continue;
								}
							}

							String textWhere = "";
							if (field.length() > 0) {
								if (field.equals("OR")) {
									fieldOperator = " OR";
									continue;
								}

								boolean negate = field.charAt(0) == '-';
								if (negate) field = field.substring(1);

								// Replace * with % and surround with % only if not starting or ending with %.
								String searchValue = field.replace('*', '%');
								if (searchValue.length() <= 2
									|| (searchValue.charAt(0) != '%' && searchValue.charAt(searchValue.length() - 1) != '%')) {
									searchValue = "%" + searchValue + "%";
								}
								searchValue = sql(searchValue.toLowerCase());

								boolean searchEnglishToo = alwaysMatchEnglishMenuItem.isSelected() && !prefs.isEnglishLanguage();
								if (negate) {
									if (titleButton.isSelected()) {
										if (searchEnglishToo) {
											textWhere += " AND (name NOT LIKE '" + searchValue + "' OR englishName NOT LIKE '" + searchValue
												+ "')";
										} else
											textWhere += " AND (name NOT LIKE '" + searchValue + "')";
									}
									if (typeButton.isSelected()) {
										if (searchEnglishToo) {
											textWhere += " AND (type NOT LIKE '" + searchValue + "' OR englishType NOT LIKE '" + searchValue
												+ "')";
										} else
											textWhere += " AND (type NOT LIKE '" + searchValue + "')";
									}
									if (textButton.isSelected()) {
										if (searchEnglishToo) {
											textWhere += " AND (legal NOT LIKE '" + searchValue + "' OR englishLegal NOT LIKE '"
												+ searchValue + "')";
										} else
											textWhere += " AND (legal NOT LIKE '" + searchValue + "')";
									}
									if (searchValue.indexOf("{cardname}") != -1) {
										String[] subText = searchValue.split("\\{cardname\\}");
										if (searchEnglishToo) {
											textWhere += " AND (legal NOT LIKE CONCAT('" + subText[0] + "', name ,'" + subText[1] + "') OR englishLegal NOT LIKE CONCAT('" + subText[0] + "', name ,'" + subText[1] + "'))";
										} else
											textWhere += " AND (legal NOT LIKE CONCAT('" + subText[0] + "', name ,'" + subText[1] + "'))";
									}
									if (textWhere.length() > 0) {
										where.append(fieldOperator);
										where.append(" (1=1");
										where.append(textWhere);
										where.append(')');
									}
								} else {
									if (titleButton.isSelected()) {
										if (searchEnglishToo) {
											textWhere += " OR (name LIKE '" + searchValue + "' OR englishName LIKE '" + searchValue + "')";
										} else
											textWhere += " OR (name LIKE '" + searchValue + "')";
									}
									if (typeButton.isSelected()) {
										if (searchEnglishToo) {
											textWhere += " OR (type LIKE '" + searchValue + "' OR englishType LIKE '" + searchValue + "')";
										} else
											textWhere += " OR (type LIKE '" + searchValue + "')";
									}
									if (textButton.isSelected()) {
										if (searchEnglishToo) {
											textWhere += " OR (legal LIKE '" + searchValue + "' OR englishLegal LIKE '" + searchValue + "')";
										} else
											textWhere += " OR (legal LIKE '" + searchValue + "')";
									}
									if (searchValue.indexOf("{cardname}") != -1) {
										String[] subText = searchValue.split("\\{cardname\\}");
										if (searchEnglishToo) {
											textWhere += " OR (legal LIKE CONCAT('" + subText[0] + "', name ,'" + subText[1] + "') OR englishLegal LIKE CONCAT('" + subText[0] + "', name ,'" + subText[1] + "'))";
										} else
											textWhere += " OR (legal LIKE CONCAT('" + subText[0] + "', name ,'" + subText[1] + "'))";
									}
									if (textWhere.length() > 0) {
										where.append(fieldOperator);
										where.append(" (1=2");
										where.append(textWhere);
										where.append(')');
									}
								}
							}

							fieldOperator = " AND";
						}
					}
					reader.close();
				} catch (IOException ex) {
					throw new ArcaneException("Error parsing search text.", ex);
				}

				where.append(") AND set IN (");
				if (requiredSets.isEmpty()) {
					for (Object entry : setsList.getSelectedValues())
						requiredSets.add(((SetEntry)entry).set);
				}
				for (String set : requiredSets) {
					where.append('\'');
					where.append(sql(set));
					where.append("', ");
				}
				where.append("1)");

				try {
					// Execute query.
					boolean uniqueOnly = uniqueOnlyButton.isSelected();
					boolean ownedOnly = ownedOnlyButton.isSelected();
					String sql = "SELECT name, set, version FROM cards WHERE " + where;
					// System.out.println(sql);
					CardDataStoreConnection conn = arcane.getCardDataStoreConnection();
					conn.conn.setReadOnly(true);
					PreparedStatement statement = conn.prepareStatement(sql);
					if (currentRefreshNumber != lastSearchRefresh) return;
					// long start = System.currentTimeMillis();
					ResultSet resultSet = statement.executeQuery();
					// long end = System.currentTimeMillis();
					// System.out.println((end - start) / 1000F);
					List<Card> visibleCards = new ArrayList<Card>(20000);
					Format currentFormat = (Format)presetCombo.getSelectedItem();
					outerLoop: while (resultSet.next()) {
						if (currentRefreshNumber != lastSearchRefresh) return;
						Card card = arcane.getCard(resultSet.getString("name"), resultSet.getString("set"), resultSet
							.getString("version"));

						if (uniqueOnly) {
							// Skip rows that are not the first picture number and are not the oldest set that is valid for the query.
							if (!"".equals(card.version) && !"a".equals(card.version) && !"1".equals(card.version) && !"Right".equals(card.version)) continue;
							for (String oldestSet : arcane.getSets(card.name)) {
								if (!requiredSets.contains(oldestSet)) continue;
								if (card.set != oldestSet) continue outerLoop;
								break;
							}
						}

						if (ownedOnly) {
							if (uniqueOnly) {
								if (arcane.getTotalOwnedQty(card.name) == 0) continue;
							} else {
								if (card.ownedQty == 0) continue;
							}
						}

						if (arcane.isBanned(card.englishName, currentFormat)) continue;

						for (Object[] values : ratings) {
							int rating = (Integer)values[1];
							switch ((Character)values[0]) {
							case '=':
								if (card.rating != rating) continue outerLoop;
								break;
							case '<':
								if (card.rating >= rating) continue outerLoop;
								break;
							case '>':
								if (card.rating <= rating) continue outerLoop;
								break;
							}
						}

						for (char flag : flags)
							if (card.flags.indexOf(flag) == -1) continue outerLoop;

						visibleCards.add(card);
					}
					resultSet.close();
					statement.close();
					cardsTable.model.unsortedCards = visibleCards;
					lastQuickSearch = null;
					quickSearchIndex = -1;
				} catch (SQLException ex) {
					arcane.logError(new ArcaneException("Error performing card search.", ex));
				}

				cardsTable.model.isUniqueOnly = uniqueOnlyButton.isSelected();

				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						if (currentRefreshNumber != lastSearchRefresh) return;
						UI.setTitle(cardsGroup, "Cards ("
							+ NumberFormat.getIntegerInstance().format(cardsTable.model.unsortedCards.size()) + ")");
						cardsTable.model.fireTableDataChanged();

						if (selectedCard != null)
							cardsTable.setSelectedCard(selectedCard);
						else
							cardsTable.scrollRectToVisible(new Rectangle());

						if (text.length() > 0) {
							searchComboModel.removeElement(selectedItem);
							searchComboModel.insertElementAt(selectedItem, 0);
							searchComboModel.setSelectedItem(selectedItem);
							if (searchComboModel.getSize() > 50) searchComboModel.removeElementAt(50);
						}

						quickSearch(false);
					}
				});
			}
		});
	}

	public List<Card> getDeckCards () {
		return deckTable.model.viewCards;
	};

	public Map<Card, Integer> getDeckCardToQty () {
		return deckTable.model.cardToQty;
	};

	public List<Card> getSideCards () {
		return sideTable.model.viewCards;
	};

	public Map<Card, Integer> getSideCardToQty () {
		return sideTable.model.cardToQty;
	};

	public void addPluginMenu (JMenu pluginMenu) {
		if (pluginsMenu == null) {
			pluginsMenu = new JMenu(trans.get("menu.Plugins", "Plugins"));
			pluginsMenu.setMnemonic(KeyEvent.VK_P);
			menuBar.add(pluginsMenu, 4);
		}
		pluginsMenu.add(pluginMenu);
	}

	private void selectFirstVisibleRow (CardTable table) {
		Point location = table.getVisibleRect().getLocation();
		location.y += 10;
		int firstVisibleRow = table.rowAtPoint(location);
		if (firstVisibleRow != -1) table.getSelectionModel().setSelectionInterval(firstVisibleRow, firstVisibleRow);
	}

	private String sql (String value) {
		return value.replace("'", "''");
	}

	private boolean addCardToTable (Card card, CardTable table) {
		if (card == null) return false;

		// Get qty from both tables.
		int existingQty = deckTable.getQty(card) + sideTable.getQty(card);
		if (isTooMany(card, existingQty)) return false;

		table.model.unsortedCards.add(card);
		isDirty = true;
		return true;
	}

	private boolean isTooMany (Card card, int qty) {
		if (qty < 4) return false;
		if (card.isBasicLand()) return false;
		if (card.legal.contains("deck can have any number of cards")) return false;
		return true;
	}

	private boolean removeCardFromTable (Card card, CardTable table) {
		if (card == null) return false;
		for (Iterator<Card> iter = table.model.unsortedCards.iterator(); iter.hasNext();) {
			Card existingCard = iter.next();
			if (!card.equals(existingCard)) continue;
			iter.remove();
			isDirty = true;
			return true;
		}
		return false;
	}

	private void addHistory (DeckAction action) {
		if (action.cardToCount.size() == 0) return;
		// Remove all to the current entry.
		while (!history.isEmpty()) {
			if (history.size() - 1 == historyIndex) break;
			history.removeLast();
		}
		history.addLast(action);
		if (history.size() > 32768)
			history.removeFirst();
		else
			historyIndex++;
	}

	private void updateDeckCount () {
		if (searchDisabled || !isVisible()) return;

		deckStats = new DeckStats();
		deckStats.deckCount = deckTable.model.unsortedCards.size();
		deckStats.sideCount += sideTable.model.unsortedCards.size();
		UI.setTitle(deckGroup, "Deck (" + deckStats.deckCount + "/" + deckStats.sideCount + ")");

		int totalSpells = 0;
		int totalConvertedCost = 0;
		for (Card card : deckTable.model.unsortedCards) {
			if (card.color.equals("Land")) {
				deckStats.land++;
				continue;
			} else if (card.englishType.contains("Creature"))
				deckStats.creatures++;
			else
				deckStats.spells++;

			totalSpells++;

			outerLoop: for (int ii = 0, n = card.castingCost.length(); ii < n; ii++) {
				switch (card.castingCost.charAt(ii)) {
				case 'W':
					deckStats.white++;
					break;
				case 'U':
					deckStats.blue++;
					break;
				case 'B':
					deckStats.black++;
					break;
				case 'R':
					deckStats.red++;
					break;
				case 'G':
					deckStats.green++;
					break;
				case '|':
					break outerLoop;
				}
			}

			totalConvertedCost += card.convertedCastingCost;
			switch (card.convertedCastingCost) {
			case 0:
				deckStats.zero++;
				break;
			case 1:
				deckStats.one++;
				break;
			case 2:
				deckStats.two++;
				break;
			case 3:
				deckStats.three++;
				break;
			case 4:
				deckStats.four++;
				break;
			case 5:
				deckStats.five++;
				break;
			case 6:
				deckStats.six++;
				break;
			case 7:
				deckStats.seven++;
				break;
			case 8:
				deckStats.eight++;
				break;
			default:
				deckStats.ninePlus++;
			}
		}

		// Change from symbol count to symbol percentage.
		// float totalSymbols = deckStats.white + deckStats.blue + deckStats.black + deckStats.red + deckStats.green;
		// deckStats.white = Math.round(deckStats.white / totalSymbols * 100);
		// deckStats.blue = Math.round(deckStats.blue / totalSymbols * 100);
		// deckStats.black = Math.round(deckStats.black / totalSymbols * 100);
		// deckStats.red = Math.round(deckStats.red / totalSymbols * 100);
		// deckStats.green = Math.round(deckStats.green / totalSymbols * 100);

		if (totalSpells > 0) deckStats.avg = String.valueOf(Math.round((totalConvertedCost / (float)totalSpells) * 100) / 100F);

		Set<String> sets;
		sets = arcane.getFormatSets(Format.vintage);
		deckStats.illegalT1main = isIllegal(Format.vintage, sets, deckTable, false);
		deckStats.illegalT1side = isIllegal(Format.vintage, sets, sideTable, false);
		sets = arcane.getFormatSets(Format.legacy);
		deckStats.illegalT15main = isIllegal(Format.legacy, sets, deckTable, false);
		deckStats.illegalT15side = isIllegal(Format.legacy, sets, sideTable, false);
		sets = arcane.getFormatSets(Format.extended);
		deckStats.illegalT1Xmain = isIllegal(Format.extended, sets, deckTable, false);
		deckStats.illegalT1Xside = isIllegal(Format.extended, sets, sideTable, false);
		sets = arcane.getFormatSets(Format.standard);
		deckStats.illegalT2main = isIllegal(Format.standard, sets, deckTable, false);
		deckStats.illegalT2side = isIllegal(Format.standard, sets, sideTable, false);

		Format currentFormat = (Format)presetCombo.getSelectedItem();
		if (deckStats.deckCount < 60 || (deckStats.sideCount != 0 && deckStats.sideCount != 15)) {
			if (currentFormat != Format.all && currentFormat != Format.custom) {
				if (deckStats.deckCount < 60)
					arcane.log("[" + currentFormat + "] Deck must have 60 cards.");
				else
					arcane.log("[" + currentFormat + "] Sideboard must have 0 or 15 cards.");
				arcane.log("");
			}
			deckStats.illegalT1main = true;
			deckStats.illegalT1side = true;
			deckStats.illegalT15main = true;
			deckStats.illegalT15side = true;
			deckStats.illegalT1Xmain = true;
			deckStats.illegalT1Xside = true;
			deckStats.illegalT2main = true;
			deckStats.illegalT2side = true;
		}

		// Check selected sets only.
		sideTable.illegalCards = deckTable.illegalCards;
		deckTable.illegalCards.clear();
		sets = new HashSet<String>();
		for (Object entry : setsList.getSelectedValues())
			sets.add(((SetEntry)entry).set);
		isIllegal(currentFormat, sets, deckTable, true);
		isIllegal(currentFormat, sets, sideTable, true);
		if (!deckTable.illegalCards.isEmpty()) {
			StringBuffer buffer = new StringBuffer(256);
			buffer.append("Illegal cards: ");
			int i = 0, lastIndex = deckTable.illegalCards.size() - 1;
			for (Iterator<Card> iter = deckTable.illegalCards.iterator(); iter.hasNext();) {
				Card card = iter.next();
				buffer.append(card);
				if (i != lastIndex) buffer.append(", ");
				i++;
			}
			buffer.append('\n');
			arcane.log(buffer.toString());
		}

		deckTable.repaint();
		sideTable.repaint();
		deckInfoPanel.repaint();
	}

	private boolean isIllegal (Format format, Set<String> sets, CardTable table, boolean setIllegalCards) {
		CardTable otherTable = (table == deckTable ? sideTable : deckTable);
		for (Card card : table.model.unsortedCards) {
			if (card.isBasicLand()) continue;
			if (!isInSets(card.name, sets)) {
				if (setIllegalCards)
					deckTable.illegalCards.add(card);
				else
					return true;
			}
			if (arcane.isBanned(card.englishName, format)) {
				if (setIllegalCards)
					deckTable.illegalCards.add(card);
				else
					return true;
			}
			if (arcane.isRestricted(card.englishName, format)) {
				int count = table.getQty(card) + otherTable.getQty(card);
				if (count > 1) {
					if (setIllegalCards)
						deckTable.illegalCards.add(card);
					else
						return true;
				}
			}
		}
		if (setIllegalCards)
			return !deckTable.illegalCards.isEmpty();
		else
			return false;
	}

	private boolean isInSets (String cardName, Set<String> sets) {
		for (Card card : arcane.getCards(cardName))
			if (sets.contains(card.set)) return true;
		return false;
	}

	private void updateButtons () {
		boolean canAddToDeck = false;
		for (Card card : cardsTable.getSelectedCards()) {
			if (!isTooMany(card, deckTable.getQty(card) + sideTable.getQty(card))) {
				canAddToDeck = true;
				break;
			}
		}
		addCardButton.setEnabled(canAddToDeck);
		addSideCardButton.setEnabled(canAddToDeck);

		canAddToDeck = false;
		for (Card card : deckTable.getSelectedCards()) {
			if (!isTooMany(card, deckTable.getQty(card) + sideTable.getQty(card))) {
				canAddToDeck = true;
				break;
			}
		}
		for (Card card : sideTable.getSelectedCards()) {
			if (!isTooMany(card, deckTable.getQty(card) + sideTable.getQty(card))) {
				canAddToDeck = true;
				break;
			}
		}
		addDeckButton.setEnabled(canAddToDeck);

		boolean isCardSelected = deckTable.getSelectedRow() != -1 || sideTable.getSelectedRow() != -1;
		removeDeckButton.setEnabled(isCardSelected);
		swapDeckButton.setEnabled(isCardSelected);

		CardTable table = deckTable.getSelectedRow() != -1 ? deckTable : null;
		if (table == null) table = sideTable.getSelectedRow() != -1 ? sideTable : null;
		boolean isUnsortedAndSelected = table != null && table.sortedColumns.size() == 0;
		upDeckButton.setEnabled(isUnsortedAndSelected);
		downDeckButton.setEnabled(isUnsortedAndSelected);
		if (isUnsortedAndSelected) {
			int lastRow = table.model.viewCards.size() - 1;
			for (int rowIndex : table.getSelectedRows()) {
				if (rowIndex == 0) upDeckButton.setEnabled(false);
				if (rowIndex == lastRow) downDeckButton.setEnabled(false);
			}
		}

		undoDeckButton.setEnabled(historyIndex >= 0);
		redoDeckButton.setEnabled(historyIndex < history.size() - 1);

		reorderDeckButton.setEnabled((deckTable.model.viewCards.size() > 1 || sideTable.model.viewCards.size() > 1)
			&& deckTable.sortedColumns.size() == 0 && sideTable.sortedColumns.size() == 0);
		unsortDeckButton.setEnabled(deckTable.sortedColumns.size() > 0 || sideTable.sortedColumns.size() > 0);
	}

	private void quickSearch (final boolean backwards) {
		final String text = quickSearchText.getText().trim().toLowerCase();
		if (text.length() == 0) return;

		if (lastQuickSearch != null && lastQuickSearch.equals(text)) {
			if (quickSearchCards.size() == 0) return;
			if (backwards) {
				quickSearchIndex--;
				if (quickSearchIndex < 0) quickSearchIndex = quickSearchCards.size() - 1;
			} else {
				quickSearchIndex++;
				if (quickSearchIndex >= quickSearchCards.size()) quickSearchIndex = 0;
			}
			cardsTable.setSelectedCard(quickSearchCards.get(quickSearchIndex));
			return;
		}

		lastQuickSearchRefresh = System.currentTimeMillis();
		final long currentRefreshNumber = lastQuickSearchRefresh;
		Util.threadPool.submit(new Runnable() {
			public void run () {
				if (currentRefreshNumber != lastQuickSearchRefresh) return;
				final List<Card> cards = new ArrayList<Card>();
				final List<Card> containsCards = new ArrayList<Card>();
				Pattern startsWithPattern = null;
				Pattern containsPattern = null;
				if (text.contains("*")) {
					String regex = text.toLowerCase();
					if (regex.charAt(regex.length() - 1) != '*') {
						regex = regex + "*";
					}
					if (regex.charAt(0) != '*') {
						startsWithPattern = Pattern.compile(regex.replace("*", ".*"));
						regex = "*" + regex;
					}
					containsPattern = Pattern.compile(regex.replace("*", ".*"));
				}
				for (Card card : cardsTable.model.viewCards) {
					if (currentRefreshNumber != lastQuickSearchRefresh) return;
					String name = card.name.toLowerCase();
					if (containsPattern != null) {
						if (startsWithPattern != null && startsWithPattern.matcher(name).matches())
							cards.add(card);
						else if (containsPattern.matcher(name).matches()) {
							containsCards.add(card);
						}
					} else {
						if (name.startsWith(text))
							cards.add(card);
						else if (name.contains(text)) {
							containsCards.add(card);
						}
					}
				}
				cards.addAll(containsCards);
				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						if (currentRefreshNumber != lastQuickSearchRefresh) return;
						lastQuickSearch = text;
						quickSearchCards = cards;
						quickSearchIndex = -1;
						quickSearch(backwards);
					}
				});
			}
		});
	}

	protected void loadCardImage (Card card) {
		if (prefs.cardImageType == CardImageType.local)
			displayLocalCardImage(card);
		else if (prefs.cardImageType == CardImageType.wizards) {
			displayWizardsCardImage(card);
		}
	}

	private void displayWizardsCardImage (Card card) {
		String name = card.englishName.replace(", ", "_").replaceAll("[ -]", "_").replaceAll("[':\"!]", "").toLowerCase();
		String url = "http://www.wizards.com/global/images/magic/general/" + name + ".jpg";
		try {
			BufferedImage srcImage;
			if(imageCache.containsKey(card))
				srcImage = imageCache.get(card);
			else {
				InputStream stream = new URL(url).openStream();
				srcImage = ImageIO.read(stream);
				imageCache.put(card, srcImage);
			}
			if (!cardInfoPane.isCurrentCard(card)) return;
			cardImagePanel.setScaleLarger(prefs.scaleCardImageLarger);
			cardImagePanel.setImage(srcImage, null);
			cardImagePanel.repaint();
			if (prefs.logFoundImages) arcane.log("Found card image URL: " + url);
		} catch (MalformedURLException ex) {
			if (prefs.logMissingImages) arcane.log("Invalid card image URL: " + url);
			cardImagePanel.clearImage();
		} catch (IOException ex) {
			if (prefs.logMissingImages) arcane.log("Unable to load card image: " + url);
			cardImagePanel.clearImage();
		}
	}

	private void addPaths (List<String> paths, String cardName, Card card, Set<String> sets) {
		cardName = cardName.replace(":", "").replace("/", "");
		for (String set : sets) {
			String base = prefs.imagesPath + set + "/" + cardName;
			if (!"".equals(card.version)) {
				paths.add(base + card.version + prefs.imagesSuffix);
				paths.add(base + " " + card.version + prefs.imagesSuffix);
			}
			paths.add(base + prefs.imagesSuffix);
			String baseUpper = prefs.imagesPath + set.toUpperCase() + "/" + cardName;
			if (!"".equals(card.version)) {
				paths.add(baseUpper + card.version + prefs.imagesSuffix);
				paths.add(baseUpper + " " + card.version + prefs.imagesSuffix);
			}
			paths.add(baseUpper + prefs.imagesSuffix);
		}
		paths.add(prefs.imagesPath + cardName + prefs.imagesSuffix);
	}

	private void displayLocalCardImage (Card card) {
		// Collect all possible sets, with the card's set first.
		LinkedHashSet<String> sets = new LinkedHashSet<String>();
		sets.addAll(arcane.getAlternateSets(card.set));
		for (String set : arcane.getSets(card.name)) {
			// Skip the cards set because it was placed first.
			if (set.equals(card.set)) continue;
			sets.addAll(arcane.getAlternateSets(set));
		}
		// Test a variety of paths.
		List<String> paths = new ArrayList<String>();
		addPaths(paths, card.name, card, sets);
		if (!card.name.equals(card.englishName)) addPaths(paths, card.englishName, card, sets);
		String imagePath = null;
		for (String path : paths) {
			if (new File(path).exists()) {
				imagePath = path;
				break;
			}
		}
		if (imagePath == null) {
			if (prefs.logMissingImages) {
				StringBuffer errorBuffer = new StringBuffer(512);
				errorBuffer.append("Unable to find card image using the following paths:\n");
				for (String path : paths) {
					errorBuffer.append("   ");
					errorBuffer.append(new File(path).getAbsolutePath());
					errorBuffer.append('\n');
				}
				arcane.log(errorBuffer.toString());
			}
			cardImagePanel.clearImage();
		} else {
			try {
				BufferedImage srcImage;
				if(imageCache.containsKey(card))
					srcImage = imageCache.get(card);
				else {
					srcImage = ImageIO.read(new File(imagePath));
					imageCache.put(card, srcImage);
				}
				if (!cardInfoPane.isCurrentCard(card)) return;
				cardImagePanel.setScaleLarger(prefs.scaleCardImageLarger);
				cardImagePanel.setImage(srcImage, null);
				cardImagePanel.repaint();
				if (prefs.logFoundImages) {
					StringBuffer errorBuffer = new StringBuffer(512);
					errorBuffer.append("Found card image using the following paths:\n");
					for (String path : paths) {
						errorBuffer.append("   ");
						if (path == imagePath) errorBuffer.append("*** ");
						errorBuffer.append(new File(path).getAbsolutePath());
						errorBuffer.append('\n');
					}
					arcane.log(errorBuffer.toString());
				}
			} catch (IOException ex) {
				throw new ArcaneException("Error reading image: " + imagePath, ex);
			}
		}
	}

	private void resetSearch () {
		searchDisabled = true;
		for (int i = 0; i < searchButtons.length; i++) {
			JToggleButton[] buttons = searchButtons[i];
			for (int k = 0; k < buttons.length; k++) {
				buttons[k].setSelected(true);
			}
		}
		multiColorButton.setSelected(false);
		exactButton.setSelected(false);
		searchCombo.setSelectedItem("");
		searchDisabled = false;
		updateCardTable();
	}

	private void initializePopupMenus () {
		cardTableCardMenu = new JPopupMenu();
		cardTableCardCountItem = new MenuItem("") {
			public void setArmed (boolean b) {
			}
		};
		Font font = cardTableCardCountItem.getFont();
		cardTableCardCountItem.setFont(new Font(font.getName(), Font.ITALIC, font.getSize()));
		cardTableCardCountItem.setIcon(UI.getImageIcon("/buttons/blank.png"));
		cardTableCardMenu.add(cardTableCardCountItem);
		cardTableCardMenu.addSeparator();
		cardTableCardMenu.add(new ButtonMenuItem("Add to deck (1)", addCardButton));
		cardTableCardMenu.add(new ButtonMenuItem("Add to deck (4)", addCardButton, true));
		cardTableCardMenu.addSeparator();
		cardTableCardMenu.add(new ButtonMenuItem("Add to sideboard (1)", addSideCardButton));
		cardTableCardMenu.add(new ButtonMenuItem("Add to sideboard (4)", addSideCardButton, true));
		addCommonMenus(cardTableCardMenu, cardsTable);

		deckTableCardMenu = new JPopupMenu();
		deckTableCardCountItem = new MenuItem("") {
			public void setArmed (boolean b) {
			}
		};
		font = deckTableCardCountItem.getFont();
		deckTableCardCountItem.setFont(new Font(font.getName(), Font.ITALIC, font.getSize()));
		deckTableCardCountItem.setIcon(UI.getImageIcon("/buttons/blank.png"));
		deckTableCardMenu.add(deckTableCardCountItem);
		deckTableCardMenu.addSeparator();
		deckTableCardMenu.add(new ButtonMenuItem("Increment (1)", addDeckButton));
		deckTableCardMenu.add(new ButtonMenuItem("Increment (4)", addDeckButton, true));
		deckTableCardMenu.add(new ButtonMenuItem("Decrement (1)", removeDeckButton));
		deckTableCardMenu.add(new ButtonMenuItem("Decrement (4)", removeDeckButton, true));
		deckTableCardMenu.add(new ButtonMenuItem("Move to sideboard (1)", swapDeckButton));
		deckTableCardMenu.add(new ButtonMenuItem("Move to sideboard (4)", swapDeckButton, true));
		deckTableCardMenu.addSeparator();
		deckTableCardMenu.add(new ButtonMenuItem("Move up (1)", upDeckButton));
		deckTableCardMenu.add(new ButtonMenuItem("Move up (4)", upDeckButton, true));
		deckTableCardMenu.add(new ButtonMenuItem("Move down (1)", downDeckButton));
		deckTableCardMenu.add(new ButtonMenuItem("Move down (4)", downDeckButton, true));
		addCommonMenus(deckTableCardMenu, deckTable);

		sideTableCardMenu = new JPopupMenu();
		sideTableCardCountItem = new MenuItem("") {
			public void setArmed (boolean b) {
			}
		};
		font = sideTableCardCountItem.getFont();
		sideTableCardCountItem.setFont(new Font(font.getName(), Font.ITALIC, font.getSize()));
		sideTableCardCountItem.setIcon(UI.getImageIcon("/buttons/blank.png"));
		sideTableCardMenu.add(sideTableCardCountItem);
		sideTableCardMenu.addSeparator();
		sideTableCardMenu.add(new ButtonMenuItem("Increment (1)", addDeckButton));
		sideTableCardMenu.add(new ButtonMenuItem("Increment (4)", addDeckButton, true));
		sideTableCardMenu.add(new ButtonMenuItem("Decrement (1)", removeDeckButton));
		sideTableCardMenu.add(new ButtonMenuItem("Decrement (4)", removeDeckButton, true));
		sideTableCardMenu.add(new ButtonMenuItem("Move to deck (1)", swapDeckButton));
		sideTableCardMenu.add(new ButtonMenuItem("Move to deck (4)", swapDeckButton, true));
		sideTableCardMenu.addSeparator();
		sideTableCardMenu.add(new ButtonMenuItem("Move up (1)", upDeckButton));
		sideTableCardMenu.add(new ButtonMenuItem("Move up (4)", upDeckButton, true));
		sideTableCardMenu.add(new ButtonMenuItem("Move down (1)", downDeckButton));
		sideTableCardMenu.add(new ButtonMenuItem("Move down (4)", downDeckButton, true));
		addCommonMenus(sideTableCardMenu, sideTable);
	}

	private void addCommonMenus (JPopupMenu menu, final CardTable table) {
		menu.addSeparator();

		Menu ratingMenu = new Menu("Rating");
		ratingMenu.setIcon(new ImageIcon(RatingRenderer.starImage));
		menu.add(ratingMenu);
		for (int i = 0; i <= 5; i++) {
			MenuItem menuItem = new MenuItem(String.valueOf(i));
			final int rating = i;
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					for (Card card : table.getSelectedCards()) {
						card.rating = rating;
						updateRows(card.name);
					}
				}
			});
			ratingMenu.add(menuItem);
		}

		Menu setFlagMenu = new Menu("Flags");
		setFlagMenu.setIcon(new ImageIcon(FlagRenderer.indexToFlag.get(0).image));
		menu.add(setFlagMenu);
		for (final Flag flag : FlagRenderer.indexToFlag.values()) {
			MenuItem menuItem = new MenuItem(flag.name, flag.image);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					boolean allAreSet = true;
					for (Card card : table.getSelectedCards()) {
						if (card.flags.indexOf(flag.letter) == -1) {
							allAreSet = false;
							break;
						}
					}
					for (Card card : table.getSelectedCards()) {
						card.setFlag(flag.letter, !allAreSet);
						updateRows(card.name);
					}
				}
			});
			setFlagMenu.add(menuItem);
		}

		menu.addSeparator();

		Menu addToMenu = new Menu("Add to decklist");
		addToMenu.setIcon(UI.getImageIcon("/buttons/addto.png"));
		menu.add(addToMenu);
		MenuItem browseMenuItem = new MenuItem("Browse...");
		browseMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				DecklistFile decklistFile = DecklistFile.open(DeckBuilder.this, null);
				if (decklistFile == null) return;
				try {
					decklistFile.open();
				} catch (IOException ex) {
					throw new ArcaneException(ex);
				}
				decklistFile.addCards(table.getSelectedCards());
				lastOpenedDecklists.remove(decklistFile);
				lastOpenedDecklists.addFirst(decklistFile);
				updateMRU();
			}
		});
		addToMenu.add(browseMenuItem);
		addToMenus.put(addToMenu, table);
	}

	private void initializeEvents () {
		InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = rootPane.getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "focusSearch");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_MASK), "focusSearch");
		actionMap.put("focusSearch", new AbstractAction() {
			public void actionPerformed (ActionEvent evt) {
				searchCombo.requestFocus();
				searchCombo.getEditor().selectAll();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.ALT_MASK), "focusQuickSearch");
		actionMap.put("focusQuickSearch", new AbstractAction() {
			public void actionPerformed (ActionEvent evt) {
				quickSearchText.requestFocus();
				quickSearchText.selectAll();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_MASK), "focusDeckTable");
		actionMap.put("focusDeckTable", new AbstractAction() {
			public void actionPerformed (ActionEvent evt) {
				if (deckTable.isFocusOwner()) {
					sideTable.requestFocus();
					if (sideTable.getSelectedRow() == -1) selectFirstVisibleRow(sideTable);
				} else {
					deckTable.requestFocus();
					if (deckTable.getSelectedRow() == -1) selectFirstVisibleRow(deckTable);
				}
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.ALT_MASK), "focusCardsTable");
		actionMap.put("focusCardsTable", new AbstractAction() {
			public void actionPerformed (ActionEvent evt) {
				cardsTable.requestFocus();
				if (cardsTable.getSelectedRow() == -1) selectFirstVisibleRow(cardsTable);
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK), "undoDeckEdit");
		actionMap.put("undoDeckEdit", new AbstractAction() {
			public void actionPerformed (ActionEvent evt) {
				undoDeckButton.doClick(0);
			}
		});
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK), "redoDeckEdit");
		actionMap.put("redoDeckEdit", new AbstractAction() {
			public void actionPerformed (ActionEvent evt) {
				redoDeckButton.doClick(0);
			}
		});

		ActionListener updateCardTable = new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				updateCardTable();
			}
		};
		whiteButton.addActionListener(updateCardTable);
		blueButton.addActionListener(updateCardTable);
		blackButton.addActionListener(updateCardTable);
		redButton.addActionListener(updateCardTable);
		greenButton.addActionListener(updateCardTable);
		colorlessButton.addActionListener(updateCardTable);
		exactButton.addActionListener(updateCardTable);
		multiColorButton.addActionListener(updateCardTable);
		landButton.addActionListener(updateCardTable);
		artifactButton.addActionListener(updateCardTable);
		sorceryButton.addActionListener(updateCardTable);
		creatureButton.addActionListener(updateCardTable);
		instantButton.addActionListener(updateCardTable);
		enchantButton.addActionListener(updateCardTable);
		auraButton.addActionListener(updateCardTable);
		planeswalkerButton.addActionListener(updateCardTable);
		titleButton.addActionListener(updateCardTable);
		textButton.addActionListener(updateCardTable);
		typeButton.addActionListener(updateCardTable);
		searchButton.addActionListener(updateCardTable);
		commonButton.addActionListener(updateCardTable);
		uncommonButton.addActionListener(updateCardTable);
		rareButton.addActionListener(updateCardTable);
		mythicrareButton.addActionListener(updateCardTable);
		uniqueOnlyButton.addActionListener(updateCardTable);
		ownedOnlyButton.addActionListener(updateCardTable);

		searchCombo.getEditor().getEditorComponent().setFocusTraversalKeysEnabled(false);
		searchCombo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			public void keyPressed (KeyEvent evt) {
				switch (evt.getKeyCode()) {
				case KeyEvent.VK_ESCAPE:
					resetSearch();
					evt.consume();
					break;
				case KeyEvent.VK_ENTER:
					updateCardTable();
					break;
				case KeyEvent.VK_TAB:
					if (evt.isShiftDown()) {
						deckTable.requestFocus();
						if (deckTable.getSelectedRow() == -1) selectFirstVisibleRow(deckTable);
					} else {
						cardsTable.requestFocus();
						if (cardsTable.getSelectedRow() == -1) selectFirstVisibleRow(cardsTable);
					}
					evt.consume();
					break;
				}
			}
		});

		MouseListener clicked = new MouseAdapter() {
			public void mouseClicked (MouseEvent evt) {
				if (evt.getButton() != 3) return;
				searchDisabled = true;
				JToggleButton clickedButton = (JToggleButton)evt.getSource();
				boolean selected = !clickedButton.isSelected();
				for (JToggleButton[] buttons : searchButtons) {
					List<JToggleButton> buttonList = Arrays.asList(buttons);
					if (buttonList.contains(clickedButton)) {
						for (JToggleButton button : buttonList)
							button.setSelected(selected);
						break;
					}
				}
				searchDisabled = false;
				updateCardTable();
			}
		};
		whiteButton.addMouseListener(clicked);
		blueButton.addMouseListener(clicked);
		blackButton.addMouseListener(clicked);
		redButton.addMouseListener(clicked);
		greenButton.addMouseListener(clicked);
		colorlessButton.addMouseListener(clicked);
		exactButton.addMouseListener(clicked);
		multiColorButton.addMouseListener(clicked);
		landButton.addMouseListener(clicked);
		artifactButton.addMouseListener(clicked);
		creatureButton.addMouseListener(clicked);
		sorceryButton.addMouseListener(clicked);
		instantButton.addMouseListener(clicked);
		enchantButton.addMouseListener(clicked);
		auraButton.addMouseListener(clicked);
		planeswalkerButton.addMouseListener(clicked);
		titleButton.addMouseListener(clicked);
		textButton.addMouseListener(clicked);
		typeButton.addMouseListener(clicked);
		commonButton.addMouseListener(clicked);
		uncommonButton.addMouseListener(clicked);
		rareButton.addMouseListener(clicked);
		mythicrareButton.addMouseListener(clicked);

		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				resetSearch();
			}
		});

		quickSearchText.addKeyListener(new KeyAdapter() {
			public void keyPressed (KeyEvent evt) {
				switch (evt.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					boolean backwards = evt.isAltDown() || evt.isShiftDown() || evt.isControlDown();
					quickSearch(backwards);
					break;
				case KeyEvent.VK_TAB:
					cardsTable.requestFocus();
					if (cardsTable.getSelectedRow() == -1) selectFirstVisibleRow(cardsTable);
					evt.consume();
					break;
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_PAGE_UP:
				case KeyEvent.VK_PAGE_DOWN:
					cardsTable.requestFocus();
					try {
						Util.robot.keyPress(evt.getKeyCode());
						Util.robot.keyRelease(evt.getKeyCode());
					} catch (Exception ignored) {
					}
					evt.consume();
					break;
				default:
					if (lastQuickSearch == null || !lastQuickSearch.equals(quickSearchText.getText())) quickSearch(false);
				}
			}
		});
		quickSearchText.getDocument().addDocumentListener(new DocumentModifiedListener() {
			public void changed () {
				quickSearchButton.setEnabled(quickSearchText.getText().length() > 0);
				quickSearch(false);
			}
		});
		quickSearchButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				boolean backwards = quickSearchButton.isAlternate(evt) || (evt.getModifiers() & InputEvent.SHIFT_MASK) != 0;
				quickSearch(backwards);
			}
		});

		addCardButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				List<Card> cards = cardsTable.getSelectedCards();
				DeckAction action = new DeckAction(deckTable, Action.add);
				for (int i = 0, n = addCardButton.isAlternate(evt) ? 4 : 1; i < n; i++)
					for (Card card : cards)
						if (addCardToTable(card, deckTable)) action.addCard(card);
				setCurrentDecklist(currentDecklist);
				addHistory(action);
				updateDeckCount();
				deckTable.sort();
				deckTable.setSelectedCards(cards);
				deckTable.requestFocus();
				updateButtons();
			}
		});
		addSideCardButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				List<Card> cards = cardsTable.getSelectedCards();
				DeckAction action = new DeckAction(sideTable, Action.add);
				for (int i = 0, n = addSideCardButton.isAlternate(evt) ? 4 : 1; i < n; i++)
					for (Card card : cards)
						if (addCardToTable(card, sideTable)) action.addCard(card);
				setCurrentDecklist(currentDecklist);
				addHistory(action);
				updateDeckCount();
				sideTable.sort();
				sideTable.setSelectedCards(cards);
				sideTable.requestFocus();
				updateButtons();
			}
		});

		cardsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent evt) {
				if (evt.getValueIsAdjusting()) return;
				if (ignoreSelections) return;
				Card card = cardsTable.getSelectedCard();
				cardInfoPane.setCard(card);
				ignoreSelections = true;
				deckTable.clearSelection();
				if (card != null) {
					deckTable.setSelectedCards(card.name);
					if (deckTable.getSelectedRow() == -1) {
						sideTable.clearSelection();
						sideTable.setSelectedCards(card.name);
					}
				}
				ignoreSelections = false;
				updateButtons();
			}
		});
		cardsTable.addFocusListener(new FocusAdapter() {
			public void focusGained (FocusEvent evt) {
				cardInfoPane.setCard(cardsTable.getSelectedCard());
			}
		});
		cardsTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent evt) {
				List<Card> cards = cardsTable.getSelectedCards();

				if (evt.getClickCount() == 1 && evt.getButton() == 3) {
					setCardCountMenuItem(cardTableCardCountItem, cards);
					cardTableCardMenu.show(cardsTable, evt.getX() + 1, evt.getY() + 1);
					return;
				}

				if (evt.getClickCount() < 2 || evt.getClickCount() % 2 != 0) return;
				CardTable toTable = evt.getButton() == 1 ? deckTable : sideTable;
				DeckAction action = new DeckAction(toTable, Action.add);
				for (int i = 0, n = evt.isAltDown() ? 4 : 1; i < n; i++)
					for (Card card : cards)
						if (addCardToTable(card, toTable)) action.addCard(card);
				setCurrentDecklist(currentDecklist);
				addHistory(action);
				updateDeckCount();
				toTable.sort();
				toTable.setSelectedCards(cards);
				updateButtons();
			}
		});
		cardsTable.addKeyListener(new KeyAdapter() {
			public void keyPressed (KeyEvent evt) {
				switch (evt.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					if (evt.isAltDown()) {
						addSideCardButton.setNextAlternate(false);
						addSideCardButton.doClick(0);
					} else
						addCardButton.doClick(0);
					evt.consume();
					return;
				case KeyEvent.VK_EQUALS:
					if (!evt.isShiftDown()) break;
				case KeyEvent.VK_ADD:
					List<Card> addCards = cardsTable.getSelectedCards();
					for (Card card : addCards) {
						card.ownedQty++;
					}
					cardsTable.repaint();
					deckTable.repaint();
					evt.consume();
					return;
				case KeyEvent.VK_SUBTRACT:
					List<Card> subCards = cardsTable.getSelectedCards();
					for (Card card : subCards) {
						if(card.ownedQty>0)
							card.ownedQty--;
					}
					cardsTable.repaint();
					deckTable.repaint();
					evt.consume();
					return;
				case KeyEvent.VK_C:
					if (evt.isControlDown()) {
						StringBuffer buffer = new StringBuffer(256);
						List<Card> cards = cardsTable.getSelectedCards();
						int i = 0, lastIndex = cards.size() - 1;
						for (Card card : cards) {
							buffer.append(card.name);
							if (i != lastIndex) buffer.append("\n");
						}
						Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(buffer.toString()), null);
						evt.consume();
						return;
					}
					break;
				}

				switch (evt.getKeyCode()) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_PAGE_UP:
				case KeyEvent.VK_PAGE_DOWN:
					if (cardsTable.getSelectedRow() != -1)
						cardsTable.scrollRectToVisible(cardsTable.getCellRect(cardsTable.getSelectedRow(), 0, true));
					break;
				case KeyEvent.VK_HOME:
					if (evt.isControlDown()) break;
					if (cardsTable.model.viewCards.size() > 0) cardsTable.setSelectedCard(cardsTable.model.viewCards.get(0));
					break;
				case KeyEvent.VK_END:
					if (evt.isControlDown()) break;
					int size = cardsTable.model.viewCards.size();
					if (size > 0) cardsTable.setSelectedCard(cardsTable.model.viewCards.get(size - 1));
					break;
				case KeyEvent.VK_DELETE:
					removeDeckButton.doClick(0);
					break;
				case KeyEvent.VK_ALT:
				case KeyEvent.VK_SHIFT:
				case KeyEvent.VK_CONTROL:
					break;
				case KeyEvent.VK_TAB:
					if (evt.isShiftDown() && !sideTable.model.viewCards.isEmpty()) {
						sideTable.requestFocus();
						if (sideTable.getSelectedRow() == -1) selectFirstVisibleRow(sideTable);
					} else {
						deckTable.requestFocus();
						if (deckTable.getSelectedRow() == -1) selectFirstVisibleRow(deckTable);
					}
					evt.consume();
					break;
				case KeyEvent.VK_A:
					if (evt.isControlDown()) break;
					// Fall through.
				default:
					quickSearchText.requestFocus();
					quickSearchText.selectAll();
					try {
						Util.robot.keyPress(evt.getKeyCode());
						Util.robot.keyRelease(evt.getKeyCode());
					} catch (Exception ignored) {
					}
					evt.consume();
					break;
				}
			}
		});

		addDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				CardTable table = deckTable.getSelectedCard() != null ? deckTable : sideTable;
				List<Card> cards = table.getSelectedCards();
				DeckAction action = new DeckAction(table, Action.add);
				for (int i = 0, n = addDeckButton.isAlternate(evt) ? 4 : 1; i < n; i++)
					for (Card card : cards)
						if (addCardToTable(card, table)) action.addCard(card);
				setCurrentDecklist(currentDecklist);
				addHistory(action);
				updateDeckCount();
				table.sort();
				table.setSelectedCards(cards);
				updateButtons();
			}
		});
		removeDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				CardTable table = deckTable.getSelectedCard() != null ? deckTable : sideTable;
				List<Card> cards = table.getSelectedCards();
				DeckAction action = new DeckAction(table, Action.remove);
				for (int i = 0, n = removeDeckButton.isAlternate(evt) ? 4 : 1; i < n; i++)
					for (Card card : cards)
						if (removeCardFromTable(card, table)) action.addCard(card);
				setCurrentDecklist(currentDecklist);
				addHistory(action);
				updateDeckCount();
				table.sort();
				table.setSelectedCards(cards);
				updateButtons();
			}
		});
		swapDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				CardTable fromTable = deckTable.getSelectedCard() != null ? deckTable : sideTable;
				CardTable toTable = fromTable == sideTable ? deckTable : sideTable;
				List<Card> cards = fromTable.getSelectedCards();
				DeckAction removeAction = new DeckAction(fromTable, Action.remove);
				DeckAction addAction = new DeckAction(toTable, Action.add);
				for (int i = 0, n = swapDeckButton.isAlternate(evt) ? 4 : 1; i < n; i++) {
					for (Card card : cards) {
						boolean removed = removeCardFromTable(card, fromTable);
						if (removed) {
							removeAction.addCard(card);
							if (addCardToTable(card, toTable)) addAction.addCard(card);
						}
					}
				}
				setCurrentDecklist(currentDecklist);
				addHistory(removeAction);
				addHistory(addAction);
				updateDeckCount();
				toTable.sort();
				fromTable.sort();
				fromTable.setSelectedCards(cards);
				if (fromTable.getSelectedRow() == -1) toTable.setSelectedCards(cards);
				updateButtons();
			}
		});

		upDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				CardTable table = deckTable.getSelectedCard() != null ? deckTable : sideTable;
				List<Card> cards = table.getSelectedCards();
				outerLoop: for (int i = 0, n = upDeckButton.isAlternate(evt) ? 4 : 1; i < n; i++) {
					List<Integer> indices = new ArrayList<Integer>();
					for (Card card : cards) {
						int rowIndex = table.model.unsortedCards.indexOf(card);
						if (rowIndex == 0) break outerLoop;
						indices.add(rowIndex);
					}
					Collections.sort(indices, new Comparator<Integer>() {
						public int compare (Integer o1, Integer o2) {
							return o1 - o2;
						}
					});
					for (int rowIndex : indices) {
						Card card = table.model.unsortedCards.remove(rowIndex);
						rowIndex--;
						table.model.unsortedCards.add(rowIndex, card);
					}
				}
				table.sort();
			}
		});
		downDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				CardTable table = deckTable.getSelectedCard() != null ? deckTable : sideTable;
				List<Card> cards = table.getSelectedCards();
				outerLoop: for (int i = 0, n = downDeckButton.isAlternate(evt) ? 4 : 1; i < n; i++) {
					List<Integer> indices = new ArrayList<Integer>();
					int lastIndex = table.model.unsortedCards.size() - 1;
					for (Card card : cards) {
						int rowIndex = table.model.unsortedCards.indexOf(card);
						if (rowIndex == lastIndex) break outerLoop;
						indices.add(rowIndex);
					}
					Collections.sort(indices, new Comparator<Integer>() {
						public int compare (Integer o1, Integer o2) {
							return o2 - o1;
						}
					});
					for (int rowIndex : indices) {
						Card card = table.model.unsortedCards.remove(rowIndex);
						if (rowIndex < lastIndex) rowIndex++;
						table.model.unsortedCards.add(rowIndex, card);
					}
				}
				table.sort();
			}
		});
		reorderDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				Comparator<Card> comparator = new Comparator<Card>() {
					public int compare (Card c1, Card c2) {
						int diff = getType(c1) - getType(c2);
						if (diff != 0) return diff;

						diff = SortedTable.powerComparator.compare(c1, c2);
						if (diff != 0) return -diff;

						diff = SortedTable.castingCostComparator.compare(c1, c2);
						if (diff != 0) return -diff;

						return c1.name.compareTo(c2.name);
					}

					private Integer getType (Card card) {
						String type = card.englishType.toLowerCase();
						if (card.color.toLowerCase().contains("land")) return card.isBasicLand() ? 1 : 2;
						if (type.contains("planeswalker")) return 3;
						if (type.contains("creature")) return 4;
						if (type.contains("aura")) return 5;
						if (type.contains("sorcery")) return 6;
						if (type.contains("instant")) return 7;
						if (type.contains("enchantment")) return 8;
						return 99999;
					}
				};
				Collections.sort(deckTable.model.unsortedCards, comparator);
				deckTable.sort();
				Collections.sort(sideTable.model.unsortedCards, comparator);
				sideTable.sort();
				updateButtons();
			}
		});
		unsortDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				deckTable.sortedColumns.clear();
				deckTable.sort();
				sideTable.sortedColumns.clear();
				sideTable.sort();
				updateButtons();
			}
		});

		undoDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				DeckAction action = null;
				for (int i = 0, n = undoDeckButton.isAlternate(evt) ? 4 : 1; i < n; i++) {
					if (historyIndex < 0) break;
					action = history.get(historyIndex);
					historyIndex--;
					for (Entry<Card, Integer> entry : action.cardToCount.entrySet()) {
						Card card = entry.getKey();
						int count = entry.getValue();
						for (int ii = 0; ii < count; ii++) {
							if (action.action == Action.remove)
								addCardToTable(card, action.table);
							else
								removeCardFromTable(card, action.table);
						}
					}
				}
				setCurrentDecklist(currentDecklist);
				action.table.sort();
				action.table.setSelectedCards(action.cardToCount.keySet());
				updateButtons();
				updateDeckCount();
			}
		});
		redoDeckButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				DeckAction action = null;
				for (int i = 0, n = redoDeckButton.isAlternate(evt) ? 4 : 1; i < n; i++) {
					if (historyIndex >= history.size() - 1) break;
					historyIndex++;
					action = history.get(historyIndex);
					for (Entry<Card, Integer> entry : action.cardToCount.entrySet()) {
						Card card = entry.getKey();
						int count = entry.getValue();
						for (int ii = 0; ii < count; ii++) {
							if (action.action == Action.add)
								addCardToTable(card, action.table);
							else
								removeCardFromTable(card, action.table);
						}
					}
				}
				setCurrentDecklist(currentDecklist);
				action.table.sort();
				action.table.setSelectedCards(action.cardToCount.keySet());
				updateButtons();
				updateDeckCount();
			}
		});

		KeyListener tableKeyListener = new KeyAdapter() {
			public void keyPressed (KeyEvent evt) {
				CardTable table = ((CardTable)evt.getSource());
				int keyCode = evt.getKeyCode();
				if (keyCode != KeyEvent.VK_CONTROL && keyCode != KeyEvent.VK_SHIFT && keyCode != KeyEvent.VK_ALT) {
					if (table.getSelectedRow() != -1) table.scrollRectToVisible(table.getCellRect(table.getSelectedRow(), 0, true));
				}
				switch (keyCode) {
				case KeyEvent.VK_DELETE:
					removeDeckButton.doClick(0);
					break;
				case KeyEvent.VK_ENTER:
					if (evt.isAltDown()) {
						swapDeckButton.setNextAlternate(false);
						swapDeckButton.doClick(0);
					} else
						addDeckButton.doClick(0);
					evt.consume();
					break;
				case KeyEvent.VK_UP:
					if (evt.isAltDown()) {
						upDeckButton.setNextAlternate(false);
						upDeckButton.doClick(0);
					}
					break;
				case KeyEvent.VK_DOWN:
					if (evt.isAltDown()) {
						downDeckButton.setNextAlternate(false);
						downDeckButton.doClick(0);
					}
					break;
				case KeyEvent.VK_HOME:
					if (evt.isControlDown()) break;
					if (table.model.viewCards.size() > 0) table.setSelectedCard(table.model.viewCards.get(0));
					break;
				case KeyEvent.VK_END:
					if (evt.isControlDown()) break;
					int size = table.model.viewCards.size();
					if (size > 0) table.setSelectedCard(table.model.viewCards.get(size - 1));
					break;
				case KeyEvent.VK_TAB:
					if (evt.isShiftDown() && table == sideTable) {
						deckTable.requestFocus();
						if (deckTable.getSelectedRow() == -1) selectFirstVisibleRow(deckTable);
					} else if (!evt.isShiftDown() && table == deckTable && !sideTable.model.viewCards.isEmpty()) {
						sideTable.requestFocus();
						if (sideTable.getSelectedRow() == -1) selectFirstVisibleRow(sideTable);
					} else {
						cardsTable.requestFocus();
						if (cardsTable.getSelectedRow() == -1) selectFirstVisibleRow(cardsTable);
					}
					evt.consume();
					break;
				case KeyEvent.VK_C:
					if (evt.isControlDown()) {
						// BOZO - Copy the displayed columns.
						StringBuffer buffer = new StringBuffer(256);
						for (Card card : table.getSelectedCards()) {
							buffer.append(table.getQty(card));
							buffer.append(" ");
							buffer.append(card.name);
							buffer.append("\n");
						}
						Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(buffer.toString()), null);
						evt.consume();
					}
					break;
				}
			}
		};

		deckTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent evt) {
				if (evt.getValueIsAdjusting()) return;
				if (ignoreSelections) return;
				Card card = deckTable.getSelectedCard();
				cardInfoPane.setCard(card);
				ignoreSelections = true;
				cardsTable.clearSelection();
				if (linkTablesButton.isSelected() && card != null) {
					if (!uniqueOnlyButton.isSelected())
						cardsTable.setSelectedCard(card);
					else
						cardsTable.setSelectedCards(card.name);
				}
				ignoreSelections = false;
				updateButtons();
			}
		});
		deckTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent evt) {
				List<Card> cards = deckTable.getSelectedCards();

				if (evt.getClickCount() == 1 && evt.getButton() == 3) {
					setCardCountMenuItem(deckTableCardCountItem, cards);
					deckTableCardMenu.show(deckTable, evt.getX() + 1, evt.getY() + 1);
					return;
				}

				if (evt.getClickCount() < 2 || evt.getClickCount() % 2 != 0) return;
				DeckAction addAction = new DeckAction(evt.getButton() == 1 ? deckTable : sideTable, Action.add);
				DeckAction removeAction = new DeckAction(deckTable, Action.remove);
				for (int i = 0, n = evt.isAltDown() ? 4 : 1; i < n; i++) {
					for (Card card : cards) {
						if (evt.getButton() == 1) {
							if (addCardToTable(card, deckTable)) addAction.addCard(card);
						} else if (evt.getButton() == 3) {
							boolean removed = removeCardFromTable(card, deckTable);
							if (removed) {
								removeAction.addCard(card);
								if (evt.isControlDown()) {
									if (addCardToTable(card, sideTable)) addAction.addCard(card);
								}
							}
						}
					}
				}
				setCurrentDecklist(currentDecklist);
				addHistory(addAction);
				addHistory(removeAction);
				updateDeckCount();
				if (evt.getButton() == 3 && evt.isControlDown()) sideTable.sort();
				deckTable.sort();
				updateButtons();
			}
		});
		deckTable.getModel().addTableModelListener(new TableModelListener() {
			public void tableChanged (TableModelEvent evt) {
				updateButtons();
			}
		});
		deckTable.addFocusListener(new FocusAdapter() {
			public void focusGained (FocusEvent evt) {
				cardInfoPane.setCard(deckTable.getSelectedCard());
			}
		});
		deckTable.addKeyListener(tableKeyListener);

		sideTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent evt) {
				if (evt.getValueIsAdjusting()) return;
				if (ignoreSelections) return;
				Card card = sideTable.getSelectedCard();
				cardInfoPane.setCard(card);
				ignoreSelections = true;
				cardsTable.clearSelection();
				if (linkTablesButton.isSelected() && card != null) {
					if (!uniqueOnlyButton.isSelected())
						cardsTable.setSelectedCard(card);
					else
						cardsTable.setSelectedCards(card.name);
				}
				ignoreSelections = false;
				updateButtons();
			}
		});
		sideTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent evt) {
				List<Card> cards = sideTable.getSelectedCards();

				if (evt.getClickCount() == 1 && evt.getButton() == 3) {
					setCardCountMenuItem(sideTableCardCountItem, cards);
					sideTableCardMenu.show(sideTable, evt.getX() + 1, evt.getY() + 1);
					return;
				}

				if (evt.getClickCount() < 2 || evt.getClickCount() % 2 != 0) return;
				DeckAction addAction = new DeckAction(evt.getButton() == 1 ? sideTable : deckTable, Action.add);
				DeckAction removeAction = new DeckAction(sideTable, Action.remove);
				for (int i = 0, n = evt.isAltDown() ? 4 : 1; i < n; i++) {
					for (Card card : cards) {
						if (evt.getButton() == 1) {
							if (addCardToTable(card, sideTable)) addAction.addCard(card);
						} else if (evt.getButton() == 3) {
							boolean removed = removeCardFromTable(card, sideTable);
							if (removed) {
								removeAction.addCard(card);
								if (evt.isControlDown()) {
									if (addCardToTable(card, deckTable)) addAction.addCard(card);
								}
							}
						}
					}
				}
				setCurrentDecklist(currentDecklist);
				addHistory(addAction);
				addHistory(removeAction);
				updateDeckCount();
				if (evt.getButton() == 3 && evt.isControlDown()) deckTable.sort();
				sideTable.sort();
				updateButtons();
			}
		});
		sideTable.getModel().addTableModelListener(new TableModelListener() {
			public void tableChanged (TableModelEvent evt) {
				updateButtons();
			}
		});
		sideTable.addFocusListener(new FocusAdapter() {
			public void focusGained (FocusEvent evt) {
				cardInfoPane.setCard(sideTable.getSelectedCard());
			}
		});
		sideTable.addKeyListener(tableKeyListener);

		newMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				if (isDirty) {
					int result = JOptionPane.showConfirmDialog(DeckBuilder.this, "Unsaved changes will be lost.", "Confirm New",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if (result != JOptionPane.OK_OPTION) return;
				}
				loadDecklist(null);
			}
		});

		openMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				if (isDirty) {
					int result = JOptionPane.showConfirmDialog(DeckBuilder.this, "Unsaved changes will be lost.", "Confirm Open",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if (result != JOptionPane.OK_OPTION) return;
				}
				DecklistFile currentFile = null;
				if (!lastOpenedDecklists.isEmpty()) {
					Decklist decklist = lastOpenedDecklists.getFirst();
					if (decklist instanceof DecklistFile) currentFile = (DecklistFile)decklist;
				}
				DecklistFile decklistFile = DecklistFile.open(DeckBuilder.this, currentFile);
				if (decklistFile == null) return;
				loadDecklist(decklistFile);
			}
		});

		saveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				if (currentDecklist != null) {
					saveDecklist(currentDecklist, true);
					return;
				}
				saveAsMenuItem.doClick(0);
			}
		});

		saveAsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				if (!lastOpenedDecklists.isEmpty()) {
					Decklist decklist = lastOpenedDecklists.getFirst();
					if (decklist instanceof DecklistFile) {
						DecklistFile decklistFile = (DecklistFile)decklist;
						saveFileChooser.setCurrentDirectory(decklistFile.file);
						for (FileFilter fileFilter : saveFileChooser.getChoosableFileFilters()) {
							if (fileFilter.getDescription().replace("*.", "").equals(decklistFile.templateName)) {
								saveFileChooser.removeChoosableFileFilter(fileFilter);
								saveFileChooser.addChoosableFileFilter(fileFilter);
								break;
							}
						}
					}
				}
				int result = saveFileChooser.showSaveDialog(DeckBuilder.this);
				if (result != JFileChooser.APPROVE_OPTION) return;

				String path = saveFileChooser.getSelectedFile().getAbsolutePath();
				String templateName = saveFileChooser.getFileFilter().getDescription().replace("*.", "");

				// Append the template's extension to the path if it has no extension.
				/*String extension = FileUtil.getExtension(path);
				if (extension == null) {
					Matcher matcher = Pattern.compile(".+\\((.+)\\)$").matcher(templateName);
					matcher.matches();
					File file = new File(path);
					path = new File(file.getParent(), file.getName() + '.' + matcher.group(1)).getAbsolutePath();
				}*/

				// Force the template's extension to the path if it doesn't have it.
				Matcher matcher = Pattern.compile(".+\\((.+)\\)$").matcher(templateName);
				matcher.matches();
				if (!path.toLowerCase().endsWith(matcher.group(1)))
					path += '.' + matcher.group(1);

				DecklistFile decklistFile = new DecklistFile(path, templateName);

				if (!decklistFile.isOpenable()) {
					JOptionPane.showMessageDialog(DeckBuilder.this, //
						"You have chosen to save this decklist in the \"" + templateName + "\" format.\n" + //
							"Note that only the CSV, Apprentice, MWS, and MTGO formats\n" + //
							"can be reopened by the Deck Builder.", "Unable to Reopen Warning", JOptionPane.WARNING_MESSAGE);
				}

				saveDecklist(decklistFile, true);
			}
		});

		helpMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				helpFrame.setVisible(true);
			}
		});

		newWindowMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				DeckBuilder deckBuilder = new DeckBuilder();
				if (deckBuilder.getExtendedState() != JFrame.MAXIMIZED_BOTH) {
					Point location = deckBuilder.getLocation();
					deckBuilder.setLocation(location.x + 22, location.y + 22);
				}
				deckBuilder.setVisible(true);
			}
		});

		mtgoImportMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				JFileChooser fileChooser = new JFileChooser("decks");
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(FileUtil.chooserFileEndsWith("MTGO (*.csv)", ".csv"));
				fileChooser.setDialogTitle("Import owned quantities");
				int result = fileChooser.showDialog(DeckBuilder.this, "Import");
				if (result != JFileChooser.APPROVE_OPTION) return;

				result = JOptionPane.showConfirmDialog(DeckBuilder.this, "All existing owned quantity values will be lost.",
					"Confirm Overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.OK_OPTION) return;
				for (Card card : arcane.getCards())
					card.ownedQty = 0;

				try {
					CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(fileChooser.getSelectedFile())), ",",
						"\"", true, true);
					while (true) {
						List<String> fields = reader.getFields();
						if (fields == null) break;
						if (fields.size() < 7) continue;
						String cardName = fields.get(0);
						cardName = cardName.replace("(premium)", "");
						cardName = cardName.replace("Æ", "AE");
						cardName = cardName.replace("û", "u");
						cardName = cardName.replace("â", "a");
						cardName = cardName.replace("ä", "a");
						cardName = cardName.replace("ô", "o");
						cardName = cardName.replace("î", "i");
						cardName = cardName.trim();
						if (cardName.equals("Card Name")) continue;
						int qty = Integer.parseInt(fields.get(1));
						String set = arcane.getMainSet(fields.get(4));
						String collectorNumber = fields.get(5);

						try {
        						String version = "dummy";
        						for (Card card : arcane.getCards(cardName, set)) {
        							if (card.collectorNumber.equals(collectorNumber)) {
        								version = card.version;
        								break;
        							}
        						}
        						if ("dummy".equals(version)) {
        							arcane.log("Unable to import owned quantities for card \"" + cardName
        								+ "\", unknown collector number for set \"" + fields.get(4) + "\": " + collectorNumber);
        							continue;
        						}
        						Card card = arcane.getCard(cardName, set, version);
        						qty += card.ownedQty;
        						card.ownedQty = qty;
						} catch(ArcaneException e){
						    arcane.log("Card not found: \"" + cardName);
						}
					}
				} catch (IOException ex) {
					throw new ArcaneException("Error importing MTGO owned quantities.", ex);
				}
				updateCardTable();
			}
		});

		magicAlbumImportMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				JFileChooser fileChooser = new JFileChooser("decks");
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(FileUtil.chooserFileEndsWith("Magic Album (*.csv)", ".csv"));
				fileChooser.setDialogTitle("Import owned quantities");
				int result = fileChooser.showDialog(DeckBuilder.this, "Import");
				if (result != JFileChooser.APPROVE_OPTION) return;

				result = JOptionPane.showConfirmDialog(DeckBuilder.this, "All existing owned quantity values will be lost.",
					"Confirm Overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.OK_OPTION) return;
				for (Card card : arcane.getCards())
					card.ownedQty = 0;

				try {
					CSVReader reader = new CSVReader(new UnicodeReader(new FileInputStream(fileChooser.getSelectedFile()), "UTF-16LE"), "\t",
						"\"", true, true);
					while (true) {
						List<String> fields = reader.getFields();
						if (fields == null) break;
						if (fields.size() < 24) {
						    arcane.log("" + fields.size());
						    for(String c : fields)
							arcane.log(c);
						    continue; }
						String cardName = fields.get(1);
						cardName = cardName.replace("Æ", "AE");
						cardName = cardName.replace("û", "u");
						cardName = cardName.replace("â", "a");
						cardName = cardName.replace("ä", "a");
						cardName = cardName.replace("ô", "o");
						cardName = cardName.replace("î", "i");
						cardName = cardName.replace("’", "'");
						cardName = cardName.trim();
						if (cardName.equals("Name (Oracle)")) continue;
						int qty = Integer.parseInt(fields.get(5)) + Integer.parseInt(fields.get(6));
						String set = arcane.getMainSet(fields.get(0));
						String versionNumber = fields.get(3);

						try {
        						String version = "dummy";
        						for (Card card : arcane.getCards(cardName, set)) {
        							if (card.version.equals(versionNumber)) {
        								version = card.version;
        								break;
        							}
        						}
        						if ("dummy".equals(version)) {
        							arcane.logError("Unable to import owned quantities for card \"" + cardName
        								+ "\", unknown version for set \"" + set + "\": " + versionNumber);
        							continue;
        						}
        						Card card = arcane.getCard(cardName, set, version);
        						qty += card.ownedQty;
        						card.ownedQty = qty;
						} catch (ArcaneException ex) {
							arcane.log("Card not found in database while importing: " + cardName + ", set: " + set);
						}
					}
				} catch (IOException ex) {
					throw new ArcaneException("Error importing Magic Album owned quantities.", ex);
				}
				updateCardTable();
			}
		});

		setsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent evt) {
				if (evt.getValueIsAdjusting()) return;
				if (searchDisabled) return;
				searchDisabled = true;
				presetCombo.setSelectedItem(Format.custom);
				searchDisabled = false;
				updateCardTable();
				updateDeckCount();
			}
		});

		presetCombo.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				if (searchDisabled) return;
				searchDisabled = true;
				setsList.clearSelection();
				ListSelectionModel selectionModel = setsList.getSelectionModel();
				Set<String> sets = arcane.getFormatSets((Format)presetCombo.getSelectedItem());
				if (sets == null) {
					if (presetCombo.getSelectedIndex() == 0) selectionModel.addSelectionInterval(0, setsListModel.getSize() - 1);
				} else {
					for (String set : sets) {
						int index = setsListModel.getIndexOf(new SetEntry(set));
						selectionModel.addSelectionInterval(index, index);
					}
				}
				searchDisabled = false;
				updateCardTable();
				updateDeckCount();
			}
		});

		alwaysMatchEnglishMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				updateCardTable();
			}
		});

		linkTablesButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				List<Card> cards = deckTable.getSelectedCards();
				if (cards.size() == 0) cards = sideTable.getSelectedCards();
				if (cards.size() == 0) return;
				cardsTable.setSelectedCards(cards);
			}
		});
	}

	public boolean exit () {
		if (isDirty) {
			int result = JOptionPane.showConfirmDialog(DeckBuilder.this, "Unsaved changes will be lost.", "Confirm Exit",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result != JOptionPane.OK_OPTION) return false;
		}
		boolean exit = super.exit();
		if (exit) {
			instances.remove(DeckBuilder.this);
		}
		return exit;
	}

	protected void updateCardImageType () {
		if (prefs.cardImageType != CardImageType.none) {
			cardInfoSplit.setEnabled(true);
			cardImageGroup.setVisible(true);
		} else {
			cardInfoSplit.setEnabled(false);
			cardImageGroup.setVisible(false);
		}
		imageCache.clear();
		cardInfoSplit.revalidate();
	}

	protected JPanel getDeckInfoPanel () {
		final FontMetrics metrics = deckGroup.getFontMetrics(deckGroup.getFont());
		final int y = Math.max(0, metrics.getHeight() - 14);
		JPanel deckInfoPanel = new JPanel() {
			protected void paintComponent (Graphics g) {
				super.paintComponent(g);
				if (deckStats == null) return;
				g.drawImage(deckInfoImage, 0, y, null);
				drawCentered(g, deckStats.land, 22, 8 + y);
				drawCentered(g, deckStats.creatures, 68, 8 + y);
				drawCentered(g, deckStats.spells, 112, 8 + y);
				drawCentered(g, deckStats.white, 154, 8 + y);
				drawCentered(g, deckStats.blue, 174, 8 + y);
				drawCentered(g, deckStats.black, 194, 8 + y);
				drawCentered(g, deckStats.red, 214, 8 + y);
				drawCentered(g, deckStats.green, 234, 8 + y);
				drawCentered(g, deckStats.avg, 18, 43 + y);
				drawCentered(g, deckStats.zero, 54, 43 + y);
				drawCentered(g, deckStats.one, 74, 43 + y);
				drawCentered(g, deckStats.two, 94, 43 + y);
				drawCentered(g, deckStats.three, 114, 43 + y);
				drawCentered(g, deckStats.four, 134, 43 + y);
				drawCentered(g, deckStats.five, 154, 43 + y);
				drawCentered(g, deckStats.six, 174, 43 + y);
				drawCentered(g, deckStats.seven, 194, 43 + y);
				drawCentered(g, deckStats.eight, 214, 43 + y);
				drawCentered(g, deckStats.ninePlus, 234, 43 + y);
				if (!deckStats.illegalT1main && deckStats.illegalT1side)
					g.drawImage(cautionT1, 258, 0 + y, null);
				else if (deckStats.illegalT1main) {
					g.drawImage(illegalT1, 258, 0 + y, null);
				}
				if (!deckStats.illegalT15main && deckStats.illegalT15side)
					g.drawImage(cautionT15, 258, 16 + y, null);
				else if (deckStats.illegalT15main) {
					g.drawImage(illegalT15, 258, 16 + y, null);
				}
				if (!deckStats.illegalT1Xmain && deckStats.illegalT1Xside)
					g.drawImage(cautionT1X, 258, 32 + y, null);
				else if (deckStats.illegalT1Xmain) {
					g.drawImage(illegalT1X, 258, 32 + y, null);
				}
				if (!deckStats.illegalT2main && deckStats.illegalT2side)
					g.drawImage(cautionT2, 258, 48 + y, null);
				else if (deckStats.illegalT2main) {
					g.drawImage(illegalT2, 258, 48 + y, null);
				}
			}

			private void drawCentered (Graphics g, int value, int x, int y) {
				if (value == 0) return;
				drawCentered(g, String.valueOf(value), x, y);
			}

			private void drawCentered (Graphics g, String text, int x, int y) {
				g.drawString(text, x - metrics.stringWidth(text) / 2, y);
			}
		};
		deckInfoPanel.setPreferredSize(new Dimension(279, 65 + y));
		deckInfoPanel.setMinimumSize(new Dimension(279, 65 + y));
		deckInfoPanel.setMaximumSize(new Dimension(279, 65 + y));
		deckInfoPanel.setSize(new Dimension(279, 65 + y));
		return deckInfoPanel;
	}

	private void setCardCountMenuItem (JMenuItem menuItem, List<Card> selectedCards) {
		int total = 0;
		int price = 0;
		for (Card card : selectedCards) {
			total += 1;
			price += card.price * 100;
		}

		String priceString = String.valueOf(price / 100f);
		if (priceString.length() > 2 && priceString.charAt(priceString.length() - 2) == '.') priceString += "0";

		if (total == 1)
			menuItem.setText("1 card: $" + priceString);
		else {
			String uniqueMessage = "";
			if (menuItem != cardTableCardCountItem && total != selectedCards.size())
				uniqueMessage = " (" + selectedCards.size() + " unique)";

			menuItem.setText(total + " cards" + uniqueMessage + ": $" + priceString);
		}
	}

	static public void updateRows (String cardName) {
		for (DeckBuilder deckBuilder : instances) {
			for (int rowIndex : deckBuilder.cardsTable.model.getRowIndices(cardName))
				deckBuilder.cardsTable.model.fireTableRowsUpdated(rowIndex, rowIndex);
			for (int rowIndex : deckBuilder.deckTable.model.getRowIndices(cardName))
				deckBuilder.deckTable.model.fireTableRowsUpdated(rowIndex, rowIndex);
			for (int rowIndex : deckBuilder.sideTable.model.getRowIndices(cardName))
				deckBuilder.sideTable.model.fireTableRowsUpdated(rowIndex, rowIndex);
		}
	}

	static private enum Action {
		add, remove
	}

	static private class DeckAction {
		public final CardTable table;
		public final Action action;
		public final Map<Card, Integer> cardToCount = new HashMap<Card, Integer>();

		DeckAction (CardTable table, Action action) {
			this.table = table;
			this.action = action;
		}

		public void addCard (Card card) {
			Integer count = cardToCount.get(card);
			if (count == null)
				cardToCount.put(card, 1);
			else
				cardToCount.put(card, count + 1);
		}
	}

	private class DeckStats {
		public int deckCount;
		public int sideCount;
		public int land;
		public int creatures;
		public int spells;
		public int white;
		public int blue;
		public int black;
		public int red;
		public int green;
		public String avg = "";
		public int zero;
		public int one;
		public int two;
		public int three;
		public int four;
		public int five;
		public int six;
		public int seven;
		public int eight;
		public int ninePlus;
		public boolean illegalT1main;
		public boolean illegalT1side;
		public boolean illegalT15main;
		public boolean illegalT15side;
		public boolean illegalT1Xmain;
		public boolean illegalT1Xside;
		public boolean illegalT2main;
		public boolean illegalT2side;

		public int getCount (CardTable table) {
			if (table == deckTable) return deckCount;
			return sideCount;
		}
	}

	static private class SetEntry {
		public final String name;
		public final String set;

		public SetEntry (String set) {
			this.set = set;
			this.name = Arcane.getInstance().getSetName(set);
		}

		public String toString () {
			return name + " (" + set.toUpperCase() + ")";
		}

		public boolean equals (Object obj) {
			if (!(obj instanceof SetEntry)) return false;
			return set.equals(((SetEntry)obj).set);
		}
	}

	public static void main (String[] args) throws Exception {
		Arcane.setup("data/arcane.properties", "arcane.log", true);
		new DeckBuilder().setVisible(true);
	}
}
