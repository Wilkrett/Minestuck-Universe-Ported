package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A marker effect for {@code TechHopeyShit} ("Hopeful Outburst") - no attribute modifiers or tick
 * behavior of its own, just lets {@code client.HopefulOutburstClientEvents} know locally that the
 * caster's own client should be dampening/nudging their movement input right now, the same
 * synced-marker-effect pattern already used for Wind Vessel and Soul Shock.
 */
public class HopingEffect extends MobEffect
{
	public HopingEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0xF3296F);
	}
}
