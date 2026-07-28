package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import com.mraof.minestuck.player.EnumClass;

import java.util.EnumMap;
import java.util.Map;

/**
 * Ported directly from MinestuckUniverse (1.12.2)'s {@code capabilities.badgeEffects.BadgeEffects}'s own
 * {@code particleColors} array's second (Class-keyed) half, exposed there as
 * {@code getClassParticleColors(EnumClass)} - the real per-{@code heroClass} particle color counterpart
 * to {@link MSUAspectColors}, which only ever covered the first (Aspect-keyed) half since the
 * {@code heroClass} package itself wasn't ported yet at the time. Every real class here gets exactly one
 * color (unlike some aspects' two), matching the original's own array shape.
 */
public final class MSUClassColors
{
	private static final Map<EnumClass, int[]> COLORS = new EnumMap<>(EnumClass.class);

	static
	{
		COLORS.put(EnumClass.BARD, new int[]{0xDB5397});
		COLORS.put(EnumClass.HEIR, new int[]{0x6D9EEB});
		COLORS.put(EnumClass.KNIGHT, new int[]{0xEF7F34});
		COLORS.put(EnumClass.MAGE, new int[]{0xB55BFF});
		COLORS.put(EnumClass.MAID, new int[]{0x31E0AB});
		COLORS.put(EnumClass.PAGE, new int[]{0xFFFF9B});
		COLORS.put(EnumClass.PRINCE, new int[]{0x7C1D1D});
		COLORS.put(EnumClass.ROGUE, new int[]{0x39C4C6});
		COLORS.put(EnumClass.SEER, new int[]{0xD670FF});
		COLORS.put(EnumClass.SYLPH, new int[]{0xFF8377});
		COLORS.put(EnumClass.THIEF, new int[]{0x996543});
		COLORS.put(EnumClass.WITCH, new int[]{0x7F7F7F});
		COLORS.put(EnumClass.LORD, new int[]{0xFF0000});
		COLORS.put(EnumClass.MUSE, new int[]{0x00FF00});
	}

	private MSUClassColors()
	{
	}

	public static int[] get(EnumClass heroClass)
	{
		return COLORS.get(heroClass);
	}
}
