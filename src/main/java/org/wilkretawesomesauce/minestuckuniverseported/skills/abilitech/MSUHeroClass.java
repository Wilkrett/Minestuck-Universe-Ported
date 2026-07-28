package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import com.mraof.minestuck.player.EnumClass;

/**
 * Homestuck Title Classes, for tagging which "flavor" of Abilitech a tech belongs to - not gated on
 * (matching this whole framework's "no unlock gating" scope decision, see {@link Abilitech}), just tagged.
 * <p>
 * 14 of these 15 values are a direct 1:1 mirror, by name, of {@code com.mraof.minestuck.player.EnumClass}
 * (confirmed via the actual Minestuck 1.21.1 dependency jar - {@code BARD, HEIR, KNIGHT, MAGE, MAID, PAGE,
 * PRINCE, ROGUE, SEER, SYLPH, THIEF, WITCH, LORD, MUSE}). {@link #HERO} is this project's own addition and
 * has no Minestuck-side equivalent - Minestuck's real class list doesn't include a "Hero" class at all
 * (it's not one of the 12 canon Homestuck classes). It exists here because the original MinestuckUniverse
 * (1.12.2)'s package split was {@code skills.abilitech.heroAspect} (generic, per-Aspect techs usable by
 * any player regardless of their actual Title class - "hero" there just means "the player using it", the
 * story's protagonist, not a class) versus a separate {@code skills.abilitech.heroClass} package (actually
 * class-specific techs - Mage, Seer, Witch, etc.). {@link org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect}
 * (the base every currently-ported tech extends) always reports {@link #HERO} for exactly that reason.
 * <p>
 * The other 14 values are wired up now too - {@code abilitech.heroClass.TechHeroClass#getHeroClass()} maps
 * each real ported class tech onto its matching value via {@link #from(EnumClass)}.
 */
public enum MSUHeroClass
{
	HERO("heroClass.hero"),
	MAGE("heroClass.mage"),
	SEER("heroClass.seer"),
	WITCH("heroClass.witch"),
	ROGUE("heroClass.rogue"),
	PRINCE("heroClass.prince"),
	BARD("heroClass.bard"),
	SYLPH("heroClass.sylph"),
	MAID("heroClass.maid"),
	KNIGHT("heroClass.knight"),
	HEIR("heroClass.heir"),
	THIEF("heroClass.thief"),
	PAGE("heroClass.page"),
	LORD("heroClass.lord"),
	MUSE("heroClass.muse");

	public final String unloc;

	MSUHeroClass(String unloc)
	{
		this.unloc = unloc;
	}

	/** Maps Minestuck's real {@link EnumClass} onto this project's mirrored enum by name - safe since all
	 * 14 non-{@link #HERO} values are a confirmed 1:1 name match (see this enum's own doc comment). */
	public static MSUHeroClass from(EnumClass enumClass)
	{
		return valueOf(enumClass.name());
	}
}
