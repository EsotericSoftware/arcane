
package arcane.util;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads CSV data. Fields are character separated and enclosed in quotes if they contain the seperator character. Embedded quotes
 * are doubled. Empty fields are represented by two consecutive seperators.
 */
public final class CSVReader {
	static private String lineSeparator = System.getProperty("line.separator");
	static {
		if (lineSeparator == null) lineSeparator = "\015012"; // crlf in octal.
	}

	static private final int EOL = 0;
	static private final int ORDINARY = 1;
	static private final int QUOTE = 2;
	static private final int SEPARATOR = 3;
	static private final int WHITESPACE = 4;

	static private final int SEEKING_START = 0; // We are in blanks before the field.
	static private final int IN_PLAIN = 1; // We are in the middle of an ordinary field.
	static private final int IN_QUOTED = 2; // We are in middle of field surrounded in quotes.
	static private final int AFTER_END_QUOTE = 3;// We have just hit a quote, might be doubled or might be last one.
	static private final int SKIPPING_TAIL = 4; // We are in blanks after the field looking for the separator

	private BufferedReader reader;
	private final String separator;
	private final String quote;
	private final boolean allowMultiLineFields;
	private final boolean trim;
	private boolean strict;
	private int lineCount = 0;
	private boolean allFieldsDone = true; // False: next EOL marks an empty field. True: next EOL marks the end of all fields.

	// The line being parsed or null for none read yet. Unprocessed chars are removed as they are processed.
	private String line = null;

	/**
	 * @param fieldSeparator Field separator character. Usually ',' in North America, ';' and sometimes '\t' in Europe.
	 * @param textQualifier Character used to enclose fields containing a separator, usually '"'.
	 * @param trim True if reader should trim leading/trailing whitespace (eg, blanks, Cr, Lf, tab) from fields.
	 * @param allowMultiLineFields True if reader should allow quoted fields to span more than one line. Microsoft Excel can
	 *           generate files like this.
	 */
	public CSVReader (Reader reader, String fieldSeparator, String textQualifier, boolean trim, boolean allowMultiLineFields) {
		if (reader == null) throw new IllegalArgumentException("reader cannot be null.");

		// Convert Reader to BufferedReader if necessary.
		if (reader instanceof BufferedReader)
			this.reader = (BufferedReader)reader;
		else
			this.reader = new BufferedReader(reader);

		this.separator = fieldSeparator;
		this.quote = textQualifier;
		this.allowMultiLineFields = allowMultiLineFields;
		this.trim = trim;
	}

	/**
	 * Convenience constructor defaulting to: comma separator, '"' for quote, with trimming, no multiline fields.
	 */
	public CSVReader (Reader reader) {
		this(reader, ",", "\"", true, false);
	}

	/**
	 * Categorize a character for the finite state machine.
	 * @return int representing the character's category.
	 */
	private int categorize (String c) {
		if (c.equals(separator)) return SEPARATOR;
		if (c.equals(" ") || c.equals("\r")) return WHITESPACE;
		if (c.equals("\n")) return EOL; // Artificially applied to end of line.

		if (c.equals(quote)) {
			return QUOTE;
		}
		if (Character.isWhitespace(c.charAt(0))) {
			return WHITESPACE;
		} else {
			return ORDINARY;
		}
	}

	/**
	 * Get all fields in the row.
	 * @return List of Strings, one for each field. Returns null for EOF.
	 * @throws IOException Some problem reading the file, possibly malformed data.
	 */
	public List<String> getFields () throws IOException {
		List<String> fields = new ArrayList<String>(30);
		try {
			while (true) {
				String field = getField();
				if (field == null) break;
				fields.add(field);
			}
		} catch (EOFException ex) {
			return null;
		}
		return fields;
	}

