
package arcane.network;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.convert.ArrayConverter;
import com.captiveimagination.jgn.convert.ConversionException;
import com.captiveimagination.jgn.convert.type.FieldExternalizable;

public final class Player implements FieldExternalizable {
	private short id;
	private String name;
	private transient Set<Phase> priorityStops = new HashSet<Phase>();

	public Player () {
		priorityStops.add(Phase.main1);
		priorityStops.add(Phase.combatDamage);
		priorityStops.add(Phase.main2);
	}

	public short getId () {
		return id;
	}

	public void setId (short id) {
		this.id = id;
	}

	public String getName () {
		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

	public Set<Phase> getPriorityStops () {
		return priorityStops;
	}

	public void setPriorityStops (Set<Phase> priorityStops) {
		if (priorityStops == null) throw new IllegalArgumentException("priorityStops cannot be null.");
		this.priorityStops = priorityStops;
	}

	public String toString () {
		return name + "-" + id;
	}

	public boolean equals (Object obj) {
		if (!(obj instanceof Player)) return false;
		return ((Player)obj).id == id;
	}

	public int hashCode () {
		return id;
	}

	public void readObjectData (ByteBuffer buffer) throws ConversionException {
		priorityStops = new HashSet<Phase>(Arrays.asList(new ArrayConverter().readObjectData(buffer, Phase[].class)));
	}

	public void writeObjectData (MessageClient client, ByteBuffer buffer) throws ConversionException {
		new ArrayConverter().writeObjectData(client, priorityStops.toArray(new Phase[priorityStops.size()]), buffer);
	}
}
