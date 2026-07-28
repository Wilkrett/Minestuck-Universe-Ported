package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A marker effect for {@code TechBreathWindVessel} ("Vessel of the Wind") - carries no attribute
 * modifiers or tick behavior of its own, it exists purely so "is this player currently wind-formed" is
 * automatically network-synced to every observing client for free (the same way any potion effect
 * already is), which {@code client.WindVesselClientEvents} needs to decide whether to hide the
 * player's render and dampen their movement input.
 */
public class WindFormedEffect extends MobEffect
{
	public WindFormedEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0x47E2FA);
	}
}