	/**
	 * Reads a single field from the CSV file.
	 * @return String value, even if the field is numeric. Surrounded and embedded double quotes are stripped. Possibly an empty
	 *         String. Returns null for end of line.
	 * @throws EOFException At end of file after all fields have been read.
	 * @throws IOException Error reading the file, possibly malformed data.
	 */
	public String getField () throws EOFException, IOException {
		StringBuffer field = new StringBuffer(allowMultiLineFields ? 512 : 64);
		// Implement the parser as a finite state automation with five states.

		int state = SEEKING_START; // Start seeking, even if partway through a line.
		// No need to maintain state between fields.

		while (true) {
			getLineIfNeeded();

			charLoop:
			// Loop for each char in the line to find a field. Guaranteed to leave early by hitting EOL.
			for (int i = 0; i < line.length(); i++) {
				String c = line.substring(i, i + 1);
				int category = categorize(c);
				if (false) {
					// For debugging.
					System.out.println("char:" + c + ", state:" + state + ", field:" + field.length());
				}
				switch (state) {
				case SEEKING_START: { // In blanks before field.
					switch (category) {
					case WHITESPACE:
						// Ignore.
						break;

					case QUOTE:
						state = IN_QUOTED;
						break;

					case SEPARATOR:
						// End of empty field.
						line = line.substring(i + 1);
						return "";

					case EOL:
						// End of line.
						if (allFieldsDone) {
							// null to mark end of line.
							line = null;
							return null;
						}
						// Empty field, usually after a field seperator.
						allFieldsDone = true;
						line = line.substring(i);
						return "";

					case ORDINARY:
						field.append(c);
						state = IN_PLAIN;
						break;
					}
					break;
				}
				case IN_PLAIN: { // In middle of ordinary field.
					switch (category) {
					case QUOTE:
						if (strict) {
							throw new IOException("Malformed CSV stream. Missing quote at start of field on line: " + lineCount);
						}
						field.append(c);
						break;

					case SEPARATOR:
						// Done.
						line = line.substring(i + 1);
						return trim(field.toString());

					case EOL:
						line = line.substring(i); // Push EOL back.
						allFieldsDone = true;
						return trim(field.toString());

					case WHITESPACE:
						field.append(' ');
						break;

					case ORDINARY:
						field.append(c);
						break;
					}
					break;
				}

				case IN_QUOTED: { // In middle of field surrounded in quotes.
					switch (category) {
					case QUOTE:
						state = AFTER_END_QUOTE;
						break;

					case EOL:
						if (allowMultiLineFields) {
							field.append(lineSeparator);
							// Done with that line, but not with the field. Don't return a null to mark the end of the line.
							line = null;
							// Read next line and seek the end of the quoted field with the state still IN_QUOTED.
							break charLoop;
						}
						// No multiline fields allowed.
						allFieldsDone = true;
						if (strict) {
							throw new IOException("Malformed CSV stream. Missing quote after field on line: " + lineCount);
						}
						line = line.substring(i); // Push back EOL.
						allFieldsDone = true;
						return trim(field.toString());

					case WHITESPACE:
						field.append(' ');
						break;

					case SEPARATOR:
					case ORDINARY:
						field.append(c);
						break;
					}
					break;
				}

				case AFTER_END_QUOTE: { // A situation like "xxx" may turn out to be xxx""xxx" or "xxx". It is found out here.
					switch (category) {
					case QUOTE:
						// Was a double quote (a literal "). */
						field.append(c);
						state = IN_QUOTED;
						break;

					case SEPARATOR:
						// Done with field.
						line = line.substring(i + 1);
						return trim(field.toString());

					case EOL:
						line = line.substring(i); // Push back EOL.
						allFieldsDone = true;
						return trim(field.toString());

					case WHITESPACE:
						// Ignore trailing spaces up to separator.
						state = SKIPPING_TAIL;
						break;

					case ORDINARY:
						if (strict) {
							throw new IOException("Malformed CSV stream, missing separator after field on line: " + lineCount);
						}
						field.append(c);
						state = ORDINARY;
						break;

					}
					break;
				}

				case SKIPPING_TAIL: { // In spaces after a field, seeking separator.
					switch (category) {
					case SEPARATOR:
						// Done.
						line = line.substring(i + 1);
						return trim(field.toString());

					case EOL:
						line = line.substring(i); // Push back EOL.
						allFieldsDone = true;
						return trim(field.toString());

					case WHITESPACE:
						// Ignore trailing spaces up to separator.
						break;

					case QUOTE:
					case ORDINARY:
						throw new IOException("Malformed CSV stream, missing separator after field on line: " + lineCount);

					}
					break;
				}

				}
			}
		}
	}

