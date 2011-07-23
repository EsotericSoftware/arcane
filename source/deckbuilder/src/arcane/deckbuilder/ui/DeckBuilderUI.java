
package arcane.deckbuilder.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;

import arcane.Arcane;
import arcane.Card;
import arcane.CardProperty;
import arcane.Format;
import arcane.rulesviewer.ui.RulesViewer;
import arcane.ui.ArcaneFrame;
import arcane.ui.CardInfoPane;
import arcane.ui.ScaledImagePanel;
import arcane.ui.ScaledImagePanel.ScalingType;
import arcane.ui.table.CardTable;
import arcane.ui.util.Button;
import arcane.ui.util.MenuItem;
import arcane.ui.util.Separator;
import arcane.ui.util.SplitPane;
import arcane.ui.util.SplitPane.SplitPaneType;
import arcane.ui.util.ToolBar;
import arcane.ui.util.UI;
import arcane.util.Util;

abstract class DeckBuilderUI extends ArcaneFrame {
	protected DefaultComboBoxModel setsListModel;
	protected CardTable deckTable;
	protected CardTable sideTable;

	public DeckBuilderUI () throws HeadlessException {
		setTitle("Deck Builder - Arcane v" + Arcane.version);
		setIconImage(UI.getImageIcon("/deckbuilder.png").getImage());
	}

