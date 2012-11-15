
package arcane;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import arcane.ui.util.ProgressDialog;
import arcane.util.CSVReader;
import arcane.util.DataStore;
import arcane.util.UnicodeReader;

public class CardDataStore extends DataStore<CardDataStore.CardDataStoreConnection> {
	public CardDataStore () throws SQLException {
		super("cardsdb", "cards", true);
		addColumn("name VARCHAR (256)");
		addColumn("englishName VARCHAR (256)");
		addColumn("set VARCHAR (6)");
		addColumn("setOrdinal INTEGER");
		addColumn("type VARCHAR (64)");
		addColumn("englishType VARCHAR (64)");
		addColumn("rarity VARCHAR (3)");
		addColumn("castingCost VARCHAR (50)");
		addColumn("convertedCastingCost INTEGER");
		addColumn("legal VARCHAR (2500)");
		addColumn("englishLegal VARCHAR (2500)");
		addColumn("color VARCHAR (24)");
		addColumn("manaProduced VARCHAR (5)");
		addColumn("power INTEGER");
		addColumn("toughness INTEGER");
		addColumn("version VARCHAR (64)");
		addColumn("collectorNumber VARCHAR (12)");
		addColumn("price DECIMAL (7, 2)");
		open();
		addIndex("name");
		addIndex("set");
		addIndex("name", "set");
		addIndex("set", "castingCost", "color", "type", "name", "legal");
	}

	public CardDataStoreConnection newConnection () throws SQLException {
		return new CardDataStoreConnection();
	}

	public final class CardDataStoreConnection extends DataStore<arcane.util.DataStore.DataStoreConnection>.DataStoreConnection {
		private final PreparedStatement addCard;
		private final PreparedStatement updateCardLanguage;
		private final PreparedStatement updateManaProduced;
		private final PreparedStatement getPictureToCollectorNumbers;
		private final PreparedStatement setPriceSet;
		private final PreparedStatement setPrice;
		private final PreparedStatement getPrice;

		private CardDataStoreConnection () throws SQLException {
			addCard = prepareStatement("INSERT INTO :table: "
				+ "(name, englishName, set, type, englishType, rarity, castingcost, convertedcastingcost, "
				+ "legal, englishLegal, color,  power, toughness, manaProduced, version, collectorNumber, price) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			updateCardLanguage = prepareStatement("UPDATE :table: SET name=?, legal=?, type=? WHERE name=?");
			updateManaProduced = prepareStatement("UPDATE :table: SET manaProduced=? WHERE name=?");
			getPictureToCollectorNumbers = prepareStatement("SELECT version, collectorNumber FROM :table: "
				+ "WHERE englishName=? AND set=?");
			setPriceSet = prepareStatement("UPDATE :table: SET price=? WHERE englishName=? AND set=?");
			setPrice = prepareStatement("UPDATE :table: SET price=? WHERE englishName=?");
			getPrice = prepareStatement("SELECT price FROM :table: " + "WHERE englishName=? AND set=?");
		}

		public void addCard (Card card) throws SQLException {
			addCard.setString(1, card.name.toLowerCase());
			addCard.setString(2, card.name.toLowerCase());
			addCard.setString(3, card.set.toLowerCase());
			addCard.setString(4, card.type.toLowerCase());
			addCard.setString(5, card.type.toLowerCase());
			addCard.setString(6, card.rarity.toLowerCase());
			addCard.setString(7, card.castingCost.toLowerCase());
			addCard.setInt(8, card.convertedCastingCost);
			addCard.setString(9, card.legal.toLowerCase());
			addCard.setString(10, card.legal.toLowerCase());
			addCard.setString(11, card.color.toLowerCase());
			addCard.setInt(12, card.power);
			addCard.setInt(13, card.toughness);
			addCard.setString(14, card.manaProduced.toLowerCase().replace("{", "").replace("}", ""));
			addCard.setString(15, card.version);
			addCard.setString(16, card.collectorNumber);
			addCard.setFloat(17, card.price);
			try {
				addCard.executeUpdate();
			} catch (SQLException ex) {
				SQLException newEx = new SQLException("Unable to add card: " + card.name);
				newEx.initCause(ex);
				throw newEx;
			}
		}

		public void updateCardLanguage (String name, String newName, String newType, String newLegal) throws SQLException {
			updateCardLanguage.setString(1, newName.toLowerCase());
			updateCardLanguage.setString(2, newLegal.toLowerCase());
			updateCardLanguage.setString(3, newType.toLowerCase());
			updateCardLanguage.setString(4, name.toLowerCase());
			try {
				updateCardLanguage.executeUpdate();
			} catch (SQLException ex) {
				SQLException newEx = new SQLException("Unable to update card: " + name);
				newEx.initCause(ex);
				throw newEx;
			}
		}

		public void updateManaProduced (String name, String manaProduced) throws SQLException {
			updateManaProduced.setString(1, manaProduced.toLowerCase().replace("{", "").replace("}", ""));
			updateManaProduced.setString(2, name.toLowerCase());
			try {
				updateManaProduced.executeUpdate();
			} catch (SQLException ex) {
				SQLException newEx = new SQLException("Unable to update card: " + name);
				newEx.initCause(ex);
				throw newEx;
			}
		}

		public void setPrice (String name, String set, float price) throws SQLException {
			setPriceSet.setFloat(1, price);
			setPriceSet.setString(2, name.toLowerCase());
			setPriceSet.setString(3, set.toLowerCase());
			setPriceSet.executeUpdate();
		}

		public void setPrice (String name, float price) throws SQLException {
			setPrice.setFloat(1, price);
			setPrice.setString(2, name.toLowerCase());
			setPrice.executeUpdate();
		}
	}

	List<Card> populate (ProgressDialog dialog, String dataDir) throws IOException, SQLException {
		dialog.setValue(0);

		CardDataStoreConnection conn = newConnection();

		List<Card> allCards = new ArrayList<Card>(20000);

		CSVReader reader = new CSVReader(new UnicodeReader(new FileInputStream(dataDir + "cards.csv"), "UTF-8"), ",", "\"", true,
			true);
		while (true) {
			List<String> fields = reader.getFields();
			if (fields == null) break;
			if (fields.size() < 2) continue;

			Card card = new Card(fields);

			Integer newOrdinal = Arcane.getInstance().getSetOrdinal(card.set);
			if (newOrdinal == null || card.set == null) {
				Arcane.getInstance().logError("Unrecognized set for card: " + fields);
				continue;
			}

			conn.addCard(card);
			allCards.add(card);

			if (allCards.size() % 250 == 0) dialog.setValue(allCards.size() / 16000F);
		}
		reader.close();

		conn.close();

		createIndexes();

		return allCards;
	}
}
