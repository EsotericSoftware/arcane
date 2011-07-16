
package arcane.rulesviewer.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.HyperlinkEvent.EventType;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.ArcanePreferences;
import arcane.RulesDataStore.RulesDataStoreConnection;
import arcane.ui.ArcaneFrame;
import arcane.ui.util.Separator;
import arcane.ui.util.SplitPane;
import arcane.ui.util.ToolBar;
import arcane.ui.util.UI;
import arcane.util.CSVReader;
import arcane.util.Util;

public class RulesViewer extends ArcaneFrame {
	private Arcane arcane = Arcane.getInstance();
	private ArcanePreferences prefs = arcane.getPrefs();
	private DefaultComboBoxModel resultsListModel;
	private long lastRefreshNumber;
	private LinkedList<RuleEntry> history = new LinkedList<RuleEntry>();
	private int historyIndex = -1;
	private boolean isHistoryClick;

	public RulesViewer () {
		setTitle("Rules Viewer - Arcane v" + Arcane.version);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(UI.getImageIcon("/rulesviewer.png").getImage());
		initializeComponents();
		initializeMenus();
		loadPreferences();
		updatedButtons();
		updateSearch();
	}

	private void loadPreferences () {
		SplitPane.setScrollPaneInfo(getContentPane(), prefs.get("rulesviewer.splitpanes", "211,"));

		prefs.loadFrameState(this, "rulesviewer", 640, 480);
	}

	protected void savePreferences () {
		prefs.set("rulesviewer.splitpanes", SplitPane.getScrollPaneInfo(getContentPane()));

		prefs.saveFrameState(this, "rulesviewer");
	}

