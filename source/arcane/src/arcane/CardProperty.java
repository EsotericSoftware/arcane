
package arcane;

// Missing: artist
public enum CardProperty {
	qty("Qty"), //
	name("Name"), //
	castingCost("Cost"), //
	convertedCost("Converted Cost"), //
	type("Type"), //
	creatureType("Creature Type"), //
	color("Color"), //
	manaProduced("Mana Produced"), //
	pt("P/T"), //
	power("Power"), //
	touhgness("Toughness"), //
	rarity("Rarity"), //
	rating("Rating"), //
	flags("Flags"), //
	set("Set"), //
	setName("Set Name"), //
	version("Art #"), //
	collectorNumber("Collector #"), //
	legal("Legal Text"), //
	ownedQty("Owned Qty"), //
	price("Price");

	public final String text;

	CardProperty (String text) {
		this.text = text;
	}
}
