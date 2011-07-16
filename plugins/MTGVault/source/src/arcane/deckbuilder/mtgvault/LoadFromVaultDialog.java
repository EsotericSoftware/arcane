
package arcane.deckbuilder.mtgvault;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import arcane.Decklist;

public class LoadFromVaultDialog extends JDialog {
	private final MtgVaultPlugin plugin;

	public LoadFromVaultDialog (final MtgVaultPlugin plugin) throws HeadlessException {
		super(plugin.deckBuilder, "MTG Vault - Load", true);

		this.plugin = plugin;

		initialize();

		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				refresh();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				setVisible(false);
			}
		});

		loadButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				if (deckList.getSelectedIndex() == -1) {
					JOptionPane.showMessageDialog(LoadFromVaultDialog.this, "Please select a deck to load.", "MTG Vault - Load",
						JOptionPane.WARNING_MESSAGE);
					return;
				}
				plugin.deckBuilder.loadDecklist((Decklist)deckList.getSelectedValue());
				setVisible(false);
			}
		});
	}

	public void refresh () {
		deckList.setModel(new DefaultComboBoxModel(new String[0]));
		deckList.setModel(new DefaultComboBoxModel(plugin.getVaultDecklists().toArray()));
	}

	private void initialize () {
		setSize(320, 240);
		GridBagLayout thisLayout = new GridBagLayout();
		getContentPane().setLayout(thisLayout);
		{
			decksLabel = new JLabel();
			getContentPane().add(
				decksLabel,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(6, 6,
					0, 0), 0, 0));
			decksLabel.setText("Decks:");
		}
		{
			buttonSection = new JPanel();
			FlowLayout buttonSectionLayout = new FlowLayout();
			buttonSectionLayout.setHgap(6);
			buttonSectionLayout.setVgap(6);
			buttonSection.setLayout(buttonSectionLayout);
			getContentPane().add(
				buttonSection,
				new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
			{
				refreshButton = new JButton();
				buttonSection.add(refreshButton);
				refreshButton.setText("Refresh");
			}
			{
				loadButton = new JButton();
				buttonSection.add(loadButton);
				loadButton.setText("Load");
			}
			{
				cancelButton = new JButton();
				buttonSection.add(cancelButton);
				cancelButton.setText("Cancel");
			}
		}
		{
			decksScrollPane = new JScrollPane();
			getContentPane().add(
				decksScrollPane,
				new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
					6), 0, 0));
			{
				deckList = new JList();
				decksScrollPane.setViewportView(deckList);
			}
		}
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width / 2 - getWidth() / 2, screenSize.height / 2 - getHeight() / 2);
	}

	private JList deckList;
	private JLabel decksLabel;
	private JScrollPane decksScrollPane;
	private JButton cancelButton;
	private JButton loadButton;
	private JPanel buttonSection;
	private JButton refreshButton;
}
