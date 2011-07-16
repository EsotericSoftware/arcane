
package arcane;

import java.util.ArrayList;
import java.util.List;

public final class DecklistCard {
	static private Arcane arcane = Arcane.getInstance();

	private final int id;
	private String name;
	private String set;
	private String version;
	private boolean isSideboard;

	public DecklistCard (Card card, boolean isSideboard) {
		id = card.id;
		setName(card.name);
		setSet(card.set);
		setVersion(card.version);
		setSideboard(isSideboard);
	}

	public int getId () {
		return id;
	}

	public String getName () {
		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

	public String getSet () {
		return set;
	}

	public void setSet (String set) {
		this.set = set;
	}

	public String getPictureNumber () {
		return version;
	}

	public void setVersion (String version2) {
		this.version = version2;
	}

	public boolean isSideboard () {
		return isSideboard;
	}

	public void setSideboard (boolean isSideboard) {
		this.isSideboard = isSideboard;
	}

	public Card getCard () {
		return arcane.getCard(name, set, version);
	}

	static public List<Card>[] getCards (List<DecklistCard> decklistCards) {
		List<Card> main = new ArrayList<Card>(70);
		List<Card> side = new ArrayList<Card>(15);

		for (DecklistCard decklistCard : decklistCards) {
			Card card = decklistCard.getCard();
			if (decklistCard.isSideboard())
				side.add(card);
			else
				main.add(card);
		}

		List<Card>[] cards = new List[2];
		cards[0] = main;
		cards[1]= side;
		return cards;
	}
}
