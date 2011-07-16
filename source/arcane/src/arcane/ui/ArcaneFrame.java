
package arcane.ui;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.ArcanePreferences;
import arcane.ArcanePreferences.CardFontFamily;
import arcane.ArcanePreferences.CardFontSize;
import arcane.ArcanePreferences.CardImageType;
import arcane.ArcaneTranslation;
import arcane.SavePreferencesListener;
import arcane.util.Util;

public class ArcaneFrame extends JFrame {
	protected Arcane arcane = Arcane.getInstance();
	protected ArcanePreferences prefs = arcane.getPrefs();
	protected ArcaneTranslation trans = arcane.getTrans();
	protected JMenuBar menuBar;
	protected JMenu fileMenu;
	protected JMenu viewMenu;
	protected JMenu settingsMenu;
	protected JMenu toolsMenu;

	public ArcaneFrame () throws HeadlessException {
		prefs.addSaveListener(new SavePreferencesListener() {
			public void savePreferences () {
				ArcaneFrame.this.savePreferences();
			}
		});
	}

	protected void initializeMenus () {
		initializeMenuBar();
		initializeFileMenu();
		initializeViewMenu();
		initializeSettingsMenu();
		initializeToolsMenu();
	}

	protected void initializeMenuBar () {
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
	}

	protected void initializeFileMenu () {
		fileMenu = new JMenu();
		menuBar.add(fileMenu);
		fileMenu.setText(trans.get("menu.File", "File"));
		fileMenu.setMnemonic(KeyEvent.VK_F);
		{
			JMenuItem menuItem = new JMenuItem(trans.get("menu.File.Exit", "Exit"), KeyEvent.VK_X);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK));
			fileMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					exit();
				}
			});
		}
	}

	protected void initializeViewMenu () {
		viewMenu = new JMenu();
		menuBar.add(viewMenu);
		viewMenu.setText(trans.get("menu.View", "View"));
		viewMenu.setMnemonic(KeyEvent.VK_V);
		{
			JMenu fontSizeMenu = new JMenu(trans.get("menu.View.CardTextSize", "Card text size"));
			viewMenu.add(fontSizeMenu);
			ButtonGroup group = new ButtonGroup();
			{
				JMenuItem menuItem = new JRadioButtonMenuItem(trans.get("menu.View.CardTextSize.Small", "Small"));
				group.add(menuItem);
				fontSizeMenu.add(menuItem);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.fontSize == CardFontSize.small;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) prefs.fontSize = CardFontSize.small;
					}
				});
			}
			{
				JMenuItem menuItem = new JRadioButtonMenuItem(trans.get("menu.View.CardTextSize.Medium", "Medium"));
				group.add(menuItem);
				fontSizeMenu.add(menuItem);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.fontSize == CardFontSize.medium;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) prefs.fontSize = CardFontSize.medium;
					}
				});
			}
			{
				JMenuItem menuItem = new JRadioButtonMenuItem(trans.get("menu.View.CardTextSize.Large", "Large"));
				group.add(menuItem);
				fontSizeMenu.add(menuItem);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.fontSize == CardFontSize.large;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) prefs.fontSize = CardFontSize.large;
					}
				});
			}
		}
		{
			JMenu fontFamilyMenu = new JMenu(trans.get("menu.View.CardTextFont", "Card text font"));
			viewMenu.add(fontFamilyMenu);
			ButtonGroup group = new ButtonGroup();
			{
				JMenuItem menuItem = new JRadioButtonMenuItem("Arial");
				group.add(menuItem);
				fontFamilyMenu.add(menuItem);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.fontFamily == CardFontFamily.arial;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) prefs.fontFamily = CardFontFamily.arial;
					}
				});
			}
			{
				JMenuItem menuItem = new JRadioButtonMenuItem("Tahoma");
				group.add(menuItem);
				fontFamilyMenu.add(menuItem);
				menuItem.setSelected(true);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.fontFamily == CardFontFamily.tahoma;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) prefs.fontFamily = CardFontFamily.tahoma;
					}
				});
			}
			{
				JMenuItem menuItem = new JRadioButtonMenuItem("Verdana");
				group.add(menuItem);
				fontFamilyMenu.add(menuItem);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.fontFamily == CardFontFamily.verdana;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) prefs.fontFamily = CardFontFamily.verdana;
					}
				});
			}
		}
		viewMenu.addSeparator();
		{
			JMenu cardDisplayMenu = new JMenu(trans.get("menu.View.CardImageDisplay", "Card image display"));
			viewMenu.add(cardDisplayMenu);
			ButtonGroup group = new ButtonGroup();
			{
				JMenuItem menuItem = new JRadioButtonMenuItem(trans.get("menu.View.CardImageDisplay.None", "None"));
				group.add(menuItem);
				cardDisplayMenu.add(menuItem);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.cardImageType == CardImageType.none;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) {
							prefs.cardImageType = CardImageType.none;
							updateCardImageType();
						}
					}
				});
			}
			{
				JMenuItem menuItem = new JRadioButtonMenuItem(trans.get("menu.View.CardImageDisplay.Local", "Local"));
				group.add(menuItem);
				cardDisplayMenu.add(menuItem);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.cardImageType == CardImageType.local;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) {
							prefs.cardImageType = CardImageType.local;
							updateCardImageType();
						}
					}
				});
			}
			{
				JMenuItem menuItem = new JRadioButtonMenuItem(trans.get("menu.View.CardImageDisplay.Wizards", "Wizards"));
				group.add(menuItem);
				cardDisplayMenu.add(menuItem);
				menuItem.setModel(new JToggleButton.ToggleButtonModel() {
					public boolean isSelected () {
						return prefs.cardImageType == CardImageType.wizards;
					}

					public void setSelected (boolean b) {
						super.setSelected(b);
						if (b) {
							prefs.cardImageType = CardImageType.wizards;
							updateCardImageType();
						}
					}
				});
			}
		}
		{
			JMenuItem menuItem = new JCheckBoxMenuItem(trans.get("menu.View.Scale", "Scale larger than original size"));
			menuItem.setModel(new JToggleButton.ToggleButtonModel() {
				public boolean isSelected () {
					return prefs.scaleCardImageLarger;
				}

				public void setSelected (boolean b) {
					super.setSelected(b);
					prefs.scaleCardImageLarger = b;
				}
			});
			viewMenu.add(menuItem);
		}
	}

	protected void initializeSettingsMenu () {
		settingsMenu = new JMenu();
		menuBar.add(settingsMenu);
		settingsMenu.setText(trans.get("menu.Settings", "Settings"));
		settingsMenu.setMnemonic(KeyEvent.VK_E);
		{
			JMenuItem menuItem = new JMenuItem();
			settingsMenu.add(menuItem);
			menuItem.setText(trans.get("menu.Settings.CardImgPath", "Set card images path") + "...");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					JFileChooser fileChooser = new JFileChooser(prefs.imagesPath);
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fileChooser.setApproveButtonText(trans.get("dialog.OK", "OK"));
					fileChooser.setDialogTitle(trans.get("menu.Settings.CardImgPath", "Set card images path"));
					int result = fileChooser.showDialog(ArcaneFrame.this, trans.get("dialog.OK", "OK"));
					if (result != JFileChooser.APPROVE_OPTION) return;
					prefs.imagesPath = fileChooser.getSelectedFile().getAbsolutePath() + "/";
				}
			});
		}
		{
			JMenuItem menuItem = new JMenuItem();
			settingsMenu.add(menuItem);
			menuItem.setText(trans.get("menu.Settings.CardImgSuffix", "Set card images suffix") + "...");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					String suffix = (String)JOptionPane.showInputDialog(ArcaneFrame.this, trans.get("menu.Settings.CardImgSuffix.Prompt", "Enter the card image suffix")+":",
							trans.get("menu.Settings.CardImgSuffix.Title", "Set card images suffix"), JOptionPane.QUESTION_MESSAGE, null, null, prefs.imagesSuffix);
					if (suffix == null || suffix.trim().length() == 0) return;
					prefs.imagesSuffix = suffix;
				}
			});
		}
		{
			JMenuItem menuItem = new JCheckBoxMenuItem(trans.get("menu.Settings.LogFoundCardImg", "Log found card images"));
			menuItem.setSelected(false);
			settingsMenu.add(menuItem);
			menuItem.setModel(new JToggleButton.ToggleButtonModel() {
				public boolean isSelected () {
					return prefs.logFoundImages;
				}

				public void setSelected (boolean b) {
					super.setSelected(b);
					prefs.logFoundImages = b;
				}
			});
		}
		{
			JMenuItem menuItem = new JCheckBoxMenuItem(trans.get("menu.Settings.LogMissingCardImg", "Log missing card images"));
			menuItem.setSelected(false);
			settingsMenu.add(menuItem);
			menuItem.setModel(new JToggleButton.ToggleButtonModel() {
				public boolean isSelected () {
					return prefs.logMissingImages;
				}

				public void setSelected (boolean b) {
					super.setSelected(b);
					prefs.logMissingImages = b;
				}
			});
		}
		settingsMenu.addSeparator();
		{
			JMenuItem menuItem = new JMenuItem(trans.get("menu.Settings.SetCardLang", "Set card language") + "...");
			menuItem.setEnabled(arcane.getLanguages().size() > 1);
			settingsMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					final String newLanguage = (String)JOptionPane.showInputDialog(ArcaneFrame.this, trans.get("menu.Settings.SetCardLang.Prompt", "Choose a language")+":",
							trans.get("menu.Settings.SetCardLang.Title", "Card Language"), JOptionPane.QUESTION_MESSAGE, null, arcane.getLanguages().toArray(), prefs.language);
					if (newLanguage == null) return;
					prefs.language = newLanguage;
					JOptionPane.showMessageDialog(ArcaneFrame.this, trans.get("menu.Settings.SetCardLang.Warning", "Changes will take affect when the application is restarted."),
						"Card Language", JOptionPane.WARNING_MESSAGE);
				}
			});
		}
		{
			JMenuItem menuItem = new JMenuItem(trans.get("menu.Settings.SetUILang", "Set UI language") + "...");
			menuItem.setEnabled(arcane.getUILanguages().size() > 1);
			settingsMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					final String newLanguage = (String)JOptionPane.showInputDialog(ArcaneFrame.this, trans.get("menu.Settings.SetUILang.Prompt", "Choose a language")+":",
							trans.get("menu.Settings.SetUILang.Title", "UI Language"), JOptionPane.QUESTION_MESSAGE, null, arcane.getUILanguages().toArray(), prefs.uiLanguage);
					if (newLanguage == null) return;
					prefs.uiLanguage = newLanguage;
					JOptionPane.showMessageDialog(ArcaneFrame.this, trans.get("menu.Settings.SetUILang.Warning", "Changes will take affect when the application is restarted."),
							trans.get("menu.Settings.SetUILang.Title", "UI Language"), JOptionPane.WARNING_MESSAGE);
				}
			});
		}
		settingsMenu.addSeparator();
		{
			JMenuItem menuItem = new JMenuItem( trans.get("menu.Settings.Proxy.Title", "Network proxy settings") + "...");
			settingsMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					String proxyHost = (String)JOptionPane.showInputDialog(ArcaneFrame.this,
						 trans.get("menu.Settings.Proxy.Host.Prompt", "Enter the proxy server host name, or blank to not use a proxy server."), trans.get("menu.Settings.Proxy.Prompt.Title", "Network Proxy Server"),
						JOptionPane.QUESTION_MESSAGE, null, null, prefs.proxyHost);
					String proxyPort = "";
					if (proxyHost != null && proxyHost.length() > 0) {
						proxyPort = (String)JOptionPane.showInputDialog(ArcaneFrame.this, trans.get("menu.Settings.Proxy.Port.Prompt", "Enter the proxy server port, or blank to use port 80."),
							trans.get("menu.Settings.Proxy.Prompt.Title", "Network Proxy Server"), JOptionPane.QUESTION_MESSAGE, null, null, prefs.proxyPort);
					}
					prefs.proxyHost = proxyHost != null && proxyHost.length() > 0 ? proxyHost : null;
					prefs.proxyPort = proxyPort != null && proxyPort.length() > 0 ? proxyPort : null;
					if(prefs.proxyHost != null)
						System.setProperty("http.proxyHost", prefs.proxyHost);
					else
						System.getProperties().remove("http.proxyHost");
					if(prefs.proxyPort != null)
						System.setProperty("http.proxyPort", prefs.proxyPort);
					else
						System.getProperties().remove("http.proxyPort");
				}
			});
		}
		settingsMenu.addSeparator();
		{
			JMenuItem menuItem = new JCheckBoxMenuItem(trans.get("menu.Settings.ErrorsOpenLog", "Errors open log viewer"));
			menuItem.setSelected(true);
			settingsMenu.add(menuItem);
			menuItem.setModel(new JToggleButton.ToggleButtonModel() {
				public boolean isSelected () {
					return prefs.showLogOnError;
				}

				public void setSelected (boolean b) {
					super.setSelected(b);
					prefs.showLogOnError = b;
				}
			});
		}
	}

	protected void initializeToolsMenu () {
		toolsMenu = new JMenu();
		menuBar.add(toolsMenu);
		toolsMenu.setText(trans.get("menu.Tools", "Tools"));
		toolsMenu.setMnemonic(KeyEvent.VK_T);
		if (Util.classExists("arcane.client.ui.LobbyFrame")) {
			JMenuItem menuItem = new JMenuItem(trans.get("menu.Tools.Lobby", "Lobby") + "...", KeyEvent.VK_L);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK));
			toolsMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					try {
						((ArcaneFrame)Class.forName("arcane.client.ui.LobbyFrame").newInstance()).setVisible(true);
					} catch (Exception ex) {
						throw new ArcaneException(ex);
					}
				}
			});
		}
		if (Util.classExists("arcane.deckbuilder.ui.DeckBuilder")) {
			JMenuItem menuItem = new JMenuItem(trans.get("menu.Tools.DeckBuilder", "Deck Builder") + "...", KeyEvent.VK_D);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK));
			toolsMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					try {
						((ArcaneFrame)Class.forName("arcane.deckbuilder.ui.DeckBuilder").newInstance()).setVisible(true);
					} catch (Exception ex) {
						throw new ArcaneException(ex);
					}
				}
			});
		}
		if (Util.classExists("arcane.rulesviewer.ui.RulesViewer")) {
			JMenuItem menuItem = new JMenuItem(trans.get("menu.Tools.RulesViewer", "Rules Viewer") + "...", KeyEvent.VK_R);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK));
			toolsMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					try {
						((ArcaneFrame)Class.forName("arcane.rulesviewer.ui.RulesViewer").newInstance()).setVisible(true);
					} catch (Exception ex) {
						throw new ArcaneException(ex);
					}
				}
			});
		}
		{
			JMenuItem menuItem = new JMenuItem(trans.get("menu.Tools.LogViewer", "Log Viewer") + "...", KeyEvent.VK_E);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK));
			toolsMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					arcane.showLogFrame();
				}
			});
		}

	}

	protected void savePreferences () {
	}

	protected void updateCardImageType () {
	}

	public boolean exit () {
		prefs.save();

		try {
			arcane.saveUserData();
		} catch (IOException ex) {
			int result = JOptionPane.showConfirmDialog(ArcaneFrame.this,
				trans.get("ialog.Exit.Confirm.Text", "User data could not be saved. Continue to close without saving user data?"), trans.get("ialog.Exit.Confirm.Title", "Confirm Exit"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result != JOptionPane.OK_OPTION) return false;
		}

		setVisible(false);
		dispose();
		return true;
	}

	protected void processWindowEvent (WindowEvent event) {
		if (event.getID() == WindowEvent.WINDOW_CLOSING) {
			if (!exit()) return;
		}
		super.processWindowEvent(event);
	}
}
