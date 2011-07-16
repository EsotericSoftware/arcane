
package arcane;

public enum Format {
	all("All"), //
	vintage("Vintage (T1)"), //
	legacy("Legacy (T1.5)"), //
	extended("Extended (T1.x)"), //
	standard("Standard (T2)"), //
	custom("Custom");

	private final String text;

	private Format (String text) {
		this.text = text;
	}

	public String toString () {
		return text;
	}

	static Format getByText (String text) {
		if (text == null || text.length() == 0) return null;
		for (Format format : values())
			if (format.text.equals(text)) return format;
		throw new ArcaneException("Format not found: " + text);
	}
}
