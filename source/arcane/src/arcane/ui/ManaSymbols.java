
package arcane.ui;

import java.awt.Graphics;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import arcane.Arcane;
import arcane.ui.util.UI;

public class ManaSymbols {
	static private final Map<String, Image> manaImages = new HashMap<String, Image>();
	static private Pattern replaceSymbolsPattern = Pattern.compile("\\{([^}/]*)/?([^}]*)\\}");

	static public void loadImages () {
		String[] symbols = new String[] {"0", "1", "10", "11", "12", "15", "16", "2", "2W", "2U", "2R", "2G", "2B", "3",
				"4", "5", "6", "7", "8", "9", "B", "BG", "BR", "G", "GU", "GW", "R", "RG", "RW", "S", "T", "U", "UB",
				"UR", "W", "WB", "WU", "PW", "PU", "PR", "PG", "PB", "PC", "X", "Y", "Z", "slash"};
		for (String symbol : symbols)
			manaImages.put(symbol, UI.getImageIcon("images/symbols-13/" + symbol + ".png").getImage());
	}

	static public void draw (Graphics g, String manaCost, int x, int y) {
		if (manaCost.length() == 0) return;
		manaCost = UI.getDisplayManaCost(manaCost);
		StringTokenizer tok = new StringTokenizer(manaCost, "}");
		while (tok.hasMoreTokens()) {
			String symbol = tok.nextToken().substring(1);
			Image image = manaImages.get(symbol);
			if (image == null) {
				Arcane.getInstance().log("Symbol not recognized \"" + symbol + "\" in mana cost: " + manaCost);
				continue;
			}
			g.drawImage(image, x, y, null);
			x += symbol.length() > 2 ? 10 : 14; // slash.png is only 10 pixels wide.
		}
	}

	static public int getWidth (String manaCost) {
		int width = 0;
		StringTokenizer tok = new StringTokenizer(manaCost, "}");
		while (tok.hasMoreTokens()) {
			String symbol = tok.nextToken().substring(1);
			width += symbol.length() > 2 ? 10 : 14; // slash.png is only 10 pixels wide.
		}
		return width;
	}

	static public synchronized String replaceSymbolsWithHTML (String value, boolean small) {
		if (small){
			value = value.replace("{C}", "<img src='file:images/symbols-11/C.png' width=13 height=11>");
			value = value.replace("{P}", "<img src='file:images/symbols-11/P.png' width=5 height=11>");
			value = value.replace("{Untap}", "{Q}");
			return replaceSymbolsPattern.matcher(value).replaceAll("<img src='file:images/symbols-11/$1$2.png' width=11 height=11>");
		}
		else {
			value = value.replace("{slash}", "<img src='file:images/symbols-13/slash.png' width=10 height=13>");
			value = value.replace("{C}", "<img src='file:images/symbols-13/C.png' width=16 height=13>");
			value = value.replace("{P}", "<img src='file:images/symbols-11/P.png' width=6 height=13>");
			value = value.replace("{Untap}", "{Q}");
			return replaceSymbolsPattern.matcher(value).replaceAll("<img src='file:images/symbols-13/$1$2.png' width=13 height=13>");
		}
	}
}
