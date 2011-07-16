
package arcane.ui.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import arcane.Arcane;
import arcane.Card;
import arcane.CardProperty;

public class CardTableModel extends AbstractTableModel {
	public List<Card> viewCards = new ArrayList<Card>(512);
	public List<Card> unsortedCards = new ArrayList<Card>(512);
	public CardProperty[] properties = new CardProperty[0];
	public Map<Card, Integer> cardToQty;
	public boolean isUniqueOnly;

	private boolean editable;

	public CardTableModel (boolean editable) {
		this.editable = editable;
	}

	public boolean isCellEditable (int rowIndex, int columnIndex) {
		if (!isUniqueOnly && properties[columnIndex] == CardProperty.ownedQty) return true;
		if (!editable) return false;
		return properties[columnIndex] == CardProperty.set || properties[columnIndex] == CardProperty.setName
			|| properties[columnIndex] == CardProperty.version;
	}

	public void setProperties (CardProperty... properties) {
		this.properties = properties;
		fireTableStructureChanged();
	}

	public Object getValueAt (int rowIndex, int columnIndex) {
		if (columnIndex < 0 || columnIndex > properties.length)
			throw new IllegalArgumentException("Invalid column index: " + columnIndex);
		Card card = viewCards.get(rowIndex);
		if (properties[columnIndex] == CardProperty.qty) return cardToQty.get(card);
		if (properties[columnIndex] == CardProperty.ownedQty && isUniqueOnly)
			return Arcane.getInstance().getTotalOwnedQty(card.name);
		return card.getValue(properties[columnIndex]);
	}

	public int getRowCount () {
		return viewCards.size();
	}

	public int getColumnCount () {
		return properties.length;
	}

	public String getColumnName (int columnIndex) {
		if (columnIndex < 0 || columnIndex > properties.length)
			throw new IllegalArgumentException("Invalid column index: " + columnIndex);
		return " " + properties[columnIndex].text;
	}

	public int getColumnIndex (CardProperty property) {
		for (int i = 0; i < properties.length; i++)
			if (properties[i] == property) return i;
		return -1;
	}

	public String getValueAt (int rowIndex, CardProperty property) {
		return (String)getValueAt(rowIndex, getColumnIndex(property));
	}

	public int getRowIndex (Card card) {
		for (int i = 0, n = viewCards.size(); i < n; i++)
			if (viewCards.get(i).equals(card)) return i;
		return -1;
	}

	public List<Integer> getRowIndices (String cardName) {
		List<Integer> indices = new ArrayList<Integer>();
		for (int i = 0, n = viewCards.size(); i < n; i++)
			if (viewCards.get(i).name.equals(cardName)) indices.add(i);
		return indices;
	}

	public List<Card> getCards (String cardName) {
		List<Card> cards = new ArrayList<Card>();
		for (int index : getRowIndices(cardName))
			cards.add(viewCards.get(index));
		return cards;
	}
}
