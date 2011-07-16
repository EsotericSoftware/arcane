
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
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import arcane.Decklist;

public class SaveToVaultDialog extends JDialog {
	private List<MtgVaultDecklist> decklists;
	private MtgVaultPlugin plugin;

	public SaveToVaultDialog (MtgVaultPlugin plugin) throws HeadlessException {
		super(plugin.deckBuilder, "MTG Vault - Save", true);

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

		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				if (saveAsTextField.getText().trim().length() == 0) {
					JOptionPane.showMessageDialog(SaveToVaultDialog.this, "Please enter a deck name to save.", "MTG Vault - Save",
						JOptionPane.WARNING_MESSAGE);
					return;
				}
				saveDeck(saveAsTextField.getText());
			}
		});

		deckList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent evt) {
				if (deckList.getSelectedIndex() == -1) return;
				saveAsTextField.setText(((Decklist)deckList.getSelectedValue()).getName());
			}
		});
	}

	public void refresh () {
		deckList.setModel(new DefaultComboBoxModel(new String[0]));
		decklists = plugin.getVaultDecklists();
		deckList.setModel(new DefaultComboBoxModel(decklists.toArray()));
	}

	private void saveDeck (String deckName) {
		MtgVaultDecklist newDecklist = new MtgVaultDecklist(deckName, null);

		for (MtgVaultDecklist decklist : decklists) {
			if (decklist.getName().equals(deckName)) {
				int result = JOptionPane.showConfirmDialog(SaveToVaultDialog.this, "The existing deck will be overwritten.",
					"MTG Vault - Save", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.OK_OPTION) return;
				newDecklist.id = decklist.id;
			}
		}

		plugin.deckBuilder.saveDecklist(newDecklist, true);

		setVisible(false);
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
			saveAsLabel = new JLabel();
			getContentPane().add(
				saveAsLabel,
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
			saveAsLabel.setText("Save as:");
		}
		{
			saveAsTextField = new JTextField();
			getContentPane().add(
				saveAsTextField,
				new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,
					6, 0, 6), 0, 0));
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
				saveButton = new JButton();
				buttonSection.add(saveButton);
				saveButton.setText("Save");
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
				new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 6,
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
	private JLabel saveAsLabel;
	private JButton refreshButton;
	private JScrollPane decksScrollPane;
	private JButton cancelButton;
	private JButton saveButton;
	private JPanel buttonSection;
	private JTextField saveAsTextField;
}
