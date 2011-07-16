
package arcane.client;

import java.util.ArrayList;
import java.util.List;

public class ManaPool {
	private int white;
	private int blue;
	private int black;
	private int red;
	private int green;
	private int colorless;

	private List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

	public int getWhite () {
		return white;
	}

	public void setWhite (int white) {
		this.white = white;
		changed();
	}

	public int getBlue () {
		return blue;
	}

	public void setBlue (int blue) {
		this.blue = blue;
		changed();
	}

	public int getBlack () {
		return black;
	}

	public void setBlack (int black) {
		this.black = black;
		changed();
	}

	public int getRed () {
		return red;
	}

	public void setRed (int red) {
		this.red = red;
		changed();
	}

	public int getGreen () {
		return green;
	}

	public void setGreen (int green) {
		this.green = green;
		changed();
	}

	public int getColorless () {
		return colorless;
	}

	public void setColorless (int colorless) {
		this.colorless = colorless;
		changed();
	}

	public void empty () {
		white = 0;
		blue = 0;
		black = 0;
		red = 0;
		green = 0;
		colorless = 0;
		changed();
	}

	public void add (String color, int qty) {
		if (color == null) throw new IllegalArgumentException("color cannot be null.");
		color = color.toUpperCase();
		if (color.equals("W")) {
			setWhite(getWhite() + qty);
		} else if (color.equals("U")) {
			setBlue(getBlue() + qty);
		} else if (color.equals("B")) {
			setBlack(getBlack() + qty);
		} else if (color.equals("R")) {
			setRed(getRed() + qty);
		} else if (color.equals("G")) {
			setGreen(getGreen() + qty);
		} else if (color.equals("C")) {
			setColorless(getColorless() + qty);
		} else
			throw new IllegalArgumentException("Invalid color: " + color);
	}

	private void changed () {
		for (ChangeListener changeListener : changeListeners)
			changeListener.changed();
	}

	public void addChangeListener (ChangeListener changeListener) {
		changeListeners.add(changeListener);
	}

	static public interface ChangeListener {
		public void changed ();
	}
}