	/**
	 * Trim the string, but only if trimming is on.
	 */
	private String trim (String s) {
		if (trim) return s.trim();
		return s;
	}

	/**
	 * Reads one integer field. An empty field returns 0, as does end of line.
	 * @throws EOFException At end of file after all fields have been read.
	 * @throws IOException Error reading the file, possibly malformed data.
	 * @throws NumberFormatException if field does not contain a well formed int.
	 */
	public int getInt () throws EOFException, IOException, NumberFormatException {
		String s = getField();
		if (s == null) return 0;
		if (!trim) s = s.trim();
		if (s.length() == 0) return 0;
		return Integer.parseInt(s);
	}

	/**
	 * Reads one long field. An empty field returns 0, as does end of line.
	 * @throws EOFException At end of file after all fields have been read.
	 * @throws IOException Error reading the file, possibly malformed data.
	 * @throws NumberFormatException if field does not contain a well formed long.
	 */
	public long getLong () throws EOFException, IOException, NumberFormatException {
		String s = getField();
		if (s == null) return 0;
		if (!trim) s = s.trim();
		if (s.length() == 0) return 0;
		return Long.parseLong(s);
	}

	/**
	 * Reads one float field. An empty field returns 0, as does end of line.
	 * @throws EOFException At end of file after all fields have been read.
	 * @throws IOException Error reading the file, possibly malformed data.
	 * @throws NumberFormatException if field does not contain a well formed float.
	 */
	public float getFloat () throws EOFException, IOException, NumberFormatException {
		String s = getField();
		if (s == null) return 0;
		if (!trim) s = s.trim();
		if (s.length() == 0) return 0;
		return Float.parseFloat(s);
	}

	/**
	 * Read one double field. An empty field returns 0, as does end of line.
	 * @throws EOFException At end of file after all fields have been read.
	 * @throws IOException Error reading the file, possibly malformed data.
	 * @throws NumberFormatException if field does not contain a well formed double.
	 */
	public double getDouble () throws EOFException, IOException, NumberFormatException {
		String s = getField();
		if (s == null) return 0;
		if (!trim) s = s.trim();
		if (s.length() == 0) return 0;
		return Double.parseDouble(s);
	}

	/**
	 * Makes sure a line is available for parsing. Does nothing if there already is one.
	 * @throws EOFException At end of file after all fields have been read.
	 * @throws IOException Error reading the file, possibly malformed data.
	 */
	private void getLineIfNeeded () throws EOFException, IOException {
		if (line == null) {
			if (reader == null) throw new IllegalStateException("This CSVReader has been closed.");
			allFieldsDone = false;
			line = reader.readLine(); // Strips platform specific line ending.
			if (line == null) throw new EOFException();
			line += '\n'; // Apply standard line end for parser to find.
			lineCount++;
		}
	}

	/**
	 * Skips over the specified number of fields.
	 * @throws EOFException At end of file after all fields have been read.
	 * @throws IOException Error reading the file, possibly malformed data.
	 */
	public void skip (int fields) throws EOFException, IOException {

		if (fields <= 0) {
			return;
		}
		for (int i = 0; i < fields; i++) {
			// throw results away
			getField();
		}
	}

	/**
	 * Advances the cursor by one line.
	 * @throws EOFException At end of file after all fields have been read.
	 * @throws IOException Error reading the file, possibly malformed data.
	 * @return false if end of file has been reached.
	 */
	public boolean skipToNextLine () throws IOException {
		if (line == null) {
			try {
				getLineIfNeeded();
			} catch (EOFException ex) {
				return false;
			}
		}
		line = null;
		return true;
	}

	/**
	 * Close the CSVReader.
	 */
	public void close () throws IOException {
		if (reader != null) reader.close();
		reader = null;
	}

	/**
	 * If true the CSVReader will fail when a text qualifier (eg, quote) is found inside a field. This failure means either the
	 * data is malformed or the text qualifier character is incorrect. If false, the text qualifier will be interpreted as part of
	 * the field. Defaults to false.
	 */
	public void setStrict (boolean strict) {
		this.strict = strict;
	}
}