	protected void initializeMenus () {
		super.initializeMenus();

		fileMenu.add(new JPopupMenu.Separator(), 0);
		{
			newWindowMenuItem = new JMenuItem(trans.get("menu.File.NewWindow", "New Window"), KeyEvent.VK_W);
			newWindowMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK));
			fileMenu.add(newWindowMenuItem, 0);
		}
		fileMenu.add(new JPopupMenu.Separator(), 0);
		{
			saveAsMenuItem = new JMenuItem(trans.get("menu.File.SaveAs", "Save As") + "...", KeyEvent.VK_A);
			saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
			fileMenu.add(saveAsMenuItem, 0);
		}
		{
			saveMenuItem = new JMenuItem(trans.get("menu.File.Save", "Save"), KeyEvent.VK_S);
			saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
			fileMenu.add(saveMenuItem, 0);
		}
		{
			openMenuItem = new JMenuItem(trans.get("menu.File.Open", "Open") + "...", KeyEvent.VK_O);
			openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
			fileMenu.add(openMenuItem, 0);
		}
		{
			newMenuItem = new JMenuItem(trans.get("menu.File.New", "New"), KeyEvent.VK_N);
			newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
			fileMenu.add(newMenuItem, 0);
		}
		{
			alwaysMatchEnglishMenuItem = new JCheckBoxMenuItem(trans.get("menu.Settings.AlwaysMatch", "Always match in English"));
			alwaysMatchEnglishMenuItem.setSelected(false);
			settingsMenu.add(alwaysMatchEnglishMenuItem, 6);
		}
		toolsMenu.add(new JPopupMenu.Separator());
		{
			JMenu importMenu = new JMenu(trans.get("menu.Tools.Import", "Import owned quantities"));
			toolsMenu.add(importMenu);
			{
				mtgoImportMenuItem = new JMenuItem("MTGO CSV");
				importMenu.add(mtgoImportMenuItem);
				magicAlbumImportMenuItem = new JMenuItem("Magic Album CSV");
				importMenu.add(magicAlbumImportMenuItem);
			}
		}
		{
			JMenu helpMenu = new JMenu();
			menuBar.add(helpMenu);
			helpMenu.setText(trans.get("menu.Help", "Help"));
			helpMenu.setMnemonic(KeyEvent.VK_H);
			{
				helpMenuItem = new JMenuItem(trans.get("menu.Help", "Help") + "...", KeyEvent.VK_H);
				helpMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
				helpMenu.add(helpMenuItem);
			}
		}
	}

	protected void initializeComponents () {
		GridBagLayout thisLayout = new GridBagLayout();
		getContentPane().setLayout(thisLayout);
		{
			JPanel searchGroup = new JPanel();
			getContentPane().add(
				searchGroup,
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 0, 0,
					6), 6, 0));
			GridBagLayout searchButtonsSectionLayout = new GridBagLayout();
			searchGroup.setLayout(searchButtonsSectionLayout);
			searchGroup.setBorder(BorderFactory.createTitledBorder(trans.get("ui.Search", "Search")));
			{
				JPanel searchButtonSection = new JPanel();
				searchGroup.add(searchButtonSection, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0));
				GridBagLayout searchGroupLayout = new GridBagLayout();
				searchButtonSection.setLayout(searchGroupLayout);
				{
					whiteButton = UI.getToggleButton();
					searchButtonSection.add(whiteButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					whiteButton.setText("white");
				}
				{
					blueButton = UI.getToggleButton();
					searchButtonSection.add(blueButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					blueButton.setText("blue");

				}
				{
					blackButton = UI.getToggleButton();
					searchButtonSection.add(blackButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					blackButton.setText("black");
				}
				{
					redButton = UI.getToggleButton();
					searchButtonSection.add(redButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					redButton.setText("red");

				}
				{
					greenButton = UI.getToggleButton();
					searchButtonSection.add(greenButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					greenButton.setText("green");
				}
				{
					colorlessButton = UI.getToggleButton();
					searchButtonSection.add(colorlessButton, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					colorlessButton.setText("colorless");
				}
				{
					multiColorButton = UI.getToggleButton();
					searchButtonSection.add(multiColorButton, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					multiColorButton.setText("multicolor");
				}
				{
					exactButton = UI.getToggleButton();
					searchButtonSection.add(exactButton, new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					exactButton.setText("exact");
				}
				{
					landButton = UI.getToggleButton();
					searchButtonSection.add(landButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					landButton.setText("land");
				}
				{
					artifactButton = UI.getToggleButton();
					searchButtonSection.add(artifactButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					artifactButton.setText("artifact");
				}
				{
					creatureButton = UI.getToggleButton();
					searchButtonSection.add(creatureButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					creatureButton.setText("creature");
				}
				{
					sorceryButton = UI.getToggleButton();
					searchButtonSection.add(sorceryButton, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					sorceryButton.setText("sorcery");
				}
				{
					instantButton = UI.getToggleButton();
					searchButtonSection.add(instantButton, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					instantButton.setText("instant");
				}
				{
					enchantButton = UI.getToggleButton();
					searchButtonSection.add(enchantButton, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					enchantButton.setText("enchant");
				}
				{
					auraButton = UI.getToggleButton();
					searchButtonSection.add(auraButton, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					auraButton.setText("aura");
				}
				{
					planeswalkerButton = UI.getToggleButton();
					searchButtonSection.add(planeswalkerButton, new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					planeswalkerButton.setText("planeswalker");
				}
				{
					titleButton = UI.getToggleButton();
					searchButtonSection.add(titleButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					titleButton.setText("title");
				}
				{
					textButton = UI.getToggleButton();
					searchButtonSection.add(textButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					textButton.setText("text");
				}
				{
					typeButton = UI.getToggleButton();
					searchButtonSection.add(typeButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					typeButton.setText("type");
				}
				{
					commonButton = UI.getToggleButton();
					searchButtonSection.add(commonButton, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					commonButton.setText("common");
				}
				{
					uncommonButton = UI.getToggleButton();
					searchButtonSection.add(uncommonButton, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					uncommonButton.setText("uncommon");
				}
				{
					rareButton = UI.getToggleButton();
					searchButtonSection.add(rareButton, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					rareButton.setText("rare");
				}
				{
					mythicrareButton = UI.getToggleButton();
					searchButtonSection.add(mythicrareButton, new GridBagConstraints(6, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					mythicrareButton.setText("mythic rare");
				}
				{
					uniqueOnlyButton = UI.getToggleButton();
					searchButtonSection.add(uniqueOnlyButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					uniqueOnlyButton.setText("unique");
				}
				{
					ownedOnlyButton = UI.getToggleButton();
					searchButtonSection.add(ownedOnlyButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
					ownedOnlyButton.setText("owned");
				}
				searchButtons = new JToggleButton[][] {
					{typeButton, textButton, titleButton}, //
					{commonButton, uncommonButton, rareButton, mythicrareButton}, //
					{planeswalkerButton, auraButton, enchantButton, instantButton, sorceryButton, creatureButton, artifactButton,
						landButton}, //
					{colorlessButton, greenButton, redButton, blackButton, blueButton, whiteButton}};
			}
			{
				searchCombo = new JComboBox();
				searchCombo.setEditable(true);
				searchComboModel = new DefaultComboBoxModel();
				searchCombo.setModel(searchComboModel);
				searchGroup.add(searchCombo, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL, new Insets(6, 4, 5, 6), 0, 0));
			}
			{
				searchButton = UI.getButton();
				searchGroup.add(searchButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				searchButton.setText("Search");
			}
			{
				resetButton = UI.getButton();
				searchGroup.add(resetButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, new Insets(0, 6, 0, 6), 0, 0));
				resetButton.setText("Reset");
			}
		}
		{
			SplitPane bottomLeftSplit = new SplitPane(SplitPane.HORIZONTAL_SPLIT);
			getContentPane().add(
				bottomLeftSplit,
				new GridBagConstraints(0, 1, 3, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 6, 6,
					6), 0, 0));
			{
				cardInfoSplit = new SplitPane(SplitPane.VERTICAL_SPLIT);
				bottomLeftSplit.add(cardInfoSplit, SplitPane.LEFT);
				{
					cardImageGroup = new JPanel();
					GridBagLayout cardImageGroupLayout = new GridBagLayout();
					cardImageGroup.setLayout(cardImageGroupLayout);
					cardInfoSplit.add(cardImageGroup, SplitPane.TOP);
					cardImageGroup.setBorder(BorderFactory.createTitledBorder("Card Image"));
					{
						cardImagePanel = new ScaledImagePanel();
						cardImagePanel.setScalingBlur(true);
						cardImagePanel.setScalingType(ScalingType.bicubic);
						cardImageGroup.add(cardImagePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
							GridBagConstraints.BOTH, new Insets(0, 4, 4, 4), 0, 0));
					}
				}
				{
					JPanel cardInfoGroup = new JPanel();
					GridBagLayout cardInfoGroupLayout = new GridBagLayout();
					cardInfoGroup.setLayout(cardInfoGroupLayout);
					cardInfoSplit.add(cardInfoGroup, SplitPane.BOTTOM);
					cardInfoGroup.setBorder(BorderFactory.createTitledBorder("Card Information"));
					{
						JScrollPane cardInfoScrollPane = new JScrollPane();
						cardInfoGroup.add(cardInfoScrollPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
							GridBagConstraints.BOTH, new Insets(0, 4, 4, 4), 0, 0));
						cardInfoScrollPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
						{
							cardInfoPane = new CardInfoPane() {
								protected void cardShown (Card card) {
									DeckBuilderUI.this.loadCardImage(card);
								}

								protected void showRule (String rule) {
									new RulesViewer().showRule(rule);
								}
							};
							cardInfoScrollPane.setViewportView(cardInfoPane);
						}
					}
				}
			}
			{
				SplitPane bottomRightSplit = new SplitPane(SplitPane.HORIZONTAL_SPLIT);
				bottomRightSplit.setType(SplitPaneType.percentage);
				bottomLeftSplit.add(bottomRightSplit, SplitPane.RIGHT);
				{
					cardsGroup = new JPanel();
					bottomRightSplit.add(cardsGroup, SplitPane.LEFT);
					GridBagLayout cardsGroupLayout = new GridBagLayout();
					cardsGroupLayout.rowWeights = new double[] {0.0};
					cardsGroupLayout.rowHeights = new int[] {};
					cardsGroupLayout.columnWeights = new double[] {0.1};
					cardsGroupLayout.columnWidths = new int[] {0};
					cardsGroup.setLayout(cardsGroupLayout);
					cardsGroup.setBorder(BorderFactory.createTitledBorder("Cards (0)"));
					{
						quickSearchText = new JTextField();
						cardsGroup.add(quickSearchText, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
							GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0));
					}
					{
						ToolBar cardsToolbar = new ToolBar();
						cardsToolbar.setFloatable(false);
						cardsToolbar.setRollover(true);
						if (!Util.isMac) cardsToolbar.setMargin(new Insets(0, 0, 0, 0));
						cardsGroup.add(cardsToolbar, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
							GridBagConstraints.BOTH, new Insets(0, 0, 0, 4), 0, 0));
						{
							quickSearchButton = new Button();
							quickSearchButton.setIcon(UI.getImageIcon("/buttons/find.png"));
							quickSearchButton.setToolTipText("Next result");
							if (!Util.isMac) quickSearchButton.setMargin(new Insets(0, 0, 0, 0));
							cardsToolbar.add(quickSearchButton);
						}
						cardsToolbar.add(new Separator());
						{
							addCardButton = new Button();
							addCardButton.setIcon(UI.getImageIcon("/buttons/add.png"));
							addCardButton.setToolTipText("Add to deck");
							if (!Util.isMac) addCardButton.setMargin(new Insets(0, 0, 0, 0));
							cardsToolbar.add(addCardButton);
						}
						{
							addSideCardButton = new Button();
							addSideCardButton.setIcon(UI.getImageIcon("/buttons/addSide.png"));
							addSideCardButton.setToolTipText("Add to sideboard");
							if (!Util.isMac) addSideCardButton.setMargin(new Insets(0, 0, 0, 0));
							cardsToolbar.add(addSideCardButton);
						}
					}
					{
						JScrollPane cardsScrollPane = new JScrollPane();
						cardsGroup.add(cardsScrollPane, new GridBagConstraints(0, 1, 2, 1, 0.0, 1.0, GridBagConstraints.CENTER,
							GridBagConstraints.BOTH, new Insets(0, 4, 4, 4), 0, 0));
						{
							cardsTable = new CardTable(false, CardProperty.qty);
							cardsTable.allowUnsorted = false;
							cardsScrollPane.setViewportView(cardsTable);
						}
					}
				}
				{
					JPanel deckSection = new JPanel();
					GridBagLayout deckSectionLayout = new GridBagLayout();
					deckSectionLayout.rowWeights = new double[] {0.0};
					deckSectionLayout.rowHeights = new int[] {0};
					deckSectionLayout.columnWeights = new double[] {0.1};
					deckSectionLayout.columnWidths = new int[] {0};
					deckSection.setLayout(deckSectionLayout);
					bottomRightSplit.add(deckSection, SplitPane.RIGHT);
					{
						deckGroup = new JPanel();
						deckSection.add(deckGroup, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER,
							GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
						GridBagLayout deckGroupLayout = new GridBagLayout();
						deckGroupLayout.rowWeights = new double[] {0.0};
						deckGroupLayout.rowHeights = new int[] {0};
						deckGroupLayout.columnWeights = new double[] {0.1};
						deckGroupLayout.columnWidths = new int[] {0};
						deckGroup.setLayout(deckGroupLayout);
						deckGroup.setBorder(BorderFactory.createTitledBorder("Deck (0/0)"));
						{
							ToolBar deckToolbar = new ToolBar();
							deckToolbar.setFloatable(false);
							deckToolbar.setRollover(true);
							if (!Util.isMac) deckToolbar.setMargin(new Insets(0, 0, 0, 0));
							deckGroup.add(deckToolbar, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
								GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0));
							{
								addDeckButton = new Button();
								addDeckButton.setIcon(UI.getImageIcon("/buttons/plus.png"));
								addDeckButton.setToolTipText("Increment");
								if (!Util.isMac) addDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(addDeckButton);
							}
							{
								removeDeckButton = new Button();
								removeDeckButton.setIcon(UI.getImageIcon("/buttons/minus.png"));
								removeDeckButton.setToolTipText("Decrement");
								if (!Util.isMac) removeDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(removeDeckButton);
							}
							{
								swapDeckButton = new Button();
								swapDeckButton.setIcon(UI.getImageIcon("/buttons/swap.png"));
								swapDeckButton.setToolTipText("Move to deck/sideboard");
								if (!Util.isMac) swapDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(swapDeckButton);
							}
							deckToolbar.add(new Separator());
							{
								upDeckButton = new Button();
								upDeckButton.setIcon(UI.getImageIcon("/buttons/up.png"));
								upDeckButton.setToolTipText("Move up");
								if (!Util.isMac) upDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(upDeckButton);
							}
							{
								downDeckButton = new Button();
								downDeckButton.setIcon(UI.getImageIcon("/buttons/down.png"));
								downDeckButton.setToolTipText("Move down");
								if (!Util.isMac) downDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(downDeckButton);
							}
							{
								reorderDeckButton = new JButton();
								reorderDeckButton.setIcon(UI.getImageIcon("/buttons/reorder.png"));
								reorderDeckButton.setToolTipText("Auto order");
								if (!Util.isMac) reorderDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(reorderDeckButton);
							}
							{
								unsortDeckButton = new Button();
								unsortDeckButton.setIcon(UI.getImageIcon("/buttons/unsort.png"));
								unsortDeckButton.setToolTipText("Remove sorting");
								if (!Util.isMac) unsortDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(unsortDeckButton);
							}
							deckToolbar.add(new Separator());
							{
								undoDeckButton = new Button();
								undoDeckButton.setIcon(UI.getImageIcon("/buttons/undo.png"));
								undoDeckButton.setToolTipText("Undo");
								if (!Util.isMac) undoDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(undoDeckButton);
							}
							{
								redoDeckButton = new Button();
								redoDeckButton.setIcon(UI.getImageIcon("/buttons/redo.png"));
								redoDeckButton.setToolTipText("Redo");
								if (!Util.isMac) redoDeckButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(redoDeckButton);
							}
							deckToolbar.add(new Separator());
							{
								linkTablesButton = new JToggleButton();
								linkTablesButton.setIcon(UI.getImageIcon("/buttons/check.png"));
								linkTablesButton.setToolTipText("Link selections with card pool");
								if (!Util.isMac) linkTablesButton.setMargin(new Insets(0, 0, 0, 0));
								deckToolbar.add(linkTablesButton);
							}
						}
						{
							SplitPane deckSplitter = new SplitPane(SplitPane.VERTICAL_SPLIT);
							deckSplitter.setType(SplitPaneType.percentage);
							deckGroup.add(deckSplitter, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER,
								GridBagConstraints.BOTH, new Insets(0, 4, 4, 4), 0, 0));
							{
								JScrollPane deckScrollPane = new JScrollPane();
								deckSplitter.add(deckScrollPane, SplitPane.TOP);
								{
									deckTable = new CardTable(true);
									deckScrollPane.setViewportView(deckTable);
								}
							}
							{
								JScrollPane sideScrollPane = new JScrollPane();
								deckSplitter.add(sideScrollPane, SplitPane.BOTTOM);
								{
									sideTable = new CardTable(true);
									sideScrollPane.setViewportView(sideTable);
									deckTable.copyTable(sideTable);
									sideTable.copyTable(deckTable);
								}
							}
						}
					}
					{
						JPanel deckInfoGroup = new JPanel();
						GridBagLayout deckInfoGroupLayout = new GridBagLayout();
						deckInfoGroupLayout.rowWeights = new double[] {0.0};
						deckInfoGroupLayout.rowHeights = new int[] {0};
						deckInfoGroupLayout.columnWeights = new double[] {0.1};
						deckInfoGroupLayout.columnWidths = new int[] {0};
						deckInfoGroup.setLayout(deckInfoGroupLayout);
						deckSection.add(deckInfoGroup, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
							GridBagConstraints.BOTH, new Insets(1, 0, 0, 0), 0, 0));
						deckInfoGroup.setBorder(BorderFactory.createTitledBorder("Deck Information"));
						{
							deckInfoPanel = getDeckInfoPanel();
							deckInfoGroup.add(deckInfoPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
								GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						}
					}
				}
			}
		}
		{
			JPanel setsGroup = new JPanel();
			GridBagLayout setsGroupLayout = new GridBagLayout();
			setsGroup.setLayout(setsGroupLayout);
			getContentPane().add(
				setsGroup,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 6, 0,
					5), 0, 0));
			setsGroup.setBorder(BorderFactory.createTitledBorder("Sets"));
			{
				JLabel presetLabel = new JLabel();
				setsGroup.add(presetLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, new Insets(0, 4, 4, 6), 0, 0));
				presetLabel.setText("Preset:");
			}
			{
				JScrollPane setsScrollPane = new JScrollPane();
				setsGroup.add(setsScrollPane, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER,
					GridBagConstraints.BOTH, new Insets(0, 4, 6, 4), 220, 0));
				{
					setsListModel = new DefaultComboBoxModel();
					setsList = new JList();
					// setsList.setUI(new CheckListUI());
					setsScrollPane.setViewportView(setsList);
					setsList.setModel(setsListModel);
				}
			}
			{
				ComboBoxModel presetComboModel = new DefaultComboBoxModel(new Format[] { //
					Format.all, Format.vintage, Format.legacy, Format.extended, Format.standard, Format.modern, Format.custom});
				presetCombo = new JComboBox();
				setsGroup.add(presetCombo, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(0, 0, 4, 4), 18, 0));
				presetCombo.setModel(presetComboModel);
			}
		}
		{
			getContentPane().add(
				new JPanel(),
				new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0,
					0), 0, 0));
		}
	}

	protected abstract void loadCardImage (Card card);

	protected abstract JPanel getDeckInfoPanel ();

	protected JPanel deckGroup;
	protected JPanel cardsGroup;
	protected JButton searchButton;
	protected JComboBox presetCombo;
	protected JButton resetButton;
	protected CardInfoPane cardInfoPane;
	protected JToggleButton commonButton;
	protected JToggleButton uncommonButton;
	protected JToggleButton rareButton;
	protected JToggleButton mythicrareButton;
	protected JToggleButton typeButton;
	protected JToggleButton textButton;
	protected JToggleButton titleButton;
	protected JToggleButton auraButton;
	protected JToggleButton planeswalkerButton;
	protected JToggleButton enchantButton;
	protected JToggleButton instantButton;
	protected JToggleButton sorceryButton;
	protected JToggleButton creatureButton;
	protected JToggleButton artifactButton;
	protected JToggleButton landButton;
	protected JToggleButton exactButton;
	protected JToggleButton multiColorButton;
	protected CardTable cardsTable;
	protected JComboBox searchCombo;
	protected DefaultComboBoxModel searchComboModel;
	protected JToggleButton colorlessButton;
	protected JToggleButton greenButton;
	protected JToggleButton redButton;
	protected JToggleButton blackButton;
	protected JToggleButton blueButton;
	protected JToggleButton whiteButton;
	protected JToggleButton uniqueOnlyButton;
	protected JToggleButton ownedOnlyButton;
	protected JToggleButton[][] searchButtons;
	protected JMenuItem saveAsMenuItem;
	protected JMenuItem saveMenuItem;
	protected JMenuItem openMenuItem;
	protected JMenuItem newMenuItem;
	protected JList setsList;
	protected JMenuItem helpMenuItem;
	protected JCheckBoxMenuItem alwaysMatchEnglishMenuItem;
	protected JMenuItem newWindowMenuItem;
	protected JMenuItem mtgoImportMenuItem;
	protected JMenuItem magicAlbumImportMenuItem;
	protected JMenu pluginsMenu;
	protected Button addCardButton;
	protected Button addSideCardButton;
	protected Button addDeckButton;
	protected Button removeDeckButton;
	protected Button upDeckButton;
	protected Button downDeckButton;
	protected Button undoDeckButton;
	protected Button redoDeckButton;
	protected JButton reorderDeckButton;
	protected Button swapDeckButton;
	protected Button quickSearchButton;
	protected JButton unsortDeckButton;
	protected JToggleButton linkTablesButton;
	protected JTextField quickSearchText;
	protected JPopupMenu cardTableCardMenu;
	protected JPopupMenu deckTableCardMenu;
	protected JPopupMenu sideTableCardMenu;
	protected JPanel cardImageGroup;
	protected ScaledImagePanel cardImagePanel;
	protected MenuItem cardTableCardCountItem;
	protected MenuItem deckTableCardCountItem;
	protected MenuItem sideTableCardCountItem;
	protected JPanel deckInfoPanel;
	protected SplitPane cardInfoSplit;
}
