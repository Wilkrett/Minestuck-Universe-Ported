package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A marker effect for {@code TechSling} ("Sylladex Sling") - carries no attribute modifiers of its own,
 * just lets the caster's own client know how far into its charge-up the current hold is, the same
 * amplifier-as-charge-percentage idiom {@code heroAspect.time.AcceleratingEffect} already established
 * (see that class's own doc comment). Amplifier is the current charge tick count, 0-20 (matching the
 * original's own {@code IBadgeEffects#getFOV()} nudge, capped the same way - see
 * {@code client.SlingZoomEvents}, the sole consumer, and {@link TechSling#onUseTick} for where it's
 * refreshed).
 */
public class SlingChargeEffect extends MobEffect
{
	public SlingChargeEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0x77FFEC);
	}
}
