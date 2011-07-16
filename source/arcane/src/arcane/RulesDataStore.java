
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

public class RulesDataStore extends DataStore<RulesDataStore.RulesDataStoreConnection> {
	public RulesDataStore () throws SQLException {
		super("data/rulesdb/rules", "rules", false);
		addColumn("rule VARCHAR (32)");
		addColumn("subrule VARCHAR (32)");
		addColumn("text VARCHAR (4096)");
		open();
		addIndex("rule");
		addIndex("subrule");
		addIndex("rule", "subrule");
		addIndex("text");
	}

	public RulesDataStoreConnection newConnection () throws SQLException {
		return new RulesDataStoreConnection();
	}

	static private String read (BufferedReader reader) throws IOException {
		String line = reader.readLine();
		if (line == null) throw new ArcaneException("Unexpected end of file while parsing: rule-general.txt");
		return line;
	}

	public final class RulesDataStoreConnection extends DataStore<arcane.util.DataStore.DataStoreConnection>.DataStoreConnection {
		private final PreparedStatement addRule;
		private final PreparedStatement getRuleText;
		private final PreparedStatement getSubrules;
		private final PreparedStatement getSubruleRule;
		private final PreparedStatement getRules;

		private RulesDataStoreConnection () throws SQLException {
			addRule = prepareStatement("INSERT INTO :table: (rule, subrule, text) VALUES (?, ?, ?)");
			getRuleText = prepareStatement("SELECT text FROM :table: WHERE rule=?");
			getSubruleRule = prepareStatement("SELECT rule FROM :table: WHERE subrule=?");
			getSubrules = prepareStatement("SELECT subrule, text FROM :table: WHERE rule=? AND subrule IS NOT NULL");
			getRules = prepareStatement("SELECT rule, text FROM :table: WHERE subrule IS NULL");
		}

		public void addRule (String rule, String subrule, String text) throws SQLException {
			addRule.setString(1, rule);
			addRule.setString(2, subrule);
			addRule.setString(3, text);
			addRule.executeUpdate();
		}

		public String getRuleText (String rule) throws SQLException {
			getRuleText.setString(1, rule);
			ResultSet set = getRuleText.executeQuery();
			if (!set.next()) return null;
			return set.getString(1);
		}

		public String getSubruleRule (String subrule) throws SQLException {
			getSubruleRule.setString(1, subrule);
			ResultSet set = getSubruleRule.executeQuery();
			if (!set.next()) return null;
			return set.getString(1);
		}

		public List<String[]> getRules () throws SQLException {
			ResultSet set = getRules.executeQuery();
			List<String[]> rules = new ArrayList<String[]>();
			while (set.next())
				rules.add(new String[] {set.getString(1), set.getString(2)});
			return rules;
		}

		public List<String[]> getSubrules (String rule) throws SQLException {
			getSubrules.setString(1, rule);
			ResultSet set = getSubrules.executeQuery();
			List<String[]> subrules = new ArrayList<String[]>();
			while (set.next())
				subrules.add(new String[] {set.getString(1), set.getString(2)});
			return subrules;
		}
	}

	void populate (final ProgressDialog dialog, String dataDir) {
		try {
			RulesDataStoreConnection conn = newConnection();
			if (conn.getCount() > 0) {
				conn.close();
				return;
			}
			dialog.setValue(0);
			dialog.setMessage("Loading rules data...");
			InputStreamMonitor stream = new InputStreamMonitor(new FileInputStream(dataDir + "rule-general.txt")) {
				protected void updateProgress () {
					dialog.setValue(getPercentageComplete());
				}
			};
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "Cp1252"));
			String line;
			// Find table of contents section.
			while (true) {
				line = read(reader);
				if (line.startsWith("------------------------------------------------------------------------------")) break;
			}
			// Find first section.
			while (true) {
				line = read(reader);
				if (line.startsWith("------------------------------------------------------------------------------")) break;
			}
			outerLoop: while (true) {
				// Search for rule.
				while (true) {
					line = read(reader);
					if (line.equals("Acknowledgments and Disclaimers") || line.equals("Acknowledgements and Disclaimers"))
						break outerLoop;
					if (line.length() > 0) break;
				}
				int dashIndex = line.indexOf("- ");
				if (dashIndex == -1) {
					dashIndex = line.indexOf(". ");
					if (dashIndex == -1) throw new ArcaneException("Expected \"[rule] - [text]\" or \"[rule]. [text]\": " + line);
				}
				// Found rule.
				String rule = line.substring(0, dashIndex).trim();
				int plusIndex = rule.indexOf("+");
				if (plusIndex != -1) {
					rule = rule.substring(plusIndex+1).trim();
				}
				String ruleText = line.substring(dashIndex + 2).trim();
				line = read(reader);
				// Rule was actually a section heading.
				if (line.equals("------------------------------------------------------------------------------")) continue;
				// Store rule.
				conn.addRule(rule, null, ruleText);
				// Store sub rules.
				while (true) {
					if (!line.startsWith("  " + rule) && !line.startsWith("+ " + rule)) {
						// If the first line is not a subrule, skip the whole rule.
						while (true) {
							if (line.length() == 0) continue outerLoop;
							line = read(reader);
						}
					}
					// Found subrule.
					dashIndex = line.indexOf(" - ");
					if (dashIndex == -1) throw new ArcaneException("Expected \"[subrule] - [text]\": " + line);
					String subrule = line.substring(2, dashIndex);
					// Read to next subrule or end of rule.
					StringBuffer buffer = new StringBuffer(256);
					buffer.append(line.substring(dashIndex + 3));
					while (true) {
						line = read(reader);
						if (line.length() == 0) break;
						if (line.startsWith("  " + rule) || line.startsWith("+ " + rule)) break;
						if (buffer.length() > 0) buffer.append(' ');
						buffer.append(line.trim());
					}
					// Store subrule.
					String text = buffer.toString();
					text = text.replace("  ", " ");
					text = text.replace("[", "<i>");
					text = text.replaceAll("(\\d)\\]", "$1</i><br>");
					text = text.replace("]", "</i>");
					text = text.replaceAll("\\b(G\\d\\d?\\.\\d\\d?[a-z]?)\\b", "<a href='$1'>$1</a>");
					text = text.replaceAll("\\b(\\d{3}(?>\\.\\d\\d?[a-z]?)?)\\b", "<a href='$1'>$1</a>");
					text = text.replaceAll("\\s*Example:", "<br><b>Example:</b> ");
					text = text.replaceAll("\\s*Note - ", "<br>Note - ");
					text = text.replaceAll("<br>\\s*<br>", "<br>");
					if (text.startsWith("<br>")) text = text.substring(4);
					if (text.endsWith("<br>")) text = text.substring(0, text.length() - 4);
					text = text.replace("{Tap}", "{T}");
					text = ManaSymbols.replaceSymbolsWithHTML(text, true);
					conn.addRule(rule, subrule, text);
					if (line.length() == 0) break;
				}
			}
			conn.close();
			createIndexes();
		} catch (Exception ex) {
			throw new ArcaneException("Error populating rules database.", ex);
		}
	}
}
