
package arcane;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import arcane.ui.ManaSymbols;
import arcane.ui.util.ProgressDialog;
import arcane.util.DataStore;
import arcane.util.InputStreamMonitor;
import arcane.util.DataStore.DataStoreConnection;

public class RulingsDataStore extends DataStore<RulingsDataStore.RulingsDataStoreConnection> {
	public RulingsDataStore () throws SQLException {
		super("data/rulesdb/rules", "rulings", false);
		addColumn("name VARCHAR (256)");
		addColumn("ruling VARCHAR (2048)");
		open();
		addIndex("name");
	}

	public RulingsDataStoreConnection newConnection () throws SQLException {
		return new RulingsDataStoreConnection();
	}

	static private String read (BufferedReader reader) throws IOException {
		String line = reader.readLine();
		if (line == null) throw new ArcaneException("Unexpected end of file while parsing: rule-cards.txt");
		return line;
	}

	public final class RulingsDataStoreConnection extends DataStore<DataStoreConnection>.DataStoreConnection {
		private final PreparedStatement addRuling;
		private final PreparedStatement getRuling;

		private RulingsDataStoreConnection () throws SQLException {
			addRuling = prepareStatement("INSERT INTO :table: (name, ruling) VALUES (?, ?)");
			getRuling = prepareStatement("SELECT ruling FROM :table: WHERE name=?");
		}

		public void addRuling (String name, String ruling) throws SQLException {
			addRuling.setString(1, name);
			addRuling.setString(2, ruling);
			addRuling.executeUpdate();
		}

		public List<String> getRulings (String name) throws SQLException {
			getRuling.setString(1, name);
			ResultSet set = getRuling.executeQuery();
			List<String> rulings = new ArrayList<String>();
			while (set.next()) {
				rulings.add(set.getString(1));
			}
			return rulings;
		}
	}

	void populate (final ProgressDialog dialog, String dataDir) {
		try {
			RulingsDataStoreConnection conn = newConnection();
			if (conn.getCount() > 0) {
				conn.close();
				return;
			}
			dialog.setValue(0);
			dialog.setMessage("Loading rulings data...");
			InputStreamMonitor stream = new InputStreamMonitor(new FileInputStream(dataDir + "rule-cards.txt")) {
				protected void updateProgress () {
					dialog.setValue(getPercentageComplete());
				}
			};
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "Cp1252"));
			String line;
			// Find initial section.
			while (true) {
				line = read(reader);
				if (line.startsWith("      -      -     *     -     *     - ")) break;
			}
			outerLoop: while (true) {
				// Search for card.
				while (true) {
					line = read(reader);
					if (line.equals("Acknowledgments and Disclaimers") || line.equals("Acknowledgements and Disclaimers"))
						break outerLoop;
					if (line.length() > 0 && line.charAt(0) != ' ' && line.endsWith(":")) break;
				}
				String name = line.substring(0, line.length() - 1);
				// Skip card info.
				while (true) {
					line = read(reader);
					if (line.startsWith("  Text")) break;
				}
				// Skip the rest of the text.
				while (true) {
					line = read(reader);
					if (!line.startsWith("    ")) break;
				}
				// For each ruling.
				while (line.length() > 0) {
					StringBuffer buffer = new StringBuffer(256);
					// Read to end of ruling.
					while (true) {
						if (buffer.length() > 0) buffer.append(' ');
						buffer.append(line.trim());
						line = read(reader);
						if (!line.startsWith("    ")) break;
					}
					String ruling = buffer.toString();
					ruling = ruling.replace("[", "<i>");
					ruling = ruling.replaceAll("(\\d)\\]", "$1</i><br>");
					ruling = ruling.replace("]", "</i>");
					ruling = ruling.replaceAll("\\b(G\\d\\d?\\.\\d\\d?[a-z]?)\\b", "<a href='$1'>$1</a>");
					ruling = ruling.replaceAll("\\b(\\d{3}(?>\\.\\d\\d?[a-z]?)?)\\b", "<a href='$1'>$1</a>");
					ruling = ruling.replace("Note - ", "<br>Note - ");
					ruling = ruling.replace("<br><br>", "<br>");
					if (ruling.startsWith("<br>")) ruling = ruling.substring(4);
					if (ruling.endsWith("<br>")) ruling = ruling.substring(0, ruling.length() - 4);
					ruling = ruling.replace("{Tap}", "{T}");
					ruling = ManaSymbols.replaceSymbolsWithHTML(ruling, true);
					conn.addRuling(name, ruling);
				}
			}
			conn.close();
			createIndexes();
		} catch (Exception ex) {
			throw new ArcaneException("Error populating rulings database:", ex);
		}
	}
}
