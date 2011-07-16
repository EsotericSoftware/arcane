
package arcane.ui;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.ArcanePreferences;
import arcane.ArcanePreferences.CardFontFamily;
import arcane.ArcanePreferences.CardFontSize;
import arcane.Card;
import arcane.ui.util.UI;
import arcane.util.Util;

public abstract class CardInfoPane extends JEditorPane {
	private Arcane arcane = Arcane.getInstance();
	private ArcanePreferences prefs = arcane.getPrefs();
	private Card currentCard;
	private boolean showSingleSet;

	public CardInfoPane () {
		UI.setHTMLEditorKit(this);
		setEditable(false);
		setBackground(UIManager.getColor("EditorPane.background"));

		addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate (HyperlinkEvent evt) {
				if (evt.getEventType() != EventType.ACTIVATED) return;
				showRule(evt.getDescription());
			}
		});
	}

	public boolean isCurrentCard (Card card) {
		return currentCard != null && card.equals(currentCard);
	}

	public void setCard (final Card card) {
		if (card == null) return;
		if (isCurrentCard(card)) return;
		currentCard = card;

		Util.threadPool.submit(new Runnable() {
			public void run () {
				if (!card.equals(currentCard)) return;

				String castingCost = UI.getDisplayManaCost(card.castingCost);
				castingCost = ManaSymbols.replaceSymbolsWithHTML(castingCost, false);

				int symbolCount = 0;
				int offset = 0;
				while ((offset = castingCost.indexOf("<img", offset) + 1) != 0)
					symbolCount++;

				String legal = card.legal;
				legal = legal.replaceAll("#([^#]+)#", "<i>$1</i>");
				legal = legal.replaceAll("\\s*//\\s*", "<hr width='50%'>");
				legal = legal.replace("\r\n", "<div style='font-size:5pt'></div>");

				List<String> rulings;
				try {
					rulings = arcane.getRulingsDataStoreConnection().getRulings(card.englishName);
				} catch (SQLException ex) {
					throw new ArcaneException("Error getting ruling.", ex);
				}

				boolean smallImages = true;
				int fontSize = 11;
				if (prefs.fontSize == CardFontSize.medium) {
					fontSize = 13;
					smallImages = false;
				} else if (prefs.fontSize == CardFontSize.large) {
					fontSize = 15;
					smallImages = false;
				}

				String fontFamily = "tahoma";
				if (prefs.fontFamily == CardFontFamily.arial)
					fontFamily = "arial";
				else if (prefs.fontFamily == CardFontFamily.verdana) {
					fontFamily = "verdana";
				}

				final StringBuffer buffer = new StringBuffer(512);
				buffer.append("<html><body style='font-family:");
				buffer.append(fontFamily);
				buffer.append(";font-size:");
				buffer.append(fontSize);
				buffer.append("pt;margin:0px 1px 0px 1px'>");
				buffer.append("<table cellspacing=0 cellpadding=0 border=0 width='100%'>");
				buffer.append("<tr><td valign='top'><b>");
				buffer.append(card.name);
				buffer.append("</b></td><td align='right' valign='top' style='width:");
				buffer.append(symbolCount * 11 + 1);
				buffer.append("px'>");
				buffer.append(castingCost);
				buffer.append("</td></tr></table>");
				buffer.append("<table cellspacing=0 cellpadding=0 border=0 width='100%'><tr><td>");
				buffer.append(card.type);
				buffer.append("</td><td align='right'>");
				if (!showSingleSet) {
					Set<String> sets = arcane.getSets(card.name);
					int i = 1, n = sets.size();
					for (String set : sets) {
						boolean currentSet = card.set.equals(set);
						if (currentSet) {
							switch (card.rarity.charAt(0)) {
							case 'M':
								buffer.append("<b color='#F9AD0A'>");
								break;
							case 'R':
								buffer.append("<b color='#E1D519'>");
								break;
							case 'U':
								buffer.append("<b color='silver'>");
								break;
							case 'C':
								buffer.append("<b color='black'>");
								break;
							}
						} else
							buffer.append("<span>");
						buffer.append(set.toUpperCase());
						if (currentSet) buffer.append("</b>");
						if (i < n) buffer.append(",");
						if (!currentSet) buffer.append("<span>");
						i++;
					}
				}
				buffer.append("</td></tr></table>");
				if (legal.length() > 0) {
					buffer.append("<br>");
					buffer.append(ManaSymbols.replaceSymbolsWithHTML(legal, smallImages));
				}
				if (!"".equals(card.version) || card.pt.length() > 0) {
					buffer.append("<table cellspacing=0 cellpadding=0 border=0 width='100%'><tr><td>");
					if (!"".equals(card.version)) {
						buffer.append(" (");
						buffer.append(card.version);
						buffer.append(')');
					}
					buffer.append("</td><td align='right'>");
					if (card.pt.length() > 0) {
						buffer.append("<b>");
						buffer.append(card.pt.replace('\\', '/'));
						buffer.append("</b>");
					}
					buffer.append("</td></tr></table>");
				}
				buffer.append("<hr>");
				if (card.rating > 0 || card.flags.length() > 0) {
					buffer.append("<table cellspacing=0 cellpadding=0 border=0 width='100%'><tr><td>");
					for (int ii = 0, nn = card.rating; ii < nn; ii++)
						buffer.append("<img src='file:images/star.png' width=14 height=14>");
					buffer.append("</td><td align='right'>");
					for (int ii = 0, nn = card.flags.length(); ii < nn; ii++) {
						buffer.append("<img src='file:images/flag_");
						buffer.append(card.flags.charAt(ii));
						buffer.append(".png' width=14 height=14>");
					}
					buffer.append("</td></tr></table>");
				}
				if (showSingleSet) {
					switch (card.rarity.charAt(0)) {
					case 'R':
						buffer.append("<b color='#E1D519'>");
						break;
					case 'U':
						buffer.append("<b color='silver'>");
						break;
					case 'C':
						buffer.append("<b color='black'>");
						break;
					}
					buffer.append(card.set.toUpperCase());
					buffer.append("</b><br>");
				}
				if (!card.name.equals(card.englishName)) {
					buffer.append(card.englishName);
					buffer.append("<br>");
				}
				if (card.price > 0) {
					buffer.append("$");
					buffer.append(card.price);
					String price = String.valueOf(card.price);
					if (price.length() > 2 && price.charAt(price.length() - 2) == '.') buffer.append("0");
					buffer.append("<br>");
				}
				if (rulings.size() > 0) {
					buffer.append("<br>");
					for (String ruling : rulings) {
						buffer.append(ruling);
						buffer.append("<br><br>");
					}
				}
				buffer.append("<br></body></html>");

				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						if (!card.equals(currentCard)) return;
						setText(buffer.toString());
						setCaretPosition(0);
					}
				});

				cardShown(card);
			}
		});
	}

	public void setShowSingleSet (boolean showSingleSet) {
		this.showSingleSet = showSingleSet;
	}

	protected void cardShown (Card card) {
	}

	protected abstract void showRule (String rule);
}
