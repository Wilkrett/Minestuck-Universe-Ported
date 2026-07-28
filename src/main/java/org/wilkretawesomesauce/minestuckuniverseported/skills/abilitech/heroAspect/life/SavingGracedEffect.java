package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect for {@code TechLifeGrace} ("Saving Grace") - stands in for the original's
 * {@code IBadgeEffects#isSavingGraced}/{@code setSavingGraced}, consumed by {@link SavingGraceEvents}
 * the instant its wearer would otherwise die.
 */
public class SavingGracedEffect extends MobEffect
{
	public SavingGracedEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0xFFF8DC);
	}
}
