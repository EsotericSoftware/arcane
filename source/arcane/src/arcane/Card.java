
package arcane;

import java.util.Arrays;
import java.util.List;

public class Card {
	static private int nextCardID = 1;

	static public Card back = new Card();

	public final int id = nextCardID++;
	public String name;
	public String englishName;
	public String set;
	public String color;
	public String type;
	public String englishType;
	public String typeSpecialCharacters;
	public String pt;
	public int power;
	public int toughness;
	public String rarity;
	public String castingCost;
	public String legal;
	public String legalSpecialCharacters;
	public int convertedCastingCost;
	public String creatureType = "";
	public String version;
	public String collectorNumber;
	public String manaProduced = "";
	public float price;
	public int rating;
	public String flags = "";
	public int ownedQty;
	public int qty = 1;
	public String shopUrl = "";

	private Card () {
		name = "";
		englishName = "";
		set = "";
		color = "";
		type = "";
		englishType = "";
		typeSpecialCharacters = "";
		pt = "";
		rarity = "";
		castingCost = "";
		legal = "";
		legalSpecialCharacters = "";
		manaProduced = "";
		creatureType = "";
		collectorNumber = "";
	}

	protected Card (Card card) {
		name = card.name;
		englishName = card.englishName;
		set = card.set;
		color = card.color;
		type = card.type;
		englishType = card.englishType;
		typeSpecialCharacters = card.typeSpecialCharacters;
		pt = card.pt;
		power = card.power;
		toughness = card.toughness;
		rarity = card.rarity;
		castingCost = card.castingCost;
		legal = card.legal;
		legalSpecialCharacters = card.legalSpecialCharacters;
		manaProduced = card.manaProduced;
		convertedCastingCost = card.convertedCastingCost;
		creatureType = card.creatureType;
		version = card.version;
		price = card.price;
		collectorNumber = card.collectorNumber;
	}

	protected Card (Card card, int setQty) {
		name = card.name;
		englishName = card.englishName;
		set = card.set;
		color = card.color;
		type = card.type;
		englishType = card.englishType;
		typeSpecialCharacters = card.typeSpecialCharacters;
		pt = card.pt;
		power = card.power;
		toughness = card.toughness;
		rarity = card.rarity;
		castingCost = card.castingCost;
		legal = card.legal;
		legalSpecialCharacters = card.legalSpecialCharacters;
		manaProduced = card.manaProduced;
		convertedCastingCost = card.convertedCastingCost;
		creatureType = card.creatureType;
		version = card.version;
		price = card.price;
		collectorNumber = card.collectorNumber;
		qty = setQty;
	}

	Card (List<String> fields) {
		name = fields.get(0);
		englishName = name.intern();

		set = Arcane.getInstance().getMainSet(fields.get(1));
		if (set == null) throw new ArcaneException("Unrecognized set \"" + fields.get(1) + "\" for card: " + name);

		color = getColor(fields.get(2));

		type = replaceDifficultCharacters(fields.get(3));
		englishType = type;
		typeSpecialCharacters = fields.get(3);

		pt = fields.get(4).replace('\\', '/');
		rarity = fields.get(6);
		if(rarity == null || "".equals(rarity))
			rarity = "R";

		if (name.equals("Little Girl"))
			castingCost = "{W}";
		else if (name.equals("Gleemax"))
			castingCost = "{1}{0}{0}{0}{0}{0}{0}";
		else if (name.equals("Transguild Courier"))
			castingCost = "{W}{U}{B}{R}{G}{|}" + fields.get(7);
		else
			castingCost = fields.get(7);

		legal = replaceDifficultCharacters(fields.get(8).trim());
		legalSpecialCharacters = fields.get(8).trim();

		if (fields.size() >= 10 && fields.get(9).length() > 0) {
			version = fields.get(9);
		} else
			version = "";

		if (fields.size() >= 12 && fields.get(11).length() > 0)
			collectorNumber = fields.get(11).replace('\\', '/');
		else
			collectorNumber = "";

		if (pt != null && pt.length() > 0) {
			// Break out power and toughness.
			String[] split = pt.split("\\/");
			if (split.length == 2) {
				try {
					power = Integer.parseInt(split[0]);
				} catch (NumberFormatException ignored) {
				}
				try {
					toughness = Integer.parseInt(split[1]);
				} catch (NumberFormatException ignored) {
				}
			}
			// Creature type.
			int dashIndex = type.indexOf(" - ");
			if (dashIndex != -1) creatureType = type.substring(dashIndex + 3);
		}

		int convertedCastingCost = 0;
		String cost = castingCost.split("\\/")[0];
		if (cost.indexOf("{|}") != -1) cost = cost.split("\\{\\|\\}")[1];
		int startingIndex = 0;
		while ((startingIndex = cost.indexOf("{", startingIndex)) != -1) {
			int closingIndex = cost.indexOf("}", startingIndex);
			String symbol = cost.substring(startingIndex + 1, closingIndex);
			startingIndex = closingIndex;
			char firstChar = symbol.charAt(0);
			char secondChar = 0;
			if(symbol.length() == 2)
				secondChar = symbol.charAt(1);
			if (firstChar == 'X' || firstChar == 'Y' || firstChar == 'Z') continue;
			if (firstChar >= '0' && firstChar <= '9' && secondChar <= '9' )
				convertedCastingCost += Integer.parseInt(symbol);
			else if (firstChar >= '0' && firstChar <= '9' && secondChar > '9')
				convertedCastingCost += firstChar - '0';
			else
				convertedCastingCost++;
		}
		this.convertedCastingCost = convertedCastingCost;
	}

