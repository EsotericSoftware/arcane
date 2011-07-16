
package arcane.network;

import java.nio.ByteBuffer;

import arcane.Arcane;
import arcane.Card;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.convert.BufferUtil;
import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.convert.type.FieldExternalizable;

public final class GameCard implements FieldExternalizable {
	static private int nextGameCardID = 1;
	static private Arcane arcane = Arcane.getInstance();

	private int id;
	public transient Card card;

	private Player controller;
	private Player owner;
	private boolean tapped;

	public GameCard () {
	}

	public GameCard (Card card) {
		this.card = card;
		id = nextGameCardID++;
		tapped = false;
	}

	public GameCard (Card card, Player owner) {
		this(card);
		this.owner = owner;
		this.controller = owner;
	}

	public int getId () {
		return id;
	}

	public void readObjectData (ByteBuffer buffer) throws ConversionException {
		card = arcane.getCard(BufferUtil.readInt(buffer));
	}

	public void writeObjectData (MessageClient client, ByteBuffer buffer) throws ConversionException {
		BufferUtil.writeInt(buffer, card.id);
	}

	public String toString () {
		return card.name + id;
	}

	public Player getController() {
		return controller;
	}

	public void setController(Player controller) {
		this.controller = controller;
	}

	public Player getOwner() {
		return owner;
	}

	public void setOwner(Player owner) {
		this.owner = owner;
	}

	public boolean isTapped() {
		return tapped;
	}

	public void setTapped(boolean tapped) {
		this.tapped = tapped;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GameCard){
			GameCard gc = (GameCard) obj;
			if (id == gc.id)
				return true;
		}
		return false;
	}
}
