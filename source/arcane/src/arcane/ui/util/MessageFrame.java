
package arcane.ui.util;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledEditorKit;

import arcane.ArcaneException;

public class MessageFrame extends JFrame {
	public JEditorPane editorPane;
	private JPanel buttonSection;

	private JScrollPane scrollPane;

	public MessageFrame (String title) {
		super(title);
		initializeComponents();
	}

	public void setHTML (boolean html) {
		if (html)
			UI.setHTMLEditorKit(editorPane);
		else
			editorPane.setEditorKit(new StyledEditorKit());
	}

	public void setText (String text) {
		editorPane.setText(text);
		editorPane.setCaretPosition(0);
	}

	public void appendText (String text) {
		Document document = editorPane.getDocument();
		try {
			document.insertString(document.getEndPosition().getOffset(), text, null);
		} catch (BadLocationException ignored) {
		}
	}

	public void setText (URL url) {
		if (url == null) return;
		BufferedReader reader = null;
		try {
		    try{
		    	reader = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuffer buffer = new StringBuffer(1024);
			while (true) {
				String line = reader.readLine();
				if (line == null) break;
				buffer.append(line);
			}
			setText(buffer.toString());
		    } finally {
			if(reader != null)
			    	reader.close();
		    }
		} catch (IOException ex) {
			throw new ArcaneException("Error setting message frame text: " + url, ex);
		}

	}

	public void scrollToEnd () {
		try {
			editorPane.scrollRectToVisible(editorPane.modelToView(editorPane.getDocument().getLength()));
		} catch (BadLocationException ignored) {
		}
	}

	public JButton addButton (String text) {
		JButton button = UI.getButton();
		buttonSection.add(button);
		button.setText(text);
		if (text.equals("Close")) {
			button.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent evt) {
					setVisible(false);
				}
			});
		}
		return button;
	}

	public void setVisible (boolean b) {
		super.setVisible(b);
		if (!b) dispose();
	}

	private void initializeComponents () {
		int width = 640, height = 480;
		setSize(width, height);
		setResizable(true);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width / 2 - width / 2, screenSize.height / 2 - height / 2);

		getContentPane().setLayout(new GridBagLayout());
		setVisible(false);
		{
			scrollPane = new JScrollPane();
			getContentPane().add(
				scrollPane,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 4,
					6), 0, 0));
			{
				editorPane = new JEditorPane();
				editorPane.setEditorKit(new StyledEditorKit());
				editorPane.setEditable(false);
				editorPane.setBackground(UIManager.getColor("EditorPane.background"));
				scrollPane.setViewportView(editorPane);
			}
		}
		{
			buttonSection = new JPanel();
			FlowLayout buttonSectionLayout = new FlowLayout();
			buttonSectionLayout.setVgap(0);
			buttonSectionLayout.setHgap(4);
			buttonSection.setLayout(buttonSectionLayout);
			getContentPane().add(
				buttonSection,
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 6, 2), 0, 0));
		}
	}
}
