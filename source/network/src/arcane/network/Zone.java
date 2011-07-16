
package arcane.network;

import com.captiveimagination.jgn.convert.type.FieldSerializable;

public class Zone implements FieldSerializable {
	private int id;
	private String name;
	private short playerID;
	private boolean isViewable;
	private boolean isPublic;

	public Zone () {
	}

	public Zone (int id, short playerID, String name, boolean isPublic, boolean isViewable) {
		this.id = id;
		this.playerID = playerID;
		this.name = name;
		this.isPublic = isPublic;
		this.isViewable = isViewable;
	}

	public int getId () {
		return id;
	}

	public short getPlayerID () {
		return playerID;
	}

	public String getName () {
		return name;
	}

	public boolean isPublic () {
		return isPublic;
	}

	public void setPublic (boolean isPublic) {
		this.isPublic = isPublic;
	}

	public boolean isViewable () {
		return isViewable;
	}

	public void setViewable (boolean isViewable) {
		this.isViewable = isViewable;
	}

	public boolean isViewableBy (short playerID) {
		return isViewable && (isPublic || playerID == this.playerID);
	}
}
