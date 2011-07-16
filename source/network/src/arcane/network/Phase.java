
package arcane.network;

public enum Phase {
	untap, upkeep, draw, main1, beginCombat, declareAttackers, declareBlockers, combatDamage, endCombat, main2, endTurn, cleanup;

	static public Phase getNextPhase (Phase phase) {
		Phase[] values = Phase.values();
		int ordinal = phase.ordinal() + 1;
		if (ordinal >= values.length) return null;
		return values[ordinal];
	}
}