	private String getColor (String colorAbbr) {
		if (colorAbbr.equals("W"))
			return "White";
		else if (colorAbbr.equals("U"))
			return "Blue";
		else if (colorAbbr.equals("B"))
			return "Black";
		else if (colorAbbr.equals("R"))
			return "Red";
		else if (colorAbbr.equals("G"))
			return "Green";
		else if (colorAbbr.equals("Gld"))
			return "Gold";
		else if (colorAbbr.equals("Lnd"))
			return "Land";
		else if (colorAbbr.equals("Art"))
			return "Artifact";
		else if (colorAbbr.indexOf("/") != -1) {
			String[] strings = colorAbbr.split("\\/");
			return getColor(strings[0]) + "/" + getColor(strings[1]);
		} else
			throw new IllegalArgumentException("Invalid color abbreviation \"" + colorAbbr + "\" for card: " + name);
	}

	public Object getValue (CardProperty property) {
		switch (property) {
		case name:
			return name;
		case set:
			return set.toUpperCase();
		case setName:
			return Arcane.getInstance().getSetName(set);
		case color:
			return color;
		case type:
			return type;
		case creatureType:
			return creatureType;
		case pt:
			return pt;
		case power:
			return power;
		case touhgness:
			return toughness;
		case rarity:
			return rarity;
		case castingCost:
			return castingCost;
		case legal:
			return legal;
		case convertedCost:
			return convertedCastingCost;
		case qty:
			return qty;
		case version:
			return version;
		case manaProduced:
			return manaProduced;
		case price:
			return price;
		case rating:
			return rating;
		case flags:
			return flags;
		case collectorNumber:
			return collectorNumber;
		case ownedQty:
			return ownedQty;
		default:
			throw new IllegalArgumentException("Invalid property: " + property);
		}
	}

	public void setFlag (String letter, boolean set) {
		if (set) {
			if (!flags.contains(letter)) {
				char[] newFlags = (flags + letter).toCharArray();
				Arrays.sort(newFlags);
				flags = new String(newFlags);
			}
		} else
			flags = flags.replace(letter, "");
	}

	public String toString () {
		return name;
	}

	public boolean isBasicLand () {
		return type.contains("Basic") && type.contains("Land");
	}

	public boolean isLand () {
		return type.contains("Land");
	}

	public boolean isCreature () {
		return pt != null && pt.length() > 0;
	}

	// Template getters:

	public boolean equals (Object obj) {
	    if(obj instanceof Card) {
		Card card = (Card)obj;
		if (!englishName.equals(card.englishName)) return false;
		if (!set.equals(card.set)) return false;
		if (!version.equals(card.version)) return false;
		return true;
	    } else
		return false;
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((englishName == null) ? 0 : englishName.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((set == null) ? 0 : set.hashCode());
		return result;
	}

	static private String replaceDifficultCharacters (String text) {
		// These are difficult to type when searching.
		text = text.replace("-", "-");
		text = text.replace("—", "-");
		text = text.replace("“", "\"");
		text = text.replace("”", "\"");
		text = text.replace("‘", "'");
		text = text.replace("’", "'");
		text = text.replace("…", "...");
		return text;
	}
}
