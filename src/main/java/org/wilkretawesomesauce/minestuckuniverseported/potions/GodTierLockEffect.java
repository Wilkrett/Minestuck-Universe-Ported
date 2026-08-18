package org.wilkretawesomesauce.minestuckuniverseported.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.MSUPotionBase}-direct "GOD_TIER_LOCK" - a
 * pure marker, carrying no attribute modifiers or tick behavior of its own, same shape as
 * {@code abilitech.heroAspect.breath.TechBreathWindVessel.WindFormedEffect}. Applied by
 * {@code heart.TechHeartSoulSwitcher} ("Soul Switcher") to the swap target, matching the original exactly.
 * Consumed by {@code abilitech.heroAspect.TechHeroAspect}'s own {@code canUse} gate at amplifier &ge;1 -
 * real, but inert today since nothing in this project's port ever produces amplifier &ge;1 (that was
 * reserved for the original's God Tier ascension ritual itself, already a documented, deliberate
 * simplification elsewhere in this project - see {@code godtier.GodTierEvents}'s own doc comment).
 */
public class GodTierLockEffect extends MobEffect
{
	public GodTierLockEffect()
	{
		super(MobEffectCategory.HARMFUL, 0x808080);
	}
}
