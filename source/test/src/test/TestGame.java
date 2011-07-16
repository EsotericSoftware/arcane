
package test;

import arcane.Arcane;
import arcane.DecklistFile;
import arcane.client.ui.LobbyFrame;
import arcane.server.Server;

public class TestGame {
	public static void main (String[] args) throws Exception {
		Arcane.setup("data/test.properties", "test.log", true);
		Server.main(null);
		final DecklistFile decklistFile = new DecklistFile("decks/Fun With Fungus.csv", null);
		final LobbyFrame lobby1 = new LobbyFrame() {
			protected void joinedGame (short gameID) {
				startGame();
			}
		};
		final LobbyFrame lobby2 = new LobbyFrame() {
			protected void joinedGame (short gameID) {
				lobby1.joinGame(gameID, decklistFile);
			}
		};
		lobby1.connect("localhost", "Nate");
		lobby2.connect("localhost", "Dogg");
		lobby2.createGame(decklistFile);
	}
}
