
package arcane;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Decklist {
	public String getName ();

	public boolean exists ();

	public boolean isOpenable ();

	public void open () throws IOException;

	public List<DecklistCard> getDecklistCards ();

	public void save (List<Card> deckCards, Map<Card, Integer> deckCardToQty, List<Card> sideCards, Map<Card, Integer> sideCardToQty) throws IOException;

	public String getData ();
}
