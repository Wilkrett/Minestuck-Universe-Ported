package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect for {@code TechRageManagement} ("Anger Management") - same role as
 * {@link FrenziedEffect}, standing in for {@code IBadgeEffects#isRageShifted}/{@code setRageShifted}.
 */
public class RageShiftedEffect extends MobEffect
{
	public RageShiftedEffect()
	{
		super(MobEffectCategory.NEUTRAL, 0xB22222);
	}
}