	protected void initializeMenus () {
		initializeMenuBar();
		initializeFileMenu();
		initializeSettingsMenu();
		initializeToolsMenu();

		fileMenu.add(new JPopupMenu.Separator(), 0);
		{
			JMenuItem menuItem = new JMenuItem("New Window", KeyEvent.VK_W);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK));
			fileMenu.add(menuItem, 0);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					new RulesViewer().showRule(searchText.getText());
				}
			});
		}
	}

	public void showAllRules () {
		setVisible(true);
		if (getExtendedState() == JFrame.ICONIFIED) setExtendedState(JFrame.NORMAL);
		searchText.setText("");
		updateSearch();
	}

	public void showRule (String rule) {
		setVisible(true);
		if (getExtendedState() == JFrame.ICONIFIED) setExtendedState(JFrame.NORMAL);
		searchText.setText(rule);
		updateSearch();
	}

	public void ruleSelected (final RuleEntry entry) {
		if (!isHistoryClick) {
			RuleEntry oldCurrentEntry = historyIndex == -1 ? null : history.get(historyIndex);
			if (!entry.equals(oldCurrentEntry)) {
				// Remove all to the old current rule.
				while (!history.isEmpty()) {
					if (history.getLast() == oldCurrentEntry) break;
					history.removeLast();
				}
				history.addLast(entry);
				historyIndex = history.size() - 1;
			}
		} else
			isHistoryClick = false;
		updatedButtons();
		Util.threadPool.submit(new Runnable() {
			public void run () {
				final StringBuffer buffer = new StringBuffer(1024);
				try {
					RulesDataStoreConnection conn = arcane.getRulesDataStoreConnection();
					buffer.append("<b>");
					buffer.append(entry);
					buffer.append("</b><br><br>");
					for (String[] subrule : conn.getSubrules(entry.rule)) {
						buffer.append("<b>");
						if (entry.subrules.contains(subrule[0])) {
							for (String keyword : entry.keywords) {
								subrule[1] = subrule[1].replaceAll("([^'])" + keyword, "$1<span style='background-color:FFFFBE'>"
									+ keyword + "</span>");
							}
							buffer.append("<span style='background-color:FFFFBE'>");
							buffer.append(subrule[0]);
							buffer.append("</span>");
						} else
							buffer.append(subrule[0]);
						buffer.append("</b> - ");
						buffer.append(subrule[1]);
						buffer.append("<br><br>");
					}
				} catch (SQLException ex) {
					throw new RuntimeException("Error loading subrules.", ex);
				}

				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						rulesEditorPane.setText("<html><body style='font-family:tahoma;font-size:11pt;margin:4px 4px 0px 5px'>"
							+ buffer + "<br></body></html>");
						rulesEditorPane.setCaretPosition(0);
					}
				});
			}
		});
	}

	private void updatedButtons () {
		backButton.setEnabled(historyIndex > 0);
		forwardButton.setEnabled(historyIndex < history.size() - 1);
	}

	private void updateSearch () {
		lastRefreshNumber = System.currentTimeMillis();
		final long currentRefreshNumber = lastRefreshNumber;
		Util.threadPool.submit(new Runnable() {
			public void run () {
				if (currentRefreshNumber != lastRefreshNumber) return;

				String input = searchText.getText().trim();
				UI.setTitle(resultsGroup, "Results (...)");
				resultsListModel.removeAllElements();

				// Build where clause.
				StringBuffer where = new StringBuffer(64);
				List<String> fields = null;
				try {
					CSVReader reader = new CSVReader(new StringReader(input), " ", "\"", true, false);
					fields = reader.getFields();
					if (fields != null) {
						for (String field : fields) {
							where.append(" AND text LIKE '%");
							where.append(field.replace("'", "''"));
							where.append("%'");
						}
					}
					reader.close();
				} catch (IOException ex) {
					throw new ArcaneException("Error parsing search text.", ex);
				}
				if (fields == null) fields = new ArrayList<String>(0);

				// Execute query.
				final List<RuleEntry> ruleEntries = new ArrayList<RuleEntry>();
				List<String> subrules = new ArrayList<String>(1);
				try {
					RulesDataStoreConnection conn = arcane.getRulesDataStoreConnection();
					if (input.length() == 0) {
						// Show all rules.
						for (String[] rule : conn.getRules())
							ruleEntries.add(new RuleEntry(input, rule[0], subrules, rule[1], fields));
					} else {
						// Try to look up a specific subrule or rule.
						String rule = conn.getSubruleRule(input);
						if (rule != null)
							subrules.add(input);
						else
							rule = input;
						String ruleText = conn.getRuleText(rule);
						if (ruleText != null)
							ruleEntries.add(new RuleEntry(input, rule, subrules, ruleText, fields));
						else {
							// Do a search.
							String sql = "SELECT rule, subrule FROM :table: WHERE 1=1" + where + " ORDER BY rule";
							PreparedStatement statement = conn.prepareStatement(sql);
							if (currentRefreshNumber != lastRefreshNumber) return;
							ResultSet set = statement.executeQuery();
							RuleEntry currentEntry = null;
							while (set.next()) {
								rule = set.getString(1);
								if (currentEntry == null || !rule.equals(currentEntry.rule)) {
									currentEntry = new RuleEntry(input, rule, new ArrayList<String>(), conn.getRuleText(rule), fields);
									ruleEntries.add(currentEntry);
								}
								currentEntry.subrules.add(set.getString(2));
							}
							set.close();
							statement.close();
						}
					}
				} catch (SQLException ex) {
					throw new ArcaneException("Error performing rules search.", ex);
				}

				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						if (currentRefreshNumber != lastRefreshNumber) return;
						UI.setTitle(resultsGroup, "Results (" + ruleEntries.size() + ")");
						for (RuleEntry entry : ruleEntries)
							resultsListModel.addElement(entry);

						if (isHistoryClick)
							resultsList.setSelectedValue(history.get(historyIndex), true);
						else if (ruleEntries.size() > 0) {
							resultsList.setSelectedIndex(0);
						}
					}
				});
			}
		});
	}

	private void initializeComponents () {
		setSize(640, 480);
		setLayout(new GridBagLayout());
		{
			splitter = new SplitPane(SplitPane.HORIZONTAL_SPLIT);
			this.getContentPane().add(
				splitter,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 6, 6,
					6), 0, 0));
			{
				JPanel searchSection = new JPanel();
				splitter.add(searchSection, SplitPane.LEFT);
				GridBagLayout searchSectionLayout = new GridBagLayout();
				searchSection.setLayout(searchSectionLayout);
				{
					JPanel searchGroup = new JPanel();
					searchGroup.setOpaque(false);
					GridBagLayout searchGroupLayout = new GridBagLayout();
					searchGroup.setLayout(searchGroupLayout);
					searchSection.add(searchGroup, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					searchGroup.setEnabled(false);
					UI.setTitle(searchGroup, "Search");
					ActionListener updateSearch = new ActionListener() {
						public void actionPerformed (ActionEvent evt) {
							updateSearch();
						}
					};
					{
						searchText = new JTextField();
						searchGroup.add(searchText, new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
							GridBagConstraints.HORIZONTAL, new Insets(2, 4, 4, 0), 0, 0));
						searchText.addActionListener(updateSearch);
					}
					{
						JButton searchButton = UI.getButton();
						searchGroup.add(searchButton, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
							GridBagConstraints.NONE, new Insets(0, 6, 0, 4), 0, 0));
						searchButton.setText("Search");
						searchButton.addActionListener(updateSearch);
					}
				}
				{
					resultsGroup = new JPanel();
					GridBagLayout jPanel1Layout = new GridBagLayout();
					resultsGroup.setLayout(jPanel1Layout);
					searchSection.add(resultsGroup, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));
					UI.setTitle(resultsGroup, "Results (0)");
					{
						resultsScrollPane = new JScrollPane();
						resultsGroup.add(resultsScrollPane, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
							GridBagConstraints.BOTH, new Insets(0, 4, 4, 4), 0, 0));
						{
							resultsListModel = new DefaultComboBoxModel();
							resultsList = new JList();
							resultsScrollPane.setViewportView(resultsList);
							resultsList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
							resultsList.setModel(resultsListModel);
							resultsList.addListSelectionListener(new ListSelectionListener() {
								public void valueChanged (ListSelectionEvent evt) {
									int selectedIndex = resultsList.getSelectedIndex();
									if (selectedIndex == -1) return;
									ruleSelected((RuleEntry)resultsListModel.getElementAt(selectedIndex));
								}
							});
						}
					}
				}
			}
			{
				JPanel rulesGroup = new JPanel();
				GridBagLayout resultsGroupLayout = new GridBagLayout();
				rulesGroup.setLayout(resultsGroupLayout);
				splitter.add(rulesGroup, SplitPane.RIGHT);
				UI.setTitle(rulesGroup, "Rules");
				{
					rulesScrollPane = new JScrollPane();
					rulesGroup.add(rulesScrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(0, 4, 4, 4), 0, 0));
					{
						ToolBar toolbar = new ToolBar();
						toolbar.setFloatable(false);
						toolbar.setRollover(true);
						if (!Util.isMac) toolbar.setMargin(new Insets(0, 0, 0, 0));
						rulesGroup.add(toolbar, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
							GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0));
						{
							backButton = new JButton();
							backButton.setToolTipText("Back");
							backButton.setIcon(UI.getImageIcon("/buttons/back.png"));
							if (!Util.isMac) backButton.setMargin(new Insets(0, 0, 0, 0));
							toolbar.add(backButton);
							backButton.addActionListener(new ActionListener() {
								public void actionPerformed (ActionEvent evt) {
									if (historyIndex == 0) return;
									historyIndex--;
									RuleEntry entry = history.get(historyIndex);
									isHistoryClick = true;
									if (searchText.getText().equals(entry.input))
										resultsList.setSelectedValue(entry, true);
									else {
										searchText.setText(entry.input);
										updateSearch();
									}
								}
							});
						}
						{
							forwardButton = new JButton();
							forwardButton.setToolTipText("Forward");
							forwardButton.setIcon(UI.getImageIcon("/buttons/add.png"));
							if (!Util.isMac) forwardButton.setMargin(new Insets(0, 0, 0, 0));
							toolbar.add(forwardButton);
							forwardButton.addActionListener(new ActionListener() {
								public void actionPerformed (ActionEvent evt) {
									if (historyIndex == history.size() - 1) return;
									historyIndex++;
									RuleEntry entry = history.get(historyIndex);
									isHistoryClick = true;
									if (searchText.getText().equals(entry.input))
										resultsList.setSelectedValue(entry, true);
									else {
										searchText.setText(entry.input);
										updateSearch();
									}
								}
							});
						}
						toolbar.add(new Separator());
						{
							JButton homeButton = new JButton();
							homeButton.setToolTipText("Home");
							homeButton.setIcon(UI.getImageIcon("/buttons/home.png"));
							if (!Util.isMac) homeButton.setMargin(new Insets(0, 0, 0, 0));
							toolbar.add(homeButton);
							homeButton.addActionListener(new ActionListener() {
								public void actionPerformed (ActionEvent evt) {
									searchText.setText("");
									updateSearch();
								}
							});
						}
					}
					{
						rulesEditorPane = new JEditorPane();
						rulesEditorPane.setBackground(UIManager.getColor("EditorPane.background"));
						UI.setHTMLEditorKit(rulesEditorPane);
						// rulesEditorPane.setContentType("text/plain");
						rulesEditorPane.setEditable(false);
						rulesEditorPane.addHyperlinkListener(new HyperlinkListener() {
							public void hyperlinkUpdate (HyperlinkEvent evt) {
								if (evt.getEventType() != EventType.ACTIVATED) return;
								showRule(evt.getDescription());
							}
						});
						rulesScrollPane.setViewportView(rulesEditorPane);
					}
				}
			}
		}
	}

	private JPanel resultsGroup;
	private JScrollPane rulesScrollPane;
	private JEditorPane rulesEditorPane;
	private JScrollPane resultsScrollPane;
	private JList resultsList;
	private JTextField searchText;
	private SplitPane splitter;
	private JButton backButton;
	private JButton forwardButton;

	static private class RuleEntry {
		public final String input;
		public final String rule;
		public final List<String> subrules;
		public final String text;
		public final List<String> keywords;

		public RuleEntry (String input, String rule, List<String> subrules, String text, List<String> keywords) {
			this.input = input;
			this.rule = rule;
			this.subrules = subrules;
			this.text = text;
			this.keywords = keywords;
		}

		public String toString () {
			return rule + " - " + text;
		}

		public boolean equals (Object obj) {
			if (obj == null) return false;
			if (!(obj instanceof RuleEntry)) throw new IllegalArgumentException("obj must be a RuleEntry.");
			return ((RuleEntry)obj).rule.equals(rule);
		}
	}

	public static void main (String[] args) throws Exception {
		Arcane.setup("data/arcane.properties", "arcane.log", true);
		new RulesViewer().setVisible(true);
	}
}
