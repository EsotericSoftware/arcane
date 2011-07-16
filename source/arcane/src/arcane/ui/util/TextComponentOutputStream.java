
package arcane.ui.util;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class TextComponentOutputStream extends OutputStream {
	StringBuilder buffer = new StringBuilder(512);
	private JTextComponent textComponent;

	public TextComponentOutputStream (JTextComponent textComponent) {
		this.textComponent = textComponent;
	}

	public void write (int b) throws IOException {
		char c = (char)b;
		buffer.append(c);
		if (c != '\n') return;
		Document document = textComponent.getDocument();
		try {
			document.insertString(document.getEndPosition().getOffset(), buffer.toString(), null);
		} catch (BadLocationException ignored) {
		}
		textComponent.setCaretPosition(textComponent.getDocument().getLength());
		buffer.setLength(0);
	}
}
