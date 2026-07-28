package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A marker effect for {@code TechSoulStun} ("Soul Shock") - no attribute modifiers or tick behavior of
 * its own. It exists purely so "is this player currently soul-shocked" is network-synced to their own
 * client for free, which {@code client.gui.SoulShockScreen}/the tick handler that forces it open needs
 * to know without a bespoke synced flag.
 */
public class SoulShockedEffect extends MobEffect
{
	public SoulShockedEffect()
	{
		super(MobEffectCategory.HARMFUL, 0xFFB745);
	}
}
