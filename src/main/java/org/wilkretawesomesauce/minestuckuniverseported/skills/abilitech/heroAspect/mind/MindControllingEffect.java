package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect applied to the <i>controller</i> (not the target) while {@code TechMindControl}
 * ("Mindflayer's Spell") is actively possessing a real player - carries no attribute modifiers, exists
 * purely so the controller's own client can tell (via the free network sync every potion effect
 * already gets) whether to start forwarding its own movement input to the server, the same
 * marker-effect idiom this project already uses for {@code WindFormedEffect}/{@code HopingEffect}.
 */
public class MindControllingEffect extends MobEffect
{
	public MindControllingEffect()
	{
		super(MobEffectCategory.NEUTRAL, 0x4B0082);
	}
}
