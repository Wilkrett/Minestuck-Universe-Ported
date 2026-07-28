package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect for {@code TechMindStrike} ("Calculated Strike") - its remaining duration doubles as
 * the original's raw {@code IBadgeEffects#getCalculating()} tick counter (the same "potion duration as
 * a free, auto-decaying, auto-synced timer" idiom {@code TimeDilationEffect}/{@code BleedingEffect}
 * already use), consumed by {@code MindStrikeEvents} the instant this player's next attack lands.
 */
public class CalculatingEffect extends MobEffect
{
	public CalculatingEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0x4287F5);
	}
}
