package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A marker effect for {@code TechTimeAccelerateSelf} ("Accelerate") - carries no attribute modifiers of
 * its own, it exists purely so the caster's own client can see how far into its charge-up the ability
 * currently is, the same "marker effect synced automatically like any potion" idiom
 * {@code breath.WindFormedEffect}/{@code hope.HopingEffect} already use for their own client-only hooks.
 * <p>
 * The amplifier is repurposed to carry the current charge percentage (0-100, refreshed every tick while
 * charging - see {@code TechTimeAccelerateSelf#onUseTick}), not a real effect strength - a variant of the
 * "duration as a free synced timer" idiom this project's other marker effects use ({@code TimeDilationEffect}/
 * {@code CalculatingEffect}), just using the amplifier slot instead of the duration one since this needs to
 * represent an accumulating value rather than a countdown. {@code client.AcceleratingVignette} is the sole
 * consumer.
 */
public class AcceleratingEffect extends MobEffect
{
	public AcceleratingEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0xFF4040);
	}
}
