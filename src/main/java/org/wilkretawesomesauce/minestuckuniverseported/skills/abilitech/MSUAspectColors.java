package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import com.mraof.minestuck.player.EnumAspect;

import java.util.EnumMap;
import java.util.Map;

/**
 * Ported directly from MinestuckUniverse (1.12.2)'s
 * {@code capabilities.badgeEffects.BadgeEffects#particleColors} - each Aspect's 1-2 particle colors,
 * used by {@link MSUAbilitechParticles} the same way the original's {@code startPowerParticles}/
 * {@code oneshotPowerParticles} calls did. Keyed by {@link EnumAspect} directly (a real {@code EnumMap}
 * lookup) rather than trusting enum ordinal alignment between this project and the original's own
 * hand-indexed array, which also mixed in a second block of Class-based colors this project has no
 * equivalent enum for (heroClass was never ported).
 */
public final class MSUAspectColors
{
	private static final Map<EnumAspect, int[]> COLORS = new EnumMap<>(EnumAspect.class);

	static
	{
		COLORS.put(EnumAspect.BLOOD, new int[]{0xB71015, 0x3E1601});
		COLORS.put(EnumAspect.BREATH, new int[]{0x47E2FA, 0x4379E6});
		COLORS.put(EnumAspect.DOOM, new int[]{0x306800, 0x111111});
		COLORS.put(EnumAspect.HEART, new int[]{0xBD1864, 0x55142A});
		COLORS.put(EnumAspect.HOPE, new int[]{0xFFDE55, 0xFDFEFF});
		COLORS.put(EnumAspect.LIFE, new int[]{0x72EB34, 0xA49787});
		COLORS.put(EnumAspect.LIGHT, new int[]{0xF6FA4E, 0xF0840C});
		COLORS.put(EnumAspect.MIND, new int[]{0x06FFC9, 0x00923D});
		COLORS.put(EnumAspect.RAGE, new int[]{0x9C4DAC, 0x520C61});
		COLORS.put(EnumAspect.SPACE, new int[]{0x4BEC13});
		COLORS.put(EnumAspect.TIME, new int[]{0xFF2106, 0xB70D0E});
		COLORS.put(EnumAspect.VOID, new int[]{0x104EA2, 0x001856});
	}

	private MSUAspectColors()
	{
	}

	public static int[] get(EnumAspect aspect)
	{
		return COLORS.get(aspect);
	}
}
