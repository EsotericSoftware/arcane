
package arcane.ui.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.JLabel;

// This turned out nice, but is too slow.
public class GlowBlurText extends JLabel {
	private int glowSize;
	private float glowIntensity;
	private Color glowColor;
	private boolean wrap;

	public void setGlow (Color glowColor, int size, float intensity) {
		this.glowColor = glowColor;
		this.glowSize = size;
		this.glowIntensity = intensity;
	}

	public void setWrap (boolean wrap) {
		this.wrap = wrap;
	}

	public Dimension getPreferredSize () {
		Dimension size = super.getPreferredSize();
		size.width += glowSize;
		size.height += glowSize / 2;
		return size;
	}

	public void paint (Graphics g) {
		if (getText().length() == 0) return;

		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		Dimension size = getSize();
		int textX = 0;
		int textY = 0;

		int wrapWidth = wrap ? size.width - glowSize : Integer.MAX_VALUE;

		AttributedString attributedString = new AttributedString(getText());
		attributedString.addAttribute(TextAttribute.FONT, getFont());
		AttributedCharacterIterator characterIterator = attributedString.getIterator();
		FontRenderContext fontRenderContext = g2d.getFontRenderContext();
		LineBreakMeasurer measurer = new LineBreakMeasurer(characterIterator, fontRenderContext);
		while (measurer.getPosition() < characterIterator.getEndIndex()) {
			TextLayout textLayout = measurer.nextLayout(Math.max(0, wrapWidth));
			float ascent = textLayout.getAscent();
			textY += ascent; // Move down to baseline.

			// Write line in glow color, then blur it.
			BufferedImage image = new BufferedImage(size.width + glowSize * 2, size.height + glowSize * 2,
				BufferedImage.TYPE_INT_ARGB);
			Graphics2D imageGraphics = image.createGraphics();
			imageGraphics.setColor(glowColor);
			textLayout.draw(imageGraphics, glowSize, size.height + glowSize);
			imageGraphics.dispose();
			BufferedImage blurredImage = ImageUtil.getBlurredImage(image, glowSize, glowIntensity);
			g2d.drawImage(blurredImage, textX - glowSize / 2, textY - size.height - glowSize / 2, null);

			// Draw line.
			textLayout.draw(g2d, textX + glowSize / 2, textY + glowSize / 2);

			textY += textLayout.getDescent() + textLayout.getLeading(); // Move down to top of next line.
		}
	}
}
