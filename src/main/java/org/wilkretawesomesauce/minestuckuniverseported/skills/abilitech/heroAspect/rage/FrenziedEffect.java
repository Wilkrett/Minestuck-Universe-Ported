package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect for {@code TechRageFrenzy} ("Frenzied Mayhem") - stands in for the original's
 * {@code IBadgeEffects#isFrenzied}/{@code setFrenzied} boolean capability field on any creature. A plain
 * potion effect already persists in NBT and re-syncs on chunk load for free, which is exactly what
 * {@code RageMobEvents}' {@code EntityJoinLevelEvent} handler needs to know whether to re-inject
 * {@link FrenzyTargetGoal} - the modern equivalent of the original's own {@code onJoinWorld} re-apply.
 */
public class FrenziedEffect extends MobEffect
{
	public FrenziedEffect()
	{
		super(MobEffectCategory.NEUTRAL, 0x8B0000);
	}
}
