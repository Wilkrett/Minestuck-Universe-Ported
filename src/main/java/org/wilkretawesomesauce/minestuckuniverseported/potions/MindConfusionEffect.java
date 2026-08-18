package org.wilkretawesomesauce.minestuckuniverseported.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.PotionConfusion} ("MIND_CONFUSION") - a pure
 * marker, carrying no attribute modifiers or tick behavior of its own, same shape as
 * {@code abilitech.heroAspect.breath.TechBreathWindVessel.WindFormedEffect}. Applied by
 * {@code mind.TechMindConfusion} ("Sensory Break"); whether a player has it is what that tech's own
 * {@code ClientEvents} checks to reverse their movement input, the same "marker effect +
 * MovementInputUpdateEvent" pattern already used by Wind Vessel and Hopeful Outburst.
 */
public class MindConfusionEffect extends MobEffect
{
	public MindConfusionEffect()
	{
		super(MobEffectCategory.HARMFUL, 0x7B2FBE);
	}
}
