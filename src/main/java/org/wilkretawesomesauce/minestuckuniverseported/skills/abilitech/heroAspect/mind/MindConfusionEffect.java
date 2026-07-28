package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect for {@code TechMindConfusion} ("Sensory Break") - carries no attribute modifiers, real
 * modern equivalent of the original's custom {@code MSUPotions.MIND_CONFUSION}. Whether a player has it
 * is what {@code client.MindConfusionClientEvents} checks to reverse their movement input, the same
 * "marker effect + MovementInputUpdateEvent" pattern already used by Wind Vessel and Hopeful Outburst.
 */
public class MindConfusionEffect extends MobEffect
{
	public MindConfusionEffect()
	{
		super(MobEffectCategory.HARMFUL, 0x7B2FBE);
	}
}
